package com.example.ar_project_1.data.api

import com.example.ar_project_1.data.model.AuthResponse
import com.example.ar_project_1.data.model.AiStatusDto
import com.example.ar_project_1.data.model.ChatRequest
import com.example.ar_project_1.data.model.ChatResponse
import com.example.ar_project_1.data.model.ConversationDetailDto
import com.example.ar_project_1.data.model.ConversationDto
import com.example.ar_project_1.data.model.DocumentDto
import com.example.ar_project_1.data.model.ImageDto
import com.example.ar_project_1.data.model.LoginRequest
import com.example.ar_project_1.data.model.LogoutRequest
import com.example.ar_project_1.data.model.MeDto
import com.example.ar_project_1.data.model.PageDto
import com.example.ar_project_1.data.model.PatientDto
import com.example.ar_project_1.data.model.RefreshRequest
import com.example.ar_project_1.data.model.RegisterRequest
import com.example.ar_project_1.data.model.SearchHitDto
import com.example.ar_project_1.data.model.TimelineEventDto
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ApiService {

    @POST("app/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("app/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("app/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse

    @POST("app/auth/logout")
    suspend fun logout(@Body request: LogoutRequest)

    @GET("app/auth/me")
    suspend fun me(): MeDto

    @GET("app/patient")
    suspend fun patient(): PatientDto

    @PUT("app/patient")
    suspend fun updatePatient(@Body body: JsonObject): PatientDto

    @GET("app/documents")
    suspend fun documents(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("sort") sort: String? = null,
    ): PageDto<DocumentDto>

    @GET("app/images")
    suspend fun images(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PageDto<ImageDto>

    @GET
    suspend fun list(
        @Url url: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
        @Query("sort") sort: String? = null,
    ): PageDto<JsonObject>

    @GET("app/measurements")
    suspend fun measurements(
        @Query("type") type: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 200,
        @Query("sort") sort: String = "measuredAt,desc",
    ): PageDto<JsonObject>

    @GET("app/timeline")
    suspend fun timeline(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("types") types: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageDto<TimelineEventDto>

    @GET("app/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageDto<SearchHitDto>

    @GET
    suspend fun get(@Url url: String): JsonObject

    @POST
    suspend fun create(@Url url: String, @Body body: JsonObject): JsonObject

    @PUT
    suspend fun update(@Url url: String, @Body body: JsonObject): JsonObject

    @DELETE
    suspend fun delete(@Url url: String)

    @Multipart
    @POST("app/documents")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("documentType") documentType: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("documentDate") documentDate: RequestBody?,
    ): DocumentDto

    @Multipart
    @POST("app/images")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("category") category: RequestBody,
        @Part("symptomId") symptomId: RequestBody?,
        @Part("capturedAt") capturedAt: RequestBody?,
    ): ImageDto

    @Streaming
    @GET("app/documents/{id}/file")
    suspend fun downloadDocument(@Path("id") id: String): ResponseBody

    @Streaming
    @GET("app/images/{id}/file")
    suspend fun downloadImage(@Path("id") id: String): ResponseBody

    @POST("ai/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("ai/status")
    suspend fun aiStatus(): AiStatusDto

    @GET("ai/conversations")
    suspend fun conversations(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageDto<ConversationDto>

    @GET("ai/conversations/{id}")
    suspend fun conversation(@Path("id") id: String): ConversationDetailDto

    @DELETE("ai/conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String)

    @POST("ai/images/{imageId}/analyze")
    suspend fun analyzeImage(@Path("imageId") imageId: String): JsonObject
}
