package com.ryntra.shared.model

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
    val loaders: List<String> = emptyList(),
    @SerialName("client_side") val clientSide: String = "unknown",
    @SerialName("server_side") val serverSide: String = "unknown",
    @SerialName("icon_url") val iconUrl: String? = null,
    val downloads: Long = 0,
    val followers: Long = 0,
    /**
     * Project visibility / review status from Modrinth.
     * Allowed values (OpenAPI): approved, archived, rejected, draft, unlisted,
     * processing, withheld, scheduled, private, unknown.
     */
    val status: String = "unknown",
    /**
     * Target status after review or schedule.
     * Allowed values (OpenAPI): approved, archived, unlisted, private, draft.
     */
    @SerialName("requested_status") val requestedStatus: String? = null,
    @SerialName("moderator_message") val moderatorMessage: ModeratorMessage? = null,
    /** ISO date when the project was submitted to moderators for review. */
    val queued: String? = null,
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
) {
    val bannerUrl: String?
        get() = gallery
            .minWithOrNull(compareByDescending<GalleryImage> { it.featured }.thenBy { it.ordering })
            ?.url

    /**
     * Modrinth often returns plugins as `project_type = "mod"` and distinguishes them via loaders
     * (bukkit/paper/spigot/...). Resolve a display kind from both fields.
     */
    fun displayKind(): ProjectDisplayKind = ProjectDisplayKind.from(projectType, loaders, categories)

    fun needsAttention(): Boolean = attentionState().needsAttention

    fun attentionState(): ProjectAttentionState = ProjectAttentionState.from(this)
}

@Serializable
data class ModeratorMessage(
    val message: String? = null,
    val body: String? = null,
) {
    fun displayText(): String? =
        message?.takeIf { it.isNotBlank() } ?: body?.takeIf { it.isNotBlank() }
}

/**
 * Maps Modrinth project [status] + moderator feedback to UI-facing cases.
 * See https://docs.modrinth.com/api/operations/getproject/
 *
 * **Needs attention** only when moderation failed or left a note that something is wrong
 * (`rejected`, `withheld`, or non-empty `moderator_message`). Quiet `processing` is **not**
 * attention — it belongs in the separate "in review" section.
 */
enum class ProjectAttentionKind {
    /** status=processing, requested_status=approved — submitted for public listing. */
    ReviewForPublication,
    /** status=processing with another or unknown requested_status. */
    InReview,
    Rejected,
    Withheld,
    Scheduled,
    Draft,
    Unlisted,
    Private,
    Archived,
    Approved,
    Unknown,
}

data class ProjectAttentionState(
    val kind: ProjectAttentionKind,
    /** True only when the creator must act (rejection / withhold / mod note). */
    val needsAttention: Boolean,
    /** True while Modrinth staff are reviewing a submission (`status=processing`). */
    val isInReview: Boolean = false,
    /** Optional moderator note from API (`moderator_message`). */
    val moderatorNote: String? = null,
) {
    companion object {
        fun from(project: Project): ProjectAttentionState {
            val status = project.status.lowercase()
            val requested = project.requestedStatus?.lowercase()
            val note = project.moderatorMessage?.displayText()
            val hasModeratorNote = !note.isNullOrBlank()
            return when (status) {
                "approved" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Approved,
                    needsAttention = false,
                )
                "archived" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Archived,
                    needsAttention = false,
                )
                "processing" -> {
                    val kind = if (requested == "approved" || requested == "listed") {
                        ProjectAttentionKind.ReviewForPublication
                    } else {
                        ProjectAttentionKind.InReview
                    }
                    // Quiet processing = "In review" only.
                    // Needs attention only when staff left a moderator note.
                    ProjectAttentionState(
                        kind = kind,
                        needsAttention = hasModeratorNote,
                        isInReview = !hasModeratorNote,
                        moderatorNote = note,
                    )
                }
                "rejected" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Rejected,
                    needsAttention = true,
                    moderatorNote = note,
                )
                "withheld" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Withheld,
                    needsAttention = true,
                    moderatorNote = note,
                )
                "scheduled" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Scheduled,
                    needsAttention = false,
                    moderatorNote = note,
                )
                "draft" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Draft,
                    needsAttention = false,
                )
                "unlisted" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Unlisted,
                    needsAttention = false,
                )
                "private" -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Private,
                    needsAttention = false,
                )
                else -> ProjectAttentionState(
                    kind = ProjectAttentionKind.Unknown,
                    needsAttention = hasModeratorNote,
                    moderatorNote = note,
                )
            }
        }
    }
}

fun Project.isInReview(): Boolean = attentionState().isInReview

enum class ProjectDisplayKind {
    Mod,
    Plugin,
    Hybrid,
    Modpack,
    ResourcePack,
    Shader,
    DataPack,
    Server,
    Project,
    ;

    companion object {
        private val pluginLoaders = setOf(
            "bukkit",
            "spigot",
            "paper",
            "purpur",
            "sponge",
            "bungeecord",
            "waterfall",
            "velocity",
            "folia",
        )
        private val modLoaders = setOf(
            "fabric",
            "forge",
            "neoforge",
            "quilt",
            "liteloader",
            "rift",
        )

        fun from(
            projectType: String,
            loaders: List<String> = emptyList(),
            categories: List<String> = emptyList(),
        ): ProjectDisplayKind {
            val normalizedType = projectType.lowercase()
            when (normalizedType) {
                "modpack" -> return Modpack
                "resourcepack" -> return ResourcePack
                "shader" -> return Shader
                "datapack" -> return DataPack
                "plugin" -> return Plugin
                "minecraft_java_server" -> return Server
                "mod" -> Unit
                else -> if (normalizedType != "project" && normalizedType.isNotBlank()) {
                    // Fall through to loader-based classification for unknown types that still carry loaders.
                } else if (loaders.isEmpty() && categories.isEmpty()) {
                    return Project
                }
            }

            val normalizedLoaders = loaders.map { it.lowercase() }.toSet()
            val hasPluginLoader = normalizedLoaders.any { it in pluginLoaders }
            val hasModLoader = normalizedLoaders.any { it in modLoaders }
            val hasDatapackLoader = "datapack" in normalizedLoaders ||
                categories.any { it.equals("datapack", ignoreCase = true) }

            return when {
                hasPluginLoader && hasModLoader -> Hybrid
                hasPluginLoader -> Plugin
                hasDatapackLoader && !hasModLoader && normalizedType != "mod" -> DataPack
                hasDatapackLoader && !hasModLoader && !hasPluginLoader -> DataPack
                hasModLoader || normalizedType == "mod" -> Mod
                normalizedType == "project" || normalizedType.isBlank() -> Project
                else -> Mod
            }
        }
    }
}

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
    /** Classic v2 gallery caption. */
    val title: String? = null,
    /** v3 organization/project payloads use `name` instead of `title`. */
    val name: String? = null,
    val description: String? = null,
    val created: String? = null,
    val ordering: Int = 0,
) {
    val displayTitle: String?
        get() = title?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() }
}
