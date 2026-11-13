package com.sudharo.provider.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudharo.provider.data.repository.SudharoRepository
import com.sudharo.provider.domain.model.Bid
import com.sudharo.provider.domain.model.Job
import com.sudharo.provider.domain.model.ProviderProfile
import com.sudharo.provider.domain.model.Quotation
import com.sudharo.provider.domain.model.Requirement
import com.sudharo.provider.util.PdfGenerator
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One ViewModel per feature area would be ideal; this single holder keeps
 *  the first build simple and is split by feature in v1.1.
 *  All state is StateFlow, collected lifecycle-aware in Compose. */
class ProviderViewModel(private val repo: SudharoRepository) : ViewModel() {
    val demoMode: Boolean get() = repo.demoMode
    val requirements: StateFlow<List<Requirement>> = repo.requirements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val jobs: StateFlow<List<Job>> = repo.jobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val quotations: StateFlow<List<Quotation>> = repo.quotations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notifications = repo.notifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val portfolio = repo.portfolio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val message = MutableStateFlow<String?>(null)
    val profile = MutableStateFlow<ProviderProfile?>(null)

    init { viewModelScope.launch { repo.seedIfEmpty(); profile.value = repo.profile() } }

    fun signUp(name: String, email: String, password: String, onOk: () -> Unit) = viewModelScope.launch {
        repo.signUp(name, email, password)
            .onSuccess { message.value = if (repo.demoMode) "Demo mode: OTP is 123456" else "OTP sent to $email"; onOk() }
            .onFailure { message.value = it.message }
    }
    fun verifyOtp(email: String, code: String, onOk: () -> Unit) = viewModelScope.launch {
        repo.verifyOtp(email, code).onSuccess { onOk() }.onFailure { message.value = it.message }
    }
    fun signIn(email: String, password: String, onOk: (String) -> Unit) = viewModelScope.launch {
        repo.signIn(email, password).onSuccess { onOk(it) }.onFailure { message.value = it.message }
    }
    fun submitBid(req: Requirement, amount: Int, days: Int, notes: String, onOk: () -> Unit) = viewModelScope.launch {
        repo.submitBid(req, amount, days, notes)
            .onSuccess { _: Bid -> message.value = "Bid submitted — Rs $amount"; onOk() }
            .onFailure { message.value = it.message }
    }
    fun createQuotation(jobId: String, title: String, labour: Int, material: Int, other: Int, days: Int, terms: String, onOk: (Quotation) -> Unit) = viewModelScope.launch {
        repo.createQuotation(jobId, title, labour, material, other, days, terms)
            .onSuccess { onOk(it) }.onFailure { message.value = it.message }
    }
    fun downloadQuotationPdf(context: Context, q: Quotation) = viewModelScope.launch {
        runCatching { PdfGenerator.saveQuotationPdf(context, q, profile.value?.name ?: "Sudharo Provider") }
            .onSuccess { message.value = "PDF saved to Downloads: $it" }
            .onFailure { message.value = it.message }
    }
    fun requestCompletion(jobId: String) = viewModelScope.launch {
        repo.requestCompletionOtp(jobId)
            .onSuccess { message.value = if (repo.demoMode) "Demo mode: completion OTP is 654321" else "OTP sent to customer" }
            .onFailure { message.value = it.message }
    }
    fun verifyCompletion(jobId: String, code: String, onOk: () -> Unit) = viewModelScope.launch {
        repo.verifyCompletionOtp(jobId, code).onSuccess { message.value = "Job completed"; onOk() }
            .onFailure { message.value = it.message }
    }
    fun saveProfile(p: ProviderProfile, onOk: () -> Unit) = viewModelScope.launch {
        repo.saveProfile(p); profile.value = p; onOk()
    }
    fun addPortfolio(title: String, category: String, uri: String) = viewModelScope.launch {
        repo.addPortfolio(title, category, uri); message.value = "Added to portfolio"
    }
    fun clearMessage() { message.value = null }
}
