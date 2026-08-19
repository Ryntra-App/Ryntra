package com.ryntra.shared.model

/**
 * Rewrites the HTML that Modrinth allows inside project descriptions into equivalent Markdown.
 *
 * Modrinth renders descriptions with `markdown-it` configured `html: true` and then sanitizes the
 * result against a tag whitelist, so raw HTML genuinely displays on the site. Creators rely on it
 * heavily — `<img>` badge rows, `<center>` banners and `<details>` changelogs are everywhere.
 * Without this pass those tags reach the reader as literal text.
 *
 * Translating to Markdown up front rather than teaching the renderers about HTML keeps every
 * existing image, span and heading rule working unchanged, on both platforms at once.
 */
internal object MarkdownHtml {
    fun normalize(markdown: String): String {
        if (!markdown.containsHtml()) return markdown.normalizeHardBreaks()
        return buildString {
            var insideFence = false
            var fenceMarker = ""
            markdown.lines().forEachIndexed { index, line ->
                if (index > 0) append('\n')
                val fence = line.trimStart().fenceMarkerOrNull()
                if (fence != null) {
                    // A fence closes only on its own marker, so ``` inside a ~~~ block is content.
                    if (!insideFence) {
                        insideFence = true
                        fenceMarker = fence
                    } else if (fence == fenceMarker) {
                        insideFence = false
                    }
                    append(line)
                    return@forEachIndexed
                }
                append(if (insideFence) line else line.rewriteOutsideCodeSpans())
            }
        }.normalizeHardBreaks()
    }

    private fun String.containsHtml(): Boolean = '<' in this || '&' in this

    private fun String.fenceMarkerOrNull(): String? = when {
        startsWith("```") -> "```"
        startsWith("~~~") -> "~~~"
        else -> null
    }

    /** Inline code is verbatim on Modrinth too, so `<img>` inside backticks must stay as text. */
    private fun String.rewriteOutsideCodeSpans(): String {
        if ('`' !in this) return rewriteHtml()
        return buildString {
            var index = 0
            while (index < this@rewriteOutsideCodeSpans.length) {
                val open = this@rewriteOutsideCodeSpans.indexOf('`', index)
                if (open < 0) {
                    append(this@rewriteOutsideCodeSpans.substring(index).rewriteHtml())
                    return@buildString
                }
                val close = this@rewriteOutsideCodeSpans.indexOf('`', open + 1)
                if (close < 0) {
                    append(this@rewriteOutsideCodeSpans.substring(index).rewriteHtml())
                    return@buildString
                }
                append(this@rewriteOutsideCodeSpans.substring(index, open).rewriteHtml())
                append(this@rewriteOutsideCodeSpans.substring(open, close + 1))
                index = close + 1
            }
        }
    }

    private fun String.rewriteHtml(): String = this
        // Two trailing spaces are a Markdown hard break; a bare newline would be soft-wrapped
        // back into the same paragraph, which is not what <br> means.
        .replace(lineBreakTag, HARD_BREAK + "\n")
        .rewriteImages()
        .rewriteLinks()
        .rewriteHeadings()
        .rewriteInlineEmphasis()
        .rewriteSummaries()
        .replace(strippedTag, "")
        .decodeEntities()

    /** `<img src alt>` becomes a Markdown image so the existing badge and gallery rules apply. */
    private fun String.rewriteImages(): String = imageTag.replace(this) { match ->
        val attributes = match.value.attributes()
        val source = attributes["src"].orEmpty()
        if (source.isBlank()) "" else "![${attributes["alt"].orEmpty()}]($source)"
    }

    private fun String.rewriteLinks(): String = anchorTag.replace(this) { match ->
        val href = match.value.attributes()["href"].orEmpty()
        val inner = match.groupValues[1].trim()
        when {
            inner.isEmpty() -> ""
            href.isBlank() -> inner
            else -> "[$inner]($href)"
        }
    }

