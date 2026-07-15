package com.ryntra.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

data class ProjectFileUpload(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
)

object ProjectUploadLimits {
    const val PROJECT_ICON_BYTES = 256 * 1024
    const val USER_AVATAR_BYTES = 2 * 1024 * 1024
    const val GALLERY_IMAGE_BYTES = 5 * 1024 * 1024
    const val VERSION_FILES_BYTES = 128L * 1024 * 1024
}

object ProjectUploadFactory {
    fun fromBase64(fileName: String, contentType: String, base64: String): ProjectFileUpload {
        require(base64.length <= MAX_BASE64_LENGTH) { "The selected file is too large to import." }
        return ProjectFileUpload(
            fileName = fileName,
            contentType = contentType,
            bytes = Base64.decode(base64),
        )
    }

    private const val MAX_BASE64_LENGTH = 180_000_000
}

data class CreateVersionRequest(
    val name: String,
    val versionNumber: String,
    val changelog: String = "",
    val dependencies: List<ProjectDependency> = emptyList(),
    val gameVersions: List<String>,
    val versionType: String = "release",
    val loaders: List<String>,
    val featured: Boolean = false,
    val files: List<ProjectFileUpload>,
    val primaryFileIndex: Int = 0,
)

@Serializable
data class VersionUpdate(
    val name: String? = null,
    @SerialName("version_number") val versionNumber: String? = null,
    val changelog: String? = null,
    val dependencies: List<ProjectDependency>? = null,
    @SerialName("game_versions") val gameVersions: List<String>? = null,
    @SerialName("version_type") val versionType: String? = null,
    val loaders: List<String>? = null,
    val featured: Boolean? = null,
    val status: String? = null,
)

@Serializable
data class ProjectMemberUpdate(
    val role: String? = null,
    val permissions: Int? = null,
    @SerialName("organization_permissions") val organizationPermissions: Int? = null,
    @SerialName("payouts_split") val payoutsSplit: Double? = null,
    val ordering: Int? = null,
)
