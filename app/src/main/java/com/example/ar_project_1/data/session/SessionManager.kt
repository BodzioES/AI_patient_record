package com.example.ar_project_1.data.session

import com.example.ar_project_1.data.api.ServerUrl
import com.example.ar_project_1.data.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(private val store: SecureSessionStore) {

    private val mutableState = MutableStateFlow(store.read())
    val state: StateFlow<StoredSession> = mutableState.asStateFlow()

    @Volatile
    private var snapshot: StoredSession = mutableState.value

    fun current(): StoredSession = snapshot

    @Synchronized
    fun setServerUrl(input: String): StoredSession {
        val normalized = ServerUrl.normalize(input)
        val old = snapshot
        val updated = if (old.serverUrl == normalized) {
            old.copy(serverUrl = normalized)
        } else {
            // Tokens are bound to a server. Never reuse them after a server change.
            StoredSession(serverUrl = normalized)
        }
        persist(updated)
        return updated
    }

    @Synchronized
    fun updateAuth(auth: AuthResponse) {
        val updated = snapshot.copy(
            accessToken = auth.accessToken,
            refreshToken = auth.refreshToken,
            userId = auth.user.id,
            email = auth.user.email,
            displayName = auth.user.displayName,
            patientId = auth.patientId,
        )
        persist(updated)
    }

    @Synchronized
    fun clearAuth() {
        persist(
            snapshot.copy(
                accessToken = null,
                refreshToken = null,
                userId = null,
                email = null,
                displayName = null,
                patientId = null,
            ),
        )
    }

    @Synchronized
    fun clearAll() {
        store.clear()
        snapshot = StoredSession()
        mutableState.value = snapshot
    }

    private fun persist(updated: StoredSession) {
        store.write(updated)
        snapshot = updated
        mutableState.value = updated
    }
}
