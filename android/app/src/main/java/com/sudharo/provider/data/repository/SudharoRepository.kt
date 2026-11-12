package com.sudharo.provider.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sudharo.provider.data.local.BidEntity
import com.sudharo.provider.data.local.JobEntity
import com.sudharo.provider.data.local.NotificationEntity
import com.sudharo.provider.data.local.ProviderEntity
import com.sudharo.provider.data.local.QuotationEntity
import com.sudharo.provider.data.local.RequirementEntity
import com.sudharo.provider.data.local.SudharoDatabase
import com.sudharo.provider.data.remote.BidRequest
import com.sudharo.provider.data.remote.QuotationRequest
import com.sudharo.provider.data.remote.SudharoApi
import com.sudharo.provider.data.remote.SupabaseAuthService
import com.sudharo.provider.domain.model.Bid
import com.sudharo.provider.domain.model.Job
import com.sudharo.provider.domain.model.ProviderProfile
import com.sudharo.provider.domain.model.Quotation
import com.sudharo.provider.domain.model.Requirement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.sessionStore by preferencesDataStore(name = "sudharo_session")
private val KEY_TOKEN = stringPreferencesKey("access_token")
private val KEY_EMAIL = stringPreferencesKey("email")
private val KEY_NAME = stringPreferencesKey("name")

/**
 * Single source of truth. Offline-first: Room is read first, server is
 * synced when a token exists. In demo mode (no Supabase keys) everything
 * works locally — bids, quotations, jobs and profile persist in Room and
 * survive app restarts. Sample requirements are seeded once and labelled
 * as sample data in README/docs.
 */
