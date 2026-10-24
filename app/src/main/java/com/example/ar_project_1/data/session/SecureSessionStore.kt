package com.example.ar_project_1.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores session values in SharedPreferences encrypted with an AES-GCM key kept
 * in the Android Keystore. No token is written in plaintext to disk.
 */
class SecureSessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun read(): StoredSession {
        return try {
            StoredSession(
                serverUrl = decrypt(FIELD_SERVER_URL),
                accessToken = decrypt(FIELD_ACCESS_TOKEN),
                refreshToken = decrypt(FIELD_REFRESH_TOKEN),
                userId = decrypt(FIELD_USER_ID),
                email = decrypt(FIELD_EMAIL),
                displayName = decrypt(FIELD_DISPLAY_NAME),
                patientId = decrypt(FIELD_PATIENT_ID),
            )
        } catch (_: Exception) {
            // A key can be invalidated after a backup/restore or lock screen change.
            clear()
            StoredSession()
        }
    }

    @Synchronized
    fun write(session: StoredSession) {
        prefs.edit(commit = true) {
            putString(FIELD_SERVER_URL, encrypt(session.serverUrl))
            putString(FIELD_ACCESS_TOKEN, encrypt(session.accessToken))
            putString(FIELD_REFRESH_TOKEN, encrypt(session.refreshToken))
            putString(FIELD_USER_ID, encrypt(session.userId))
            putString(FIELD_EMAIL, encrypt(session.email))
            putString(FIELD_DISPLAY_NAME, encrypt(session.displayName))
            putString(FIELD_PATIENT_ID, encrypt(session.patientId))
        }
    }

    @Synchronized
    fun clear() {
        prefs.edit(commit = true) { clear() }
    }

    private fun encrypt(value: String?): String? {
        if (value == null) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val payload = cipher.iv + encrypted
            Base64.encodeToString(payload, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val payload = Base64.decode(value, Base64.NO_WRAP)
        if (payload.size <= IV_SIZE) return null
        val iv = payload.copyOfRange(0, IV_SIZE)
        val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "patient_card_secure_session"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "patient_card_session_aes"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128

        const val FIELD_SERVER_URL = "server_url"
        const val FIELD_ACCESS_TOKEN = "access_token"
        const val FIELD_REFRESH_TOKEN = "refresh_token"
        const val FIELD_USER_ID = "user_id"
        const val FIELD_EMAIL = "email"
        const val FIELD_DISPLAY_NAME = "display_name"
        const val FIELD_PATIENT_ID = "patient_id"
    }
}
