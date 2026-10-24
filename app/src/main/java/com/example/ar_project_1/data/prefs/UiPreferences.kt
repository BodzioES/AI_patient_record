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

    private companion object {
        const val PREFS_NAME = "ui_preferences"
        const val KEY_WELCOME_SEEN = "welcome_seen"
    }
}
