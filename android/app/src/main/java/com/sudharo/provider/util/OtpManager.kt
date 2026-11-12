package com.sudharo.provider.util

/**
 * Job-completion OTP rules — SEPARATE from auth OTP (Supabase).
 * Auth OTP: Supabase Auth, 6 digits, 5 min. Completion OTP: generated +
 * verified by the NestJS backend (POST /jobs/:id/completion/request|verify),
 * with its own expiry, rate limit and audit record. Never reuse a login OTP
 * to approve completed work. This class only holds the client-side timers
 * and attempt counting; the server is the source of truth.
 */
object OtpManager {
    const val LENGTH = 6
    const val EXPIRY_MINUTES = 5
    const val RESEND_COOLDOWN_SECONDS = 60
    const val MAX_ATTEMPTS = 5

    fun isValidFormat(code: String): Boolean =
        code.length == LENGTH && code.all { it.isDigit() }
}
