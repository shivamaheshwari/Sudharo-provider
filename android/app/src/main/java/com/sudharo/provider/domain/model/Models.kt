package com.sudharo.provider.domain.model

/** Domain models — UI and repository share these, never Room entities directly. */
data class ProviderProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "Mumbai",
    val categories: List<String> = emptyList(),
    val experienceYears: Int = 0,
    val about: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val completedJobs: Int = 0,
    val successRate: Int = 0,
    val verified: Boolean = false
)

enum class RequirementCategory { Plumbing, Electrical, Carpentry, Painting, All }

data class Requirement(
    val id: String,
    val title: String,
    val category: String,
    val location: String,
    val budgetMin: Int,
    val budgetMax: Int,
    val daysLeft: Int,
    val description: String,
    val postedAgo: String,
    val status: String = "open" // open | awarded | closed | expired
)

data class Bid(
    val id: String,
    val requirementId: String,
    val requirementTitle: String,
    val amount: Int,
    val days: Int,
    val notes: String,
    val status: String = "sent" // sent | accepted | rejected
)

data class Job(
    val id: String,
    val title: String,
    val amount: Int,
    val days: Int,
    val status: String, // in_progress | pending | completed | cancelled
    val date: String
)

data class Quotation(
    val id: String,
    val jobId: String,
    val title: String,
    val labour: Int,
    val material: Int,
    val other: Int,
    val timelineDays: Int,
    val terms: String,
    val version: Int = 1,
    val status: String = "draft" // draft | sent | accepted | rejected
) {
    val total: Int get() = labour + material + other
}

data class PortfolioItem(
    val id: String,
    val title: String,
    val category: String,
    val imageUri: String
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val read: Boolean = false
)