    private fun String.rewriteHeadings(): String = headingTag.replace(this) { match ->
        val level = match.groupValues[1].toIntOrNull()?.coerceIn(1, 6) ?: 1
        val text = match.groupValues[2].trim()
        if (text.isEmpty()) "" else "\n${"#".repeat(level)} $text\n"
    }

    /**
     * `<details>` has no collapsible equivalent here, so its `<summary>` becomes a heading and the
     * body stays visible. Expanded content reads better on a phone than a control that hides it.
     */
    private fun String.rewriteSummaries(): String = summaryTag.replace(this) { match ->
        val text = match.groupValues[1].trim()
        if (text.isEmpty()) "" else "\n### $text\n"
    }

    private fun String.rewriteInlineEmphasis(): String {
        var result = this
        emphasisTags.forEach { (tag, marker) ->
            result = Regex("<$tag(?:\\s[^>]*)?>(.*?)</$tag\\s*>", regexOptions).replace(result) { match ->
                val inner = match.groupValues[1]
                if (inner.isBlank()) inner else "$marker$inner$marker"
            }
        }
        return result
    }

    private fun String.decodeEntities(): String {
        if ('&' !in this) return this
        var result = this
        entities.forEach { (entity, replacement) -> result = result.replace(entity, replacement) }
        return numericEntity.replace(result) { match ->
            val code = match.groupValues[1]
            val value = if (code.startsWith("x") || code.startsWith("X")) {
                code.drop(1).toIntOrNull(16)
            } else {
                code.toIntOrNull()
            }
            // Out-of-range values would crash the char conversion; leaving them alone is harmless.
            if (value != null && value in 1..0xFFFF) value.toChar().toString() else match.value
        }
    }

    /**
     * A trailing backslash is a Markdown hard break, and a line holding nothing else is left over
     * from one. Both reach the reader as a stray `\` unless they are resolved here.
     */
    private fun String.normalizeHardBreaks(): String = lines()
        .map { line ->
            when {
                line.trim() == "\\" -> ""
                line.trimEnd().endsWith("\\") -> line.trimEnd().dropLast(1).trimEnd() + HARD_BREAK
                else -> line
            }
        }
        .joinToString("\n")

    private fun String.attributes(): Map<String, String> = attribute.findAll(this).associate { match ->
        val name = match.groupValues[1].lowercase()
        val value = match.groupValues[2].ifEmpty { match.groupValues[3] }.ifEmpty { match.groupValues[4] }
        name to value.trim()
    }

    /** Markdown spells a hard line break as two trailing spaces. */
    private const val HARD_BREAK = "  "

    private val regexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)

    private val lineBreakTag = Regex("<br\\s*/?>", regexOptions)
    private val imageTag = Regex("<img\\s[^>]*/?>", regexOptions)
    private val anchorTag = Regex("<a\\s[^>]*>(.*?)</a\\s*>", regexOptions)
    private val headingTag = Regex("<h([1-6])(?:\\s[^>]*)?>(.*?)</h[1-6]\\s*>", regexOptions)
    private val summaryTag = Regex("<summary(?:\\s[^>]*)?>(.*?)</summary\\s*>", regexOptions)
    private val attribute = Regex("""([a-zA-Z_:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""")
    private val numericEntity = Regex("&#(x?[0-9a-fA-F]+);")

    /** Anything left after the rewrites above is layout Modrinth applies and the apps cannot. */
    private val strippedTag = Regex("</?[a-zA-Z][a-zA-Z0-9-]*(?:\\s[^>]*)?/?>", regexOptions)

    private val emphasisTags = listOf(
        "strong" to "**",
        "b" to "**",
        "em" to "*",
        "i" to "*",
        "del" to "~~",
        "s" to "~~",
        "strike" to "~~",
        "code" to "`",
        "kbd" to "`",
    )

    private val entities = listOf(
        "&nbsp;" to " ",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&lt;" to "<",
        "&gt;" to ">",
        "&mdash;" to "—",
        "&ndash;" to "–",
        "&hellip;" to "…",
        // Ampersand goes last so a decoded value cannot be decoded a second time.
        "&amp;" to "&",
    )
}
