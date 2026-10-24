package com.example.ar_project_1

import android.content.Context
import android.os.Build
import com.example.ar_project_1.data.api.ApiClient
import com.example.ar_project_1.data.api.ServerAutoConfig
import com.example.ar_project_1.data.prefs.UiPreferences
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.session.SecureSessionStore
import com.example.ar_project_1.data.session.SessionManager
import com.example.ar_project_1.data.upload.FileSaver
import com.google.gson.GsonBuilder

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val gson = GsonBuilder().create()
    val sessionManager = SessionManager(SecureSessionStore(appContext))
    val uiPreferences = UiPreferences(appContext)
    val serverAutoConfig = ServerAutoConfig(isEmulator())
    private val apiClient = ApiClient(sessionManager, gson)
    val repository = PatientCardRepository(
        contentResolver = appContext.contentResolver,
        api = apiClient.service,
        sessionManager = sessionManager,
    )
    val fileSaver = FileSaver(appContext)

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.startsWith("sdk_")
}
