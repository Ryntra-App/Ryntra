package com.ryntra.shared.model

import kotlinx.serialization.Serializable

/**
 * The only two embed sources Modrinth's sanitizer accepts inside a project description.
 *
 * [key] is a stable string for the platform UIs: Kotlin/Native rewrites enum entry names on
 * export, so the Apple layer matches on this rather than on a bridged case name.
 */
enum class MarkdownEmbedProvider(val key: String) {
    YouTube("youtube"),
    Discord("discord"),
}

@Serializable
data class MarkdownEmbed(
    val provider: MarkdownEmbedProvider,
    /** Video id or Discord server id, depending on [provider]. */
    val embedId: String,
    /** Where opening the card takes the reader. */
    val url: String,
    val thumbnailUrl: String? = null,
)

/**
 * Recognises the `<iframe>` sources Modrinth allows.
 *
 * Modrinth's own whitelist (`packages/utils/parse.ts`) permits YouTube embeds and the Discord
 * widget and nothing else, so anything unrecognised is dropped rather than guessed at.
 */
internal object MarkdownEmbeds {
    fun fromIframeSource(source: String): MarkdownEmbed? {
        val cleaned = source.trim().replace("&amp;", "&")
        youTubeEmbed.find(cleaned)?.let { match ->
            val id = match.groupValues[1]
            return MarkdownEmbed(
                provider = MarkdownEmbedProvider.YouTube,
                embedId = id,
                url = "https://www.youtube.com/watch?v=$id",
                // hqdefault exists for every video; maxresdefault 404s on older uploads.
                thumbnailUrl = "https://img.youtube.com/vi/$id/hqdefault.jpg",
            )
        }
        discordWidget.find(cleaned)?.let { match ->
            val id = match.groupValues[1]
            return MarkdownEmbed(
                provider = MarkdownEmbedProvider.Discord,
                embedId = id,
                // The widget page is the only address derivable from an embed; a real invite
                // code is not part of it.
                url = "https://discord.com/widget?id=$id",
            )
        }
        return null
    }

    private val youTubeEmbed = Regex(
        "^https?://(?:www\\.)?youtube(?:-nocookie)?\\.com/embed/([a-zA-Z0-9_-]{11})",
        RegexOption.IGNORE_CASE,
    )

    private val discordWidget = Regex(
        "^https?://(?:www\\.)?discord\\.com/widget\\?(?:[^#]*&)?id=(\\d{17,20})",
        RegexOption.IGNORE_CASE,
    )
}
