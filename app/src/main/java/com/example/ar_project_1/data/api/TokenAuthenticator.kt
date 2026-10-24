package com.example.ar_project_1.data.api

import com.example.ar_project_1.data.session.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val refresher: AuthRefresher,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization").isNullOrBlank()) return null
        if (responseCount(response) >= 2) return null

        val path = response.request.url.encodedPath
        if (path.contains("/auth/")) return null

        synchronized(lock) {
            val current = sessionManager.current()
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val access = current.accessToken

            // Another thread may have refreshed already.
            if (!access.isNullOrBlank() && access != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $access")
                    .build()
            }

            val refresh = current.refreshToken
            val serverUrl = current.serverUrl
            if (refresh.isNullOrBlank() || serverUrl.isNullOrBlank()) {
                sessionManager.clearAuth()
                return null
            }

            val refreshed = refresher.refresh(serverUrl, refresh)
            if (refreshed == null) {
                sessionManager.clearAuth()
                return null
            }
            sessionManager.updateAuth(refreshed)
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
