package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val slug: String? = null,
    val title: String,
    val description: String = "",
    val body: String = "",
    val categories: List<String> = emptyList(),
    @SerialName("project_type") val projectType: String = "project",
    @SerialName("client_side") val clientSide: String = "unknown",
    @SerialName("server_side") val serverSide: String = "unknown",
    @SerialName("icon_url") val iconUrl: String? = null,
    val downloads: Long = 0,
    val followers: Long = 0,
    val status: String = "unknown",
    val updated: String? = null,
    val team: String? = null,
    val organization: String? = null,
    val license: ProjectLicense? = null,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("issues_url") val issuesUrl: String? = null,
    @SerialName("wiki_url") val wikiUrl: String? = null,
    @SerialName("discord_url") val discordUrl: String? = null,
    val published: String? = null,
    val gallery: List<GalleryImage> = emptyList(),
)

@Serializable
data class ProjectLicense(
    val id: String,
    val name: String? = null,
    val url: String? = null,
)

@Serializable
data class GalleryImage(
    val url: String,
    @SerialName("raw_url") val rawUrl: String? = null,
    val featured: Boolean = false,
    val title: String? = null,
    val description: String? = null,
    val created: String? = null,
    val ordering: Int = 0,
)
