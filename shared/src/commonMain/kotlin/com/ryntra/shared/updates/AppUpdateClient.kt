package com.ryntra.shared.updates

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.ryntra.shared.network.createPlatformHttpClient

@Serializable
private data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

@Serializable
private data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

data class AppUpdate(
    val version: String,
    val title: String,
    val notes: String,
    val releaseUrl: String,
    val downloadUrl: String?,
)

class AppUpdateClient internal constructor(
    private val httpClient: HttpClient,
) {
    constructor() : this(createPlatformHttpClient())

    suspend fun latestRelease(assetExtension: String? = null): AppUpdate? {
        val response = httpClient.get(LATEST_RELEASE_URL) {
            accept(ContentType.Application.Json)
        }.body<GitHubReleaseResponse>()
        val version = normalizeVersion(response.tagName) ?: return null
        return AppUpdate(
            version = version,
            title = response.name?.takeIf(String::isNotBlank) ?: "Ryntra $version",
            notes = response.body.orEmpty().trim(),
            releaseUrl = response.htmlUrl ?: RELEASES_URL,
            downloadUrl = response.assets
                .firstOrNull { isSupportedAsset(it.name, assetExtension) }
                ?.downloadUrl,
        )
    }

    companion object {
        const val CURRENT_VERSION = "3.1.1"
        const val RELEASES_URL = "https://github.com/Ryntra-App/Ryntra/releases"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Ryntra-App/Ryntra/releases/latest"

        fun isNewerVersion(candidate: String, current: String = CURRENT_VERSION): Boolean {
            val left = normalizeVersion(candidate)?.split('.')?.map(String::toInt) ?: return false
            val right = normalizeVersion(current)?.split('.')?.map(String::toInt) ?: return false
            return (left + List(3 - left.size) { 0 }).take(3)
                .zip((right + List(3 - right.size) { 0 }).take(3))
                .firstOrNull { it.first != it.second }
                ?.let { it.first > it.second } == true
        }

        private fun normalizeVersion(raw: String): String? {
            val parts = raw.trim().removePrefix("v").split(".")
            if (parts.size !in 1..3 || parts.any { it.toIntOrNull() == null }) return null
            return parts.joinToString(".")
        }

        private fun isSupportedAsset(name: String, assetExtension: String?): Boolean {
            val lower = name.lowercase()
            return when (assetExtension?.lowercase()) {
                "apk" -> lower.endsWith(".apk")
                "ipa" -> lower.endsWith(".ipa")
                else -> lower.endsWith(".apk") || lower.endsWith(".ipa")
            }
        }
    }
}
