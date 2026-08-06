package com.ryntra.shared.model

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser as JetBrainsMarkdownParser
import org.intellij.markdown.parser.CancellationToken

internal object GfmMarkdownParser {
    private val parser = JetBrainsMarkdownParser(
        GFMFlavourDescriptor(),
        false,
        object : CancellationToken {
            override fun checkCancelled() = Unit
        },
    )

    fun parse(markdown: String): List<MarkdownBlock> {
        if (markdown.isBlank()) return emptyList()
        val root = parser.buildMarkdownTreeFromString(markdown as CharSequence)
        return root.children.flatMap { node -> node.toNativeBlocks(markdown) }
    }

    private fun ASTNode.toNativeBlocks(source: String): List<MarkdownBlock> {
        val raw = source.substring(startOffset, endOffset).trimEnd()
        if (raw.isBlank()) return emptyList()
        return when (type) {
            GFMElementTypes.TABLE -> listOf(
                MarkdownBlock(
                    content = normalizeTable(raw),
                    type = MarkdownBlockType.Table,
                    table = parseTable(raw),
                )
            )
            MarkdownElementTypes.PARAGRAPH -> {
                val normalized = if ("![" in raw || "  \n" in raw) raw else raw.lines().joinToString(" ") { it.trim() }
                MarkdownParser.parseLegacy(normalized).mergeAdjacentImages()
            }
            else -> MarkdownParser.parseLegacy(raw)
        }
    }

    private fun normalizeTable(raw: String): String {
        val rows = raw.lines()
            .map { line ->
                line.trim().trim('|').split('|').joinToString(" | ") { cell -> cell.trim() }
            }
            .filterNot { row -> row.split('|').all { cell -> cell.trim().all { it == '-' || it == ':' || it.isWhitespace() } } }
        return rows.joinToString("\n")
    }

    private fun parseTable(raw: String): MarkdownTable? {
        val lines = raw.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return null
        val headers = splitTableRow(lines.first())
        val delimiter = splitTableRow(lines[1])
        if (headers.isEmpty() || delimiter.size != headers.size || delimiter.any { cell ->
                cell.trim(':', ' ').length < 3 || cell.trim(':', ' ').any { it != '-' }
            }
        ) return null
        val alignments = delimiter.map { cell ->
            val value = cell.trim()
            when {
                value.startsWith(':') && value.endsWith(':') -> MarkdownTableAlignment.Center
                value.endsWith(':') -> MarkdownTableAlignment.End
                else -> MarkdownTableAlignment.Start
            }
        }
        return MarkdownTable(
            headers = headers,
            rows = lines.drop(2).map { MarkdownTableRow(splitTableRow(it).padTo(headers.size)) },
            alignments = alignments,
        )
    }

    private fun splitTableRow(line: String): List<String> = line
        .trim()
        .trim('|')
        .split(Regex("(?<!\\\\)\\|"))
        .map { it.trim().replace("\\|", "|") }

    private fun List<String>.padTo(size: Int): List<String> =
        take(size) + List((size - this.size).coerceAtLeast(0)) { "" }

    private fun List<MarkdownBlock>.mergeAdjacentImages(): List<MarkdownBlock> {
        val merged = mutableListOf<MarkdownBlock>()
        for (block in this) {
            val previous = merged.lastOrNull()
            if (block.type == MarkdownBlockType.Image && previous?.type == MarkdownBlockType.Image) {
                merged[merged.lastIndex] = previous.copy(images = previous.images + block.images)
            } else {
                merged += block
            }
        }
        return merged
    }
}
