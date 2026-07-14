package com.rinthy.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class MarkdownBlock(
    val content: String,
    val type: MarkdownBlockType,
    val level: Int = 0,
    val spans: List<MarkdownSpan> = emptyList(),
    val ordered: Boolean = false,
    val ordinal: Int = 0,
    val url: String? = null,
    val images: List<MarkdownImage> = emptyList(),
    val checked: Boolean? = null,
)

@Serializable
data class MarkdownImage(
    val alt: String,
    val url: String,
    val linkUrl: String? = null,
    val isBadge: Boolean = false,
)

enum class MarkdownBlockType {
    Paragraph,
    Heading,
    ListItem,
    CodeBlock,
    Quote,
    Divider,
    Image,
    Table,
}

@Serializable
data class MarkdownSpan(
    val start: Int,
    val end: Int,
    val type: MarkdownSpanType,
    val linkUrl: String? = null,
)

enum class MarkdownSpanType {
    Bold,
    Italic,
    Strikethrough,
    Code,
    Link,
}

object MarkdownParser {
    fun parse(markdown: String): List<MarkdownBlock> = GfmMarkdownParser.parse(markdown)

    internal fun parseLegacy(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.lines()
        var index = 0

        while (index < lines.size) {
            val raw = lines[index]
            val trimmed = raw.trim()
            val imageRow = parseImageRow(trimmed)
            val mixedImageLine = if (imageRow == null) parseMixedImageLine(trimmed) else null

            when {
                trimmed.isEmpty() -> index++

                // Fenced code block: ``` ... ```
                trimmed.startsWith("```") -> {
                    val language = trimmed.drop(3).trim()
                    val code = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        if (code.isNotEmpty()) code.append('\n')
                        code.append(lines[index])
                        index++
                    }
                    if (index < lines.size) index++ // consume closing fence
                    blocks += MarkdownBlock(
                        content = code.toString(),
                        type = MarkdownBlockType.CodeBlock,
                        url = language.ifBlank { null },
                    )
                }

                // Horizontal rule: ---, ***, ___
                isDivider(trimmed) -> {
                    blocks += MarkdownBlock("", MarkdownBlockType.Divider)
                    index++
                }

                // A line that is entirely image(s) / linked image(s): ![alt](url), [![alt](url)](link), badges
                imageRow != null -> {
                    val images = imageRow
                    blocks += MarkdownBlock(
                        content = images.firstOrNull()?.alt.orEmpty(),
                        type = MarkdownBlockType.Image,
                        url = images.firstOrNull()?.url,
                        images = images,
                    )
                    index++
                }

                // Preserve images that share a line with text instead of treating them as links.
                mixedImageLine != null -> {
                    blocks += mixedImageLine
                    index++
                }

                // Heading: #, ##, ...
                trimmed.startsWith("#") -> {
                    val headingLevel = trimmed.takeWhile { it == '#' }.length
                    val content = trimmed.drop(headingLevel).trim()
                    blocks += inlineBlock(content, MarkdownBlockType.Heading, level = headingLevel.coerceAtMost(MAX_HEADING_LEVEL))
                    index++
                }

                // Blockquote: > text
                trimmed.startsWith(">") -> {
                    val content = trimmed.drop(1).trim()
                    blocks += inlineBlock(content, MarkdownBlockType.Quote)
                    index++
                }

                // Unordered list: - item / * item
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val (content, checked) = parseTaskItem(trimmed.drop(2).trim())
                    blocks += inlineBlock(content, MarkdownBlockType.ListItem, checked = checked)
                    index++
                }

                // Ordered list: 1. item
                orderedMarker(trimmed) != null -> {
                    val (ordinal, rest) = orderedMarker(trimmed)!!
                    blocks += inlineBlock(rest, MarkdownBlockType.ListItem, ordered = true, ordinal = ordinal)
                    index++
                }

                else -> {
                    blocks += inlineBlock(trimmed, MarkdownBlockType.Paragraph)
                    index++
                }
            }
        }

        return blocks
    }

    private fun inlineBlock(
        content: String,
        type: MarkdownBlockType,
        level: Int = 0,
        ordered: Boolean = false,
        ordinal: Int = 0,
        checked: Boolean? = null,
    ): MarkdownBlock {
        val inline = InlineParser(content).parse()
        return MarkdownBlock(
            content = inline.content,
            type = type,
            level = level,
            spans = inline.spans,
            ordered = ordered,
            ordinal = ordinal,
            checked = checked,
        )
    }

    private fun isDivider(trimmed: String): Boolean {
        if (trimmed.length < 3) return false
        val ch = trimmed.first()
        if (ch != '-' && ch != '*' && ch != '_') return false
        return trimmed.all { it == ch }
    }

    /**
     * If [trimmed] consists ENTIRELY of images and/or linked images (badge rows on Modrinth,
     * e.g. `[![github](badge.svg)](https://github.com/...) ![downloads](x)`), returns them.
     * Otherwise returns null so the line is treated as normal text.
     */
    private fun parseImageRow(trimmed: String): List<MarkdownImage>? {
        if (!trimmed.startsWith("![") && !trimmed.startsWith("[![")) return null
        val images = mutableListOf<MarkdownImage>()
        var i = 0
        while (i < trimmed.length) {
            if (trimmed[i].isWhitespace()) {
                i++
                continue
            }
            val linked = matchNestedImage(trimmed, i) ?: matchLinkedImage(trimmed, i)
            if (linked != null) {
                images += linked.first
                i = linked.second
                continue
            }
            val image = matchImage(trimmed, i)
            if (image != null) {
                images += image.first
                i = image.second
                continue
            }
            return null // some non-image content on this line
        }
        return images.ifEmpty { null }
    }

    private fun parseMixedImageLine(text: String): List<MarkdownBlock>? {
        if ("![" !in text) return null
        val blocks = mutableListOf<MarkdownBlock>()
        var cursor = 0
        var textStart = 0
        var foundImage = false

        while (cursor < text.length) {
            val match = matchNestedImage(text, cursor) ?: matchLinkedImage(text, cursor) ?: matchImage(text, cursor)
            if (match == null) {
                cursor++
                continue
            }

            val leadingText = text.substring(textStart, cursor).trim()
            if (leadingText.isNotEmpty()) {
                blocks += inlineBlock(leadingText, MarkdownBlockType.Paragraph)
            }
            blocks += MarkdownBlock(
                content = match.first.alt,
                type = MarkdownBlockType.Image,
                url = match.first.url,
                images = listOf(match.first),
            )
            foundImage = true
            cursor = match.second
            textStart = cursor
        }

        val trailingText = text.substring(textStart).trim()
        if (trailingText.isNotEmpty()) {
            blocks += inlineBlock(trailingText, MarkdownBlockType.Paragraph)
        }
        return blocks.takeIf { foundImage }
    }

    /** Matches `![alt](url)` at [from]; returns the image and the index just past it. */
    private fun matchImage(text: String, from: Int): Pair<MarkdownImage, Int>? {
        if (!text.startsWith("![", from)) return null
        val altEnd = text.indexOf(']', from + 2)
        if (altEnd < 0 || altEnd + 1 >= text.length || text[altEnd + 1] != '(') return null
        val urlEnd = text.indexOf(')', altEnd + 2)
        if (urlEnd < 0) return null
        val url = text.substring(altEnd + 2, urlEnd).trim()
        if (url.isEmpty()) return null
        val alt = text.substring(from + 2, altEnd)
        return MarkdownImage(alt, url, isBadge = looksLikeBadge(alt, url)) to urlEnd + 1
    }

    /** Matches `[![alt](url)](link)` at [from]; returns the linked image and the index just past it. */
    private fun matchLinkedImage(text: String, from: Int): Pair<MarkdownImage, Int>? {
        if (!text.startsWith("[!", from)) return null
        val inner = matchImage(text, from + 1) ?: return null
        val afterImage = inner.second
        if (afterImage >= text.length || text[afterImage] != ']' || afterImage + 1 >= text.length || text[afterImage + 1] != '(') return null
        val linkEnd = text.indexOf(')', afterImage + 2)
        if (linkEnd < 0) return null
        val link = text.substring(afterImage + 2, linkEnd).trim()
        return inner.first.copy(linkUrl = link.ifBlank { null }) to linkEnd + 1
    }

    /**
     * Modrinth descriptions sometimes contain `![![alt](image)](target)`. Although the
     * leading `!` makes this invalid nested-image Markdown, Modrinth renders it as a linked
     * image. Accept it without leaking the trailing `](target)` into the description.
     */
    private fun matchNestedImage(text: String, from: Int): Pair<MarkdownImage, Int>? {
        if (!text.startsWith("![![", from)) return null
        val inner = matchImage(text, from + 2) ?: return null
        val afterImage = inner.second
        if (afterImage >= text.length || text[afterImage] != ']') return null
        if (afterImage + 1 >= text.length || text[afterImage + 1] != '(') return null
        val linkEnd = text.indexOf(')', afterImage + 2)
        if (linkEnd < 0) return null
        val link = text.substring(afterImage + 2, linkEnd).trim()
        return inner.first.copy(
            linkUrl = link.ifBlank { null },
            isBadge = false,
        ) to linkEnd + 1
    }

    private fun orderedMarker(trimmed: String): Pair<Int, String>? {
        val dot = trimmed.indexOf('.')
        if (dot <= 0 || dot + 1 >= trimmed.length || trimmed[dot + 1] != ' ') return null
        val digits = trimmed.substring(0, dot)
        if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
        return digits.toInt() to trimmed.substring(dot + 2).trim()
    }

    private fun parseTaskItem(content: String): Pair<String, Boolean?> {
        if (content.length < 3 || content[0] != '[' || content[2] != ']') return content to null
        return when (content[1]) {
            'x', 'X' -> content.drop(3).trimStart() to true
            ' ' -> content.drop(3).trimStart() to false
            else -> content to null
        }
    }

    private fun looksLikeBadge(alt: String, url: String): Boolean {
        val normalizedUrl = url.lowercase()
        if ("shields.io" in normalizedUrl || "/badge" in normalizedUrl) return true
        val normalizedAlt = alt.lowercase()
        val hasBadgeAlt = BADGE_ALT_HINTS.any { hint ->
            normalizedAlt == hint || normalizedAlt.startsWith("$hint ") || normalizedAlt.startsWith("$hint-")
        }
        return hasBadgeAlt || (
            normalizedUrl.substringBefore('?').endsWith(".svg") &&
                BADGE_ALT_HINTS.any(normalizedAlt::contains)
            )
    }

    private const val MAX_HEADING_LEVEL = 6
    private val BADGE_ALT_HINTS = listOf(
        "github",
        "download",
        "version",
        "license",
        "build",
        "discord",
        "modrinth",
    )
}

