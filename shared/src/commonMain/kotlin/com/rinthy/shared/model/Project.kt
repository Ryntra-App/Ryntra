package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val slug: String? = null,
    val title: String,
    val description: String = "",
    @SerialName("project_type") val projectType: String = "project",
    @SerialName("icon_url") val iconUrl: String? = null,
    val downloads: Long = 0,
    val followers: Long = 0,
    val status: String = "unknown",
    val updated: String? = null,
    val team: String? = null,
    val organization: String? = null,
)
