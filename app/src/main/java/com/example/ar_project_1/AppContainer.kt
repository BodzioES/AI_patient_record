package com.example.ar_project_1

import android.content.Context
import com.example.ar_project_1.data.api.ApiClient
import com.example.ar_project_1.data.repository.PatientCardRepository
import com.example.ar_project_1.data.session.SecureSessionStore
import com.example.ar_project_1.data.session.SessionManager
import com.example.ar_project_1.data.upload.FileSaver
import com.google.gson.GsonBuilder

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    val gson = GsonBuilder().create()
    val sessionManager = SessionManager(SecureSessionStore(appContext))
    private val apiClient = ApiClient(sessionManager, gson)
    val repository = PatientCardRepository(
        contentResolver = appContext.contentResolver,
        api = apiClient.service,
        sessionManager = sessionManager,
    )
    val fileSaver = FileSaver(appContext)
}