private data class ParsedInline(
    val content: String,
    val spans: List<MarkdownSpan>,
)

private data class ActiveStyle(
    val type: MarkdownSpanType,
    val linkUrl: String? = null,
)

private class InlineParser(
    private val source: String,
) {
    private val output = StringBuilder(source.length)
    private val spans = mutableListOf<MarkdownSpan>()

    fun parse(): ParsedInline {
        appendRange(source, emptyList())
        return ParsedInline(output.toString(), spans.sortedWith(compareBy(MarkdownSpan::start, MarkdownSpan::end)))
    }

    private fun appendRange(text: String, styles: List<ActiveStyle>) {
        var index = 0
        var plainStart = 0

        fun flushPlain(endExclusive: Int) {
            if (endExclusive <= plainStart) return
            appendStyled(text.substring(plainStart, endExclusive), styles)
        }

        while (index < text.length) {
            val token = findToken(text, index)
            if (token == null) {
                flushPlain(text.length)
                return
            }

            flushPlain(token.start)
            when (token.type) {
                MarkdownSpanType.Code -> appendStyled(token.label, styles + ActiveStyle(MarkdownSpanType.Code))
                MarkdownSpanType.Link -> {
                    val linkStyles = styles + ActiveStyle(MarkdownSpanType.Link, token.linkUrl)
                    if (token.parseLabel) appendRange(token.label, linkStyles) else appendStyled(token.label, linkStyles)
                }
                else -> appendRange(token.label, styles + ActiveStyle(token.type))
            }
            index = token.endExclusive
            plainStart = index
        }
    }

    private fun appendStyled(text: String, styles: List<ActiveStyle>) {
        if (text.isEmpty()) return
        val start = output.length
        output.append(text)
        val end = output.length
        styles.forEach { style ->
            spans += MarkdownSpan(start, end, style.type, style.linkUrl)
        }
    }

    private fun findToken(text: String, fromIndex: Int): InlineToken? {
        for (index in fromIndex until text.length) {
            if (text.startsWith("**", index)) {
                val end = text.indexOf("**", index + 2)
                if (end >= 0) {
                    return InlineToken(index, end + 2, text.substring(index + 2, end), MarkdownSpanType.Bold)
                }
            }
            if (text.startsWith("~~", index)) {
                val end = text.indexOf("~~", index + 2)
                if (end >= 0) {
                    return InlineToken(index, end + 2, text.substring(index + 2, end), MarkdownSpanType.Strikethrough)
                }
            }
            if (text[index] == '`') {
                val end = text.indexOf('`', index + 1)
                if (end >= 0) {
                    return InlineToken(index, end + 1, text.substring(index + 1, end), MarkdownSpanType.Code)
                }
            }
            if (text[index] == '[') {
                val labelEnd = text.indexOf(']', index + 1)
                if (labelEnd >= 0 && labelEnd + 1 < text.length && text[labelEnd + 1] == '(') {
                    val urlEnd = text.indexOf(')', labelEnd + 2)
                    if (urlEnd >= 0) {
                        return InlineToken(
                            start = index,
                            endExclusive = urlEnd + 1,
                            label = text.substring(index + 1, labelEnd),
                            type = MarkdownSpanType.Link,
                            linkUrl = text.substring(labelEnd + 2, urlEnd),
                        )
                    }
                }
            }
            if (text.startsWith("https://", index) || text.startsWith("http://", index)) {
                val end = text.indexOfFirstFrom(index) { it.isWhitespace() || it == ')' || it == ']' }
                val urlEnd = if (end < 0) text.length else end
                val url = text.substring(index, urlEnd).trimEnd('.', ',', ';', ':')
                return InlineToken(index, index + url.length, url, MarkdownSpanType.Link, url, parseLabel = false)
            }
            if (text[index] == '*' && !text.startsWith("**", index)) {
                val end = text.indexOf('*', index + 1)
                if (end >= 0) {
                    return InlineToken(index, end + 1, text.substring(index + 1, end), MarkdownSpanType.Italic)
                }
            }
        }
        return null
    }
}

private inline fun String.indexOfFirstFrom(fromIndex: Int, predicate: (Char) -> Boolean): Int {
    for (index in fromIndex until length) {
        if (predicate(this[index])) return index
    }
    return -1
}

private data class InlineToken(
    val start: Int,
    val endExclusive: Int,
    val label: String,
    val type: MarkdownSpanType,
    val linkUrl: String? = null,
    val parseLabel: Boolean = true,
)
