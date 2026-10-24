package com.example.ar_project_1.data.prefs

import android.content.Context

/**
 * Proste preferencje interfejsu (nie zawierają danych medycznych ani tokenów).
 */
class UiPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Czy użytkownik widział już ekran powitalny. */
    var welcomeSeen: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_SEEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WELCOME_SEEN, value).apply()
        }

    /** Skala tekstu wybrana w Ustawieniach (1.0 = domyślna). */
    var textScale: Float
        get() = prefs.getFloat(KEY_TEXT_SCALE, 1.0f)
        set(value) {
            prefs.edit().putFloat(KEY_TEXT_SCALE, value.coerceIn(0.9f, 1.4f)).apply()
        }

    private companion object {
        const val PREFS_NAME = "ui_preferences"
        const val KEY_WELCOME_SEEN = "welcome_seen"
        const val KEY_TEXT_SCALE = "text_scale"
    }
}
