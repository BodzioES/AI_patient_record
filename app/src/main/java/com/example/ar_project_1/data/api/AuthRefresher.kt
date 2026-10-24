package com.example.ar_project_1.data.api

import com.example.ar_project_1.data.model.AuthResponse
import com.example.ar_project_1.data.model.RefreshRequest
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Blocking refresh used from OkHttp's Authenticator thread. It intentionally
 * uses a separate client without the Authenticator to avoid recursion.
 */
class AuthRefresher(private val gson: Gson) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun refresh(serverUrl: String, refreshToken: String): AuthResponse? {
        val body = gson.toJson(RefreshRequest(refreshToken)).toRequestBody(JSON)
        val request = Request.Builder()
            .url(serverUrl + "app/auth/refresh")
            .post(body)
            .header("Accept", "application/json")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val text = response.body?.string() ?: return null
                gson.fromJson(text, AuthResponse::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
