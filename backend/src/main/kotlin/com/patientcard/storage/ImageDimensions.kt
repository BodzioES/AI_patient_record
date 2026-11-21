package com.patientcard.storage

import java.nio.file.Path
import javax.imageio.ImageIO

/** Reads width/height from the image header without decoding the pixels. */
object ImageDimensions {

    fun read(path: Path): Pair<Int, Int>? = runCatching {
        ImageIO.createImageInputStream(path.toFile())?.use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (!readers.hasNext()) return@use null
            val reader = readers.next()
            try {
                reader.input = input
                reader.getWidth(0) to reader.getHeight(0)
            } finally {
                reader.dispose()
            }
        }
    }.getOrNull()
}
