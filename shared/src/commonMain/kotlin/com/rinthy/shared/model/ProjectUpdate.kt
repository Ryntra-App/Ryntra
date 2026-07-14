package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectUpdate(
    val title: String? = null,
    val description: String? = null,
    val body: String? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    val status: String? = null,
    @SerialName("requested_status") val requestedStatus: String? = null,
    @SerialName("license_id") val licenseId: String? = null,
)
