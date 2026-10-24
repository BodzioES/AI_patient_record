package com.example.ar_project_1.data.api

import com.example.ar_project_1.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isAuthEndpoint = path.contains("/auth/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/refresh") ||
            path.contains("/auth/logout")
        val token = sessionManager.current().accessToken
        val authorized = if (!isAuthEndpoint && !token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(authorized)
    }
}
