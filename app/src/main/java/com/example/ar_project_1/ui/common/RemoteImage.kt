package com.example.ar_project_1.ui.common

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.ar_project_1.data.repository.PatientCardRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Downloads images from the authenticated API, decodes them downsampled
 * (ImageDecoder also applies EXIF rotation) and keeps them in a memory cache,
 * so the gallery, the detail view and the chat share the same bitmaps.
 */
class RemoteImageLoader(private val repository: PatientCardRepository) {

    private val cache = object : LruCache<String, ImageBitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<ImageBitmap>>()

    fun cached(id: String, maxSide: Int): ImageBitmap? = cache.get(key(id, maxSide))

    suspend fun load(id: String, maxSide: Int): ImageBitmap {
        val key = key(id, maxSide)
        cache.get(key)?.let { return it }
        val fresh = CompletableDeferred<ImageBitmap>()
        val existing = inFlight.putIfAbsent(key, fresh)
        if (existing != null) return existing.await()
        try {
            val bytes = repository.downloadImage(id)
            val bitmap = withContext(Dispatchers.Default) { decode(bytes, maxSide) }
            cache.put(key, bitmap)
            fresh.complete(bitmap)
            return bitmap
        } catch (e: Exception) {
            fresh.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key)
        }
    }

    fun evict(id: String) {
        cache.snapshot().keys.filter { it.startsWith("$id@") }.forEach(cache::remove)
    }

    private fun decode(bytes: ByteArray, maxSide: Int): ImageBitmap =
        try {
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > maxSide) decoder.setTargetSampleSize(longest / maxSide)
            }.asImageBitmap()
        } catch (e: Exception) {
            // ImageDecoder odrzuca część plików (np. JPEG z dużymi segmentami metadanych
            // C2PA z generatorów AI). BitmapFactory jest bardziej tolerancyjny.
            Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory", e)
            decodeWithBitmapFactory(bytes, maxSide)
        }

    private fun decodeWithBitmapFactory(bytes: ByteArray, maxSide: Int): ImageBitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IllegalStateException("Nie można zdekodować zdjęcia (${bytes.size} B).")
        return bitmap.asImageBitmap()
    }

    private fun key(id: String, maxSide: Int) = "$id@$maxSide"

    companion object {
        private const val TAG = "RemoteImage"
        const val THUMBNAIL = 320
        const val FULL = 1600
        private const val CACHE_BYTES = 48 * 1024 * 1024
    }
}

@Composable
fun RemoteImage(
    imageId: String,
    loader: RemoteImageLoader,
    modifier: Modifier = Modifier,
    maxSide: Int = RemoteImageLoader.THUMBNAIL,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    var bitmap by remember(imageId, maxSide) { mutableStateOf(loader.cached(imageId, maxSide)) }
    var failed by remember(imageId, maxSide) { mutableStateOf(false) }

    LaunchedEffect(imageId, maxSide) {
        if (bitmap == null) {
            try {
                bitmap = loader.load(imageId, maxSide)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("RemoteImage", "Loading image $imageId failed", e)
                failed = true
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        val image = bitmap
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
            failed -> Icon(
                Icons.Default.BrokenImage,
                contentDescription = "Nie udało się wczytać zdjęcia",
                tint = MaterialTheme.colorScheme.outline,
            )
            else -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
