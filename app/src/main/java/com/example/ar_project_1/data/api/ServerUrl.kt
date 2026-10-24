package com.example.ar_project_1.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Normalizes whatever the user typed into a Retrofit base URL ending with
 * /api/v1/. The host and port are kept; any path is replaced.
 */
object ServerUrl {

    fun normalize(input: String): String {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "Adres serwera jest wymagany." }

        val withScheme = if (SCHEME_PATTERN.containsMatchIn(trimmed)) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val parsed = withScheme.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Nieprawidłowy adres serwera.")
        require(parsed.scheme == "http" || parsed.scheme == "https") {
            "Dozwolone są tylko adresy http:// i https://."
        }
        require(parsed.host.isNotBlank()) { "Adres serwera nie zawiera hosta." }

        return parsed.newBuilder()
            .username("")
            .password("")
            .encodedPath("/api/v1/")
            .query(null)
            .fragment(null)
            .build()
            .toString()
    }

    fun display(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.removeSuffix("/api/v1/").removeSuffix("/")
    }

    private val SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
}
