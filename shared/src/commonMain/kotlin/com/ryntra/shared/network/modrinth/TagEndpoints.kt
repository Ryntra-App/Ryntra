package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ProjectCategory
import com.ryntra.shared.model.ProjectLicense
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

internal class TagEndpoints(private val client: HttpClient) {
    suspend fun projectTypes(): List<String> = client.get("tag/project_type").decode()
    suspend fun categories(): List<ProjectCategory> = client.get("tag/category").decode()
    suspend fun licenses(): List<ProjectLicense> =
        client.get("tag/license").decode<List<LicenseTagDto>>().map(LicenseTagDto::toProjectLicense)
}

@Serializable
private data class LicenseTagDto(
    val short: String,
    val name: String,
) {
    fun toProjectLicense(): ProjectLicense = ProjectLicense(id = short, name = name)
}
