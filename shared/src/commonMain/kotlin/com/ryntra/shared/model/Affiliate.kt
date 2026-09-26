package com.ryntra.shared.model

/** One affiliate link and what it produced over the report's range. */
data class AffiliateCodeStats(
    val id: String,
    val clicks: Long = 0,
    val conversions: Long = 0,
    /** US dollars. */
    val revenue: Double = 0.0,
) {
    /** The link Modrinth's dashboard hands out for this code. */
    val link: String get() = "https://modrinth.gg?afl=$id"
}

/**
 * Affiliate performance from `POST /v3/analytics`, which labrinth scopes to the caller's own codes.
 *
 * Listing, naming, creating and revoking codes live on `/_internal/affiliate`, which requires
 * `SESSION_ACCESS` — a scope Modrinth never grants to personal access tokens or OAuth apps. The
 * apps therefore show the codes by id and send the creator to the site to manage them.
 */
data class AffiliateReport(
    val rangeDays: Int,
    /** Busiest first. */
    val codes: List<AffiliateCodeStats> = emptyList(),
) {
    val totalClicks: Long get() = codes.sumOf(AffiliateCodeStats::clicks)
    val totalConversions: Long get() = codes.sumOf(AffiliateCodeStats::conversions)
    val totalRevenue: Double get() = codes.sumOf(AffiliateCodeStats::revenue)
}

const val MODRINTH_AFFILIATE_LINKS_URL = "https://modrinth.com/dashboard/affiliate-links"
