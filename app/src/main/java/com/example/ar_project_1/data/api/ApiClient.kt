package com.example.ar_project_1.data.api

import com.example.ar_project_1.BuildConfig
import com.example.ar_project_1.data.session.SessionManager
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(
    sessionManager: SessionManager,
    gson: Gson,
) {
    val service: ApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(ServerUrlInterceptor(sessionManager))
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(sessionManager, AuthRefresher(gson)))
            .build()

        service = Retrofit.Builder()
            .baseUrl("http://localhost/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