class SudharoRepository(
    private val db: SudharoDatabase,
    private val api: SudharoApi,
    private val auth: SupabaseAuthService,
    private val context: Context
) {
    private val dao get() = db.dao()
    val demoMode: Boolean get() = !auth.isConfigured

    // --- Session ---
    suspend fun saveSession(token: String, email: String, name: String) {
        context.sessionStore.edit { it[KEY_TOKEN] = token; it[KEY_EMAIL] = email; it[KEY_NAME] = name }
    }
    suspend fun clearSession() { context.sessionStore.edit { it.clear() } }
    suspend fun sessionEmail(): String? = context.sessionStore.data.first()[KEY_EMAIL]
    suspend fun authToken(): String? = context.sessionStore.data.first()[KEY_TOKEN]

    // --- Auth (Supabase when configured, local demo account otherwise) ---
    suspend fun signUp(name: String, email: String, password: String): Result<Unit> = runCatching {
        if (auth.isConfigured) {
            auth.signUp(email, password) // Supabase sends the 6-digit OTP email
            saveSession("", email, name)
        } else {
            // Local demo account: stored in Room profile; OTP step still shown,
            // code is 123456 in demo mode only — documented in README/SETUP.
            dao.saveProfile(ProviderEntity(name = name, email = email))
            saveSession("demo", email, name)
        }
    }
    suspend fun verifyOtp(email: String, code: String): Result<Unit> = runCatching {
        if (auth.isConfigured) {
            val r = auth.verifyEmailOtp(email, code)
            saveSession(r.accessToken, email, sessionEmail() ?: email)
        } else {
            require(code == "123456") { "Demo OTP is 123456 (demo mode only). Add Supabase keys for real OTP." }
        }
    }
    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        if (auth.isConfigured) {
            val r = auth.signIn(email, password)
            saveSession(r.accessToken, email, r.email)
            email
        } else {
            val p = dao.profile()
            require(p != null && p.email.equals(email, ignoreCase = true)) { "No local demo account for this email. Sign up first (demo mode)." }
            saveSession("demo", email, p.name)
            p.name
        }
    }

    // --- Requirements ---
    fun requirements(): Flow<List<Requirement>> = dao.requirements().map { list ->
        list.map { Requirement(it.id, it.title, it.category, it.location, it.budgetMin, it.budgetMax, it.daysLeft, it.description, it.postedAgo, it.status) }
    }
    suspend fun seedIfEmpty() {
        if (dao.requirements().first().isNotEmpty()) return
        dao.insertRequirements(
            listOf(
                RequirementEntity("REQ-001", "Bathroom fitting work", "Plumbing", "Mumbai, MH", 8000, 12000, 2, "Install new bathroom fittings including tap, sink and shower.", "2h ago"),
                RequirementEntity("REQ-002", "Kitchen tap installation", "Plumbing", "Mumbai, MH", 3000, 4500, 2, "Replace kitchen tap, check leakage.", "2h ago"),
                RequirementEntity("REQ-003", "House painting (2 rooms)", "Painting", "Mumbai, MH", 8000, 12000, 3, "Paint 2 rooms, walls + ceiling.", "5h ago"),
                RequirementEntity("REQ-004", "AC service & cleaning", "Electrical", "Mumbai, MH", 1500, 2500, 1, "AC gas check + deep cleaning.", "1d ago"),
                RequirementEntity("REQ-005", "Electrical wiring", "Electrical", "Mumbai, MH", 10000, 14000, 4, "Full wiring for 2BHK.", "1d ago")
            )
        )
        dao.insertJobs(
            listOf(
                JobEntity("SDH-2026-0012", "Bathroom fitting work", 9500, 1, "in_progress", "Today"),
                JobEntity("SDH-2026-0011", "Electrical wiring", 12000, 2, "pending", "Yesterday")
            )
        )
        dao.insertNotifications(
            listOf(
                NotificationEntity("N1", "New requirement available", "Kitchen tap installation in Mumbai", "2m ago"),
                NotificationEntity("N2", "Quotation accepted", "Your quotation for Bathroom fitting work has been accepted", "1h ago"),
                NotificationEntity("N3", "Job reminder", "Complete Electrical wiring by tomorrow", "3h ago"),
                NotificationEntity("N4", "New review", "You received a 5-star review!", "1d ago")
            )
        )
    }

    // --- Bids: one bid per requirement, persisted; server sync when logged in ---
    suspend fun submitBid(req: Requirement, amount: Int, days: Int, notes: String): Result<Bid> = runCatching {
        require(amount > 0) { "Bid amount must be greater than 0" }
        require(days in 1..90) { "Estimated time must be 1–90 days" }
        require(dao.bidCountFor(req.id) == 0) { "You have already bid on this requirement" }
        val bid = Bid("BID-${UUID.randomUUID().toString().take(8).uppercase()}", req.id, req.title, amount, days, notes)
        dao.insertBid(BidEntity(bid.id, bid.requirementId, bid.requirementTitle, bid.amount, bid.days, bid.notes))
        authToken()?.takeIf { it != "demo" && it.isNotBlank() }?.let { token ->
            try { api.submitBid("Bearer $token", req.id, BidRequest(amount, days, notes)) } catch (_: Exception) { /* offline: local bid stays, sync later */ }
        }
        bid
    }

    // --- Jobs / completion OTP (server-verified in production) ---
    fun jobs(): Flow<List<Job>> = dao.jobs().map { it.map { j -> Job(j.id, j.title, j.amount, j.days, j.status, j.date) } }
    suspend fun requestCompletionOtp(jobId: String): Result<Unit> = runCatching {
        val token = authToken()
        if (token != null && token != "demo") {
            api.requestCompletion("Bearer $token", jobId) // backend creates OTP + notifies customer
        } // demo mode: OTP is shown in-app in the completion screen (documented)
    }
    suspend fun verifyCompletionOtp(jobId: String, code: String): Result<Unit> = runCatching {
        val token = authToken()
        if (token != null && token != "demo") {
            api.verifyCompletion("Bearer $token", jobId, com.sudharo.provider.data.remote.CompletionVerifyRequest(code))
        } else {
            require(code == "654321") { "Demo completion OTP is 654321 (demo mode only)." }
        }
        dao.completeJob(jobId)
    }

    // --- Quotations: new version on revision, never overwrite an accepted one ---
    fun quotations(): Flow<List<Quotation>> = dao.quotations().map { it.map { q -> Quotation(q.id, q.jobId, q.title, q.labour, q.material, q.other, q.timelineDays, q.terms, q.version, q.status) } }
    suspend fun createQuotation(jobId: String, title: String, labour: Int, material: Int, other: Int, days: Int, terms: String): Result<Quotation> = runCatching {
        require(labour + material + other > 0) { "Total must be greater than 0" }
        val existing = dao.quotations().first().filter { it.jobId == jobId }
        require(existing.none { it.status == "accepted" }) { "An accepted quotation exists — create a new version only if the customer asks for a revision" }
        val version = (existing.maxOfOrNull { it.version } ?: 0) + 1
        val q = Quotation("QT-${UUID.randomUUID().toString().take(8).uppercase()}", jobId, title, labour, material, other, days, terms, version, "sent")
        dao.insertQuotation(QuotationEntity(q.id, q.jobId, q.title, q.labour, q.material, q.other, q.timelineDays, q.terms, q.version, q.status))
        authToken()?.takeIf { it != "demo" && it.isNotBlank() }?.let { token ->
            try { api.createQuotation("Bearer $token", QuotationRequest(jobId, title, labour, material, other, days, terms)) } catch (_: Exception) { }
        }
        q
    }

    // --- Profile / portfolio / notifications ---
    suspend fun profile(): ProviderProfile? {
        val p = dao.profile() ?: return null
        val jobs = dao.jobs().first()
        val done = jobs.count { it.status == "completed" }
        return ProviderProfile(name = p.name, email = p.email, phone = p.phone, city = p.city, categories = p.categories.split(",").filter { it.isNotBlank() }, experienceYears = p.experienceYears, about = p.about, completedJobs = done, successRate = if (jobs.isEmpty()) 98 else (done * 100 / jobs.size).coerceIn(0, 100))
    }
    suspend fun saveProfile(p: ProviderProfile) {
        dao.saveProfile(ProviderEntity(name = p.name, email = p.email, phone = p.phone, city = p.city, categories = p.categories.joinToString(","), experienceYears = p.experienceYears, about = p.about))
    }
    fun portfolio() = dao.portfolio()
    suspend fun addPortfolio(title: String, category: String, uri: String) {
        dao.insertPortfolio(com.sudharo.provider.data.local.PortfolioEntity("PF-${UUID.randomUUID().toString().take(8).uppercase()}", title, category, uri))
    }
    fun notifications() = dao.notifications()
    suspend fun markNotificationRead(id: String) = dao.markNotificationRead(id)
}
