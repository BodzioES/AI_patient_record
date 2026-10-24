package com.example.ar_project_1.data.session

data class StoredSession(
    val serverUrl: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val patientId: String? = null,
) {
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()
}
