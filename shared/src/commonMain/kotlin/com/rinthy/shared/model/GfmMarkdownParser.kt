package com.rinthy.shared.model

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
