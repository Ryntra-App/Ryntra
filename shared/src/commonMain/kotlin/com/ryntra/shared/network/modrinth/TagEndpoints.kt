package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.GameVersion
import com.ryntra.shared.model.ModLoader
import com.ryntra.shared.model.ProjectCategory
import com.ryntra.shared.model.ProjectLicense
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal class TagEndpoints(private val client: HttpClient) {
    suspend fun projectTypes(): List<String> = client.get("tag/project_type").decode()
    suspend fun categories(): List<ProjectCategory> = client.get("tag/category").decode()
    suspend fun licenses(): List<ProjectLicense> =
        client.get("tag/license").decode<List<LicenseTagDto>>().map(LicenseTagDto::toProjectLicense)

    suspend fun gameVersions(): List<GameVersion> =
        client.get("tag/game_version").decode<List<GameVersionTagDto>>().map(GameVersionTagDto::toModel)

    suspend fun loaders(): List<ModLoader> =
        client.get("tag/loader").decode<List<LoaderTagDto>>().map(LoaderTagDto::toModel)
}

@Serializable
private data class GameVersionTagDto(
    val version: String,
    @SerialName("version_type") val versionType: String = "release",
    val major: Boolean = false,
) {
    fun toModel(): GameVersion = GameVersion(version = version, versionType = versionType, isMajor = major)
}

@Serializable
private data class LoaderTagDto(
    val name: String,
    @SerialName("supported_project_types") val supportedProjectTypes: List<String> = emptyList(),
) {
    fun toModel(): ModLoader = ModLoader(name = name, supportedProjectTypes = supportedProjectTypes)
}

@Serializable
private data class LicenseTagDto(
    val short: String,
    val name: String,
) {
    fun toProjectLicense(): ProjectLicense = ProjectLicense(id = short, name = name)
}
