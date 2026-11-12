package com.sudharo.provider.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- DTOs (match backend/src, see docs/API.md) ---
data class RequirementDto(
    val id: String, val title: String, val category: String, val location: String,
    @SerializedName("budgetMin") val budgetMin: Int,
    @SerializedName("budgetMax") val budgetMax: Int,
    val daysLeft: Int, val description: String, val postedAgo: String, val status: String = "open"
)
data class BidRequest(val amount: Int, val days: Int, val notes: String)
data class BidDto(val id: String, val requirementId: String, val amount: Int, val days: Int, val status: String)
data class QuotationRequest(
    val jobId: String, val title: String, val labour: Int, val material: Int,
    val other: Int, val timelineDays: Int, val terms: String
)
data class CompletionRequest(val jobId: String)
data class CompletionVerifyRequest(val otp: String)
data class ApiMessage(val message: String = "", val ok: Boolean = true)

/** NestJS API. Every call needs a Supabase JWT in production; demo mode never calls it. */
interface SudharoApi {
    @GET("requirements") suspend fun requirements(@Header("Authorization") token: String): List<RequirementDto>
    @GET("requirements/{id}") suspend fun requirement(@Header("Authorization") t: String, @Path("id") id: String): RequirementDto
    @POST("requirements/{id}/bids") suspend fun submitBid(@Header("Authorization") t: String, @Path("id") id: String, @Body b: BidRequest): BidDto
    @POST("quotations") suspend fun createQuotation(@Header("Authorization") t: String, @Body q: QuotationRequest): ApiMessage
    @POST("jobs/{id}/completion/request") suspend fun requestCompletion(@Header("Authorization") t: String, @Path("id") id: String): ApiMessage
    @POST("jobs/{id}/completion/verify") suspend fun verifyCompletion(@Header("Authorization") t: String, @Path("id") id: String, @Body b: CompletionVerifyRequest): ApiMessage
}

object ApiClient {
    fun create(baseUrl: String): SudharoApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(SudharoApi::class.java)
    }
}
