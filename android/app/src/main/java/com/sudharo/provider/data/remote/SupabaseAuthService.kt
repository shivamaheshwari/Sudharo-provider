package com.sudharo.provider.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Supabase Auth via its REST API (no extra SDK dependency to verify).
 * Docs: supabase.com/docs/guides/auth — signUp / token (password) /
 * verify OTP (type=email, token=6-digit code from {{ .Token }} template).
 *
 * DEMO MODE: when SUPABASE_URL / SUPABASE_ANON_KEY are blank in
 * local.properties, [isConfigured] is false and the repository uses the
 * local account store instead. The app never invents a server login.
 */
class SupabaseAuthService(
    private val supabaseUrl: String,
    private val anonKey: String
) {
    val isConfigured: Boolean get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()
    private val client = OkHttpClient()

    data class AuthResult(val accessToken: String, val userId: String, val email: String)

    private suspend fun post(path: String, json: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val base = supabaseUrl.trimEnd('/')
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$base/auth/v1/$path")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .post(body).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (!resp.isSuccessful) throw IllegalStateException(obj.optString("msg", obj.optString("message", "Supabase error ${resp.code}")))
            obj
        }
    }

    /** signUp — Supabase sends the 6-digit OTP email (template must use {{ .Token }}). */
    suspend fun signUp(email: String, password: String): AuthResult {
        val r = post("signup", JSONObject().put("email", email).put("password", password))
        return AuthResult(r.optString("access_token"), r.optJSONObject("user")?.optString("id").orEmpty(), email)
    }

    /** verifyEmailOtp — type=email, token_hash is NOT used here; token = 6-digit code. */
    suspend fun verifyEmailOtp(email: String, code: String): AuthResult {
        val r = post("verify", JSONObject().put("email", email).put("token", code).put("type", "email"))
        return AuthResult(r.optString("access_token"), r.optJSONObject("user")?.optString("id").orEmpty(), email)
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val r = post("token?grant_type=password", JSONObject().put("email", email).put("password", password))
        return AuthResult(r.optString("access_token"), r.optJSONObject("user")?.optString("id").orEmpty(), email)
    }

    /** Forgot password: Supabase sends a recovery OTP/email; verify with type=recovery. */
    suspend fun resetPassword(email: String) {
        post("recover", JSONObject().put("email", email))
    }

    @Suppress("unused")
    data class OtpConfig(
        @SerializedName("length") val length: Int = 6,
        @SerializedName("expiryMinutes") val expiryMinutes: Int = 5,
        @SerializedName("resendCooldownSeconds") val resendCooldownSeconds: Int = 60,
        @SerializedName("maxAttempts") val maxAttempts: Int = 5
    )
}
