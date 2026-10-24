package com.example.ar_project_1.data.api

import com.example.ar_project_1.data.session.SessionManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class ServerUrlInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val serverUrl = sessionManager.current().serverUrl
            ?: throw IOException("Nie wykryto adresu serwera. Sprawdź połączenie z siecią.")
        val base = serverUrl.toHttpUrlOrNull()
            ?: throw IOException("Nieprawidłowy adres serwera.")
        val original = chain.request().url
        val rewritten = base.newBuilder()
            .encodedPath(original.encodedPath)
            .encodedQuery(original.encodedQuery)
            .build()
        return chain.proceed(
            chain.request().newBuilder()
                .url(rewritten)
                .build(),
        )
    }
}
