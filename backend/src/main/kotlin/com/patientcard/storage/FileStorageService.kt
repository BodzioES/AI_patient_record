package com.patientcard.storage

import com.patientcard.common.NotFoundException
import com.patientcard.common.StorageException
import com.patientcard.config.AppProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

data class StoredFile(
    val storageKey: String,
    val sha256: String,
    val sizeBytes: Long,
)

@Service
class FileStorageService(appProperties: AppProperties) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val properties = appProperties.storage
    private val root: Path = Path.of(properties.root).toAbsolutePath().normalize()

    @PostConstruct
    fun initialize() {
        try {
            Files.createDirectories(root)
            logger.info("File storage root: {}", root)
        } catch (ex: Exception) {
            throw StorageException("Nie można utworzyć katalogu magazynu plików.", ex)
        }
    }

    /**
     * Stores a file under a server-generated key. The original filename is never
     * used as a path, only as metadata.
     */
    fun storeImage(patientId: UUID, file: MultipartFile, type: DetectedFileType): StoredFile =
        store(patientId, "images", file, type)

    fun storeDocument(patientId: UUID, file: MultipartFile, type: DetectedFileType): StoredFile =
        store(patientId, "documents", file, type)

    fun storeBytes(patientId: UUID, category: String, extension: String, bytes: ByteArray): StoredFile {
        val key = "patients/$patientId/$category/${UUID.randomUUID()}.${extension.lowercase()}"
        val target = resolveSafe(key)
        try {
            Files.createDirectories(target.parent)
            Files.write(target, bytes)
            return StoredFile(key, sha256(bytes), bytes.size.toLong())
        } catch (ex: Exception) {
            deleteQuietly(key)
            throw StorageException("Nie udało się zapisać pliku.", ex)
        }
    }

    fun load(storageKey: String): Path {
        val path = resolveSafe(storageKey)
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw NotFoundException("Plik nie istnieje w magazynie.")
        }
        return path
    }

    fun openStream(storageKey: String): InputStream = Files.newInputStream(load(storageKey))

    fun delete(storageKey: String) {
        try {
            Files.deleteIfExists(resolveSafe(storageKey))
        } catch (ex: Exception) {
            logger.warn("Could not delete stored file {}: {}", storageKey, ex.javaClass.simpleName)
        }
    }

    fun deleteQuietly(storageKey: String) = delete(storageKey)

    /** Deletes the file only after the surrounding transaction commits successfully. */
    fun deleteAfterCommit(storageKey: String) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = deleteQuietly(storageKey)
            })
        } else {
            deleteQuietly(storageKey)
        }
    }

    /** Removes a newly written file if the surrounding transaction rolls back. */
    fun deleteOnRollback(storageKey: String) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        deleteQuietly(storageKey)
                    }
                }
            })
        }
    }

    fun resolveSafe(storageKey: String): Path {
        val path = root.resolve(storageKey).normalize()
        if (!path.startsWith(root)) {
            throw StorageException("Niedozwolona ścieżka pliku.")
        }
        return path
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun store(patientId: UUID, category: String, file: MultipartFile, type: DetectedFileType): StoredFile {
        val key = "patients/$patientId/$category/${UUID.randomUUID()}.${type.extension}"
        val target = resolveSafe(key)
        try {
            Files.createDirectories(target.parent)
            val digest = MessageDigest.getInstance("SHA-256")
            // Stream to a temporary file in the same directory, hashing while copying.
            val temp = Files.createTempFile(target.parent, "upload-", ".tmp")
            try {
                file.inputStream.use { input ->
                    Files.newOutputStream(temp).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var read: Int
                        var total = 0L
                        while (input.read(buffer).also { read = it } != -1) {
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            total += read
                            if (total > properties.maxFileSizeBytes) {
                                throw StorageException("Plik przekracza dozwolony rozmiar.")
                            }
                        }
                    }
                }
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(temp)
            }
            return StoredFile(key, digest.digest().joinToString("") { "%02x".format(it) }, file.size)
        } catch (ex: StorageException) {
            deleteQuietly(key)
            throw ex
        } catch (ex: Exception) {
            deleteQuietly(key)
            throw StorageException("Nie udało się zapisać pliku.", ex)
        }
    }
}
