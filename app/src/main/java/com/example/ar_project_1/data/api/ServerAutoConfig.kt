package com.example.ar_project_1.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Ustala adres backendu bez pytania użytkownika.
 *
 * Kolejność kandydatów zależy od typu urządzenia:
 * - emulator: najpierw 10.0.2.2 (host widziany z emulatora), potem 127.0.0.1,
 * - telefon: najpierw 127.0.0.1 (przez `adb reverse tcp:8080 tcp:8080`),
 *   potem 10.0.2.2.
 *
 * Wygrywa pierwszy adres, który odpowie na `/actuator/health`. Jeśli żaden nie
 * odpowiada, zwracany jest pierwszy kandydat — aplikacja pokaże wtedy normalny
 * błąd połączenia przy logowaniu, zamiast blokować start.
 */
class ServerAutoConfig(isEmulator: Boolean) {

    private val orderedCandidates = candidates(isEmulator)

    private val client = OkHttpClient.Builder()
        .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    /** Zwraca pierwszy działający adres (albo pierwszy kandydat, gdy nic nie odpowiada). */
    suspend fun resolve(): String = withContext(Dispatchers.IO) {
        firstReachable() ?: orderedCandidates.first()
    }

    /**
     * Zwraca adres, który trzeba zapisać, albo `null`, gdy obecny adres jest nadal
     * poprawny. Dzięki temu nie nadpisujemy działającej konfiguracji i nie
     * wylogowujemy użytkownika bez powodu, ale potrafimy naprawić stary adres
     * (np. z błędnym portem), gdy przestanie odpowiadać.
     */
    suspend fun resolveFor(currentUrl: String?): String? = withContext(Dispatchers.IO) {
        if (currentUrl.isNullOrBlank()) {
            return@withContext firstReachable() ?: orderedCandidates.first()
        }
        if (isReachable(currentUrl)) return@withContext null
        val detected = firstReachable() ?: return@withContext null
        if (detected == currentUrl) null else detected
    }

    private fun firstReachable(): String? = orderedCandidates.firstOrNull { isReachable(it) }

    private fun isReachable(baseUrl: String): Boolean = runCatching {
        val request = Request.Builder()
            .url(healthUrl(baseUrl))
            .get()
            .build()
        client.newCall(request).execute().use { it.isSuccessful }
    }.getOrDefault(false)

    /** Buduje `.../actuator/health` niezależnie od tego, czy adres zawiera `/api/v1/`. */
    private fun healthUrl(baseUrl: String): String {
        val parsed = baseUrl.toHttpUrlOrNull() ?: return "$baseUrl/actuator/health"
        return parsed.newBuilder()
            .encodedPath("/actuator/health")
            .query(null)
            .build()
            .toString()
    }

    companion object {
        const val LOOPBACK = "http://127.0.0.1:8080"
        const val EMULATOR_HOST = "http://10.0.2.2:8080"

        private const val PROBE_TIMEOUT_SECONDS = 2L

        fun candidates(isEmulator: Boolean): List<String> =
            if (isEmulator) listOf(EMULATOR_HOST, LOOPBACK) else listOf(LOOPBACK, EMULATOR_HOST)
    }
}
