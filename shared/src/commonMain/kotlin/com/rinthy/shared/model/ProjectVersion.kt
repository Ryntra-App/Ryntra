package com.rinthy.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectVersion(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("author_id") val authorId: String? = null,
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    @SerialName("version_type") val versionType: String = "release",
    val changelog: String = "",
    val dependencies: List<ProjectDependency> = emptyList(),
    @SerialName("game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val featured: Boolean = false,
    val status: String = "listed",
    @SerialName("date_published") val datePublished: String? = null,
    val downloads: Long = 0,
    val files: List<ProjectVersionFile> = emptyList(),
)

@Serializable
data class ProjectDependency(
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("dependency_type") val dependencyType: String = "required",
)

@Serializable
data class ProjectVersionFile(
    val hashes: Map<String, String> = emptyMap(),
    val url: String,
    val filename: String,
    val primary: Boolean = false,
    val size: Long = 0,
)
