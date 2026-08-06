package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownParserTest {
    @Test
    fun standaloneImageBecomesImageBlock() {
        val block = MarkdownParser.parse("![github](https://example.com/github.png)").single()

        assertEquals(MarkdownBlockType.Image, block.type)
        assertEquals(
            MarkdownImage("github", "https://example.com/github.png", isBadge = true),
            block.images.single(),
        )
    }

    @Test
    fun githubImageAltStaysCompactForRasterBadges() {
        val image = MarkdownParser.parse("![github](https://example.com/github.png)").single().images.single()

        assertEquals(true, image.isBadge)
    }

    @Test
    fun descriptiveRasterAltDoesNotTurnScreenshotIntoBadge() {
        val image = MarkdownParser.parse(
            "![GitHub project screenshot](https://example.com/project.png)",
        ).single().images.single()

        assertEquals(false, image.isBadge)
    }

    @Test
    fun linkedBadgeRowPreservesImagesAndDestinations() {
        val markdown = "[![github](https://img.example/github.svg)](https://github.com/ryntra) " +
            "![downloads](https://img.example/downloads.svg)"

        val images = MarkdownParser.parse(markdown).single().images

        assertEquals(2, images.size)
        assertEquals("https://github.com/ryntra", images[0].linkUrl)
        assertEquals(true, images[0].isBadge)
        assertEquals("https://img.example/downloads.svg", images[1].url)
    }

    @Test
    fun consecutiveGfmBadgeLinesBecomeOneRow() {
        val markdown = listOf(
            "[![Github](https://img.example/github.png)](https://github.com/ryntra)",
            "[![Modrinth](https://img.example/modrinth.png)](https://modrinth.com/project/ryntra)",
            "[![Discord](https://img.example/discord.png)](https://discord.com/invite/ryntra)",
        ).joinToString("\n")

        val block = MarkdownParser.parse(markdown).single()

        assertEquals(MarkdownBlockType.Image, block.type)
        assertEquals(3, block.images.size)
    }

    @Test
    fun imageSharingLineWithTextIsNotDropped() {
        val blocks = MarkdownParser.parse("Logo: ![ryntra](https://example.com/logo.png)")

        assertEquals(2, blocks.size)
        assertEquals("Logo:", blocks[0].content)
        assertEquals(MarkdownBlockType.Image, blocks[1].type)
        assertEquals("https://example.com/logo.png", blocks[1].images.single().url)
    }

    @Test
    fun modrinthNestedImageDoesNotLeakTrailingLinkText() {
        val imageUrl = "https://cdn.modrinth.com/data/cached_images/info.png"
        val blocks = MarkdownParser.parse("![![Modrinth]($imageUrl)]($imageUrl)")

        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlockType.Image, blocks.single().type)
        assertEquals(imageUrl, blocks.single().images.single().url)
        assertEquals(imageUrl, blocks.single().images.single().linkUrl)
        assertEquals(false, blocks.single().images.single().isBadge)
    }

    @Test
    fun gfmTableBecomesNativeTableBlock() {
        val block = MarkdownParser.parse(
            """
                | Loader | Version |
                | --- | --- |
                | Fabric | 1.21.1 |
            """.trimIndent()
        ).single()

        assertEquals(MarkdownBlockType.Table, block.type)
        assertEquals("Loader | Version\nFabric | 1.21.1", block.content)
        assertEquals(listOf("Loader", "Version"), block.table?.headers)
        assertEquals(listOf("Fabric", "1.21.1"), block.table?.rows?.single()?.cells)
    }

    @Test
    fun gfmTablePreservesAlignmentAndEscapedPipes() {
        val block = MarkdownParser.parse(
            "| Name | Notes |\n| :--- | ---: |\n| Ryntra | iOS \\| Android |",
        ).single()

        assertEquals(listOf(MarkdownTableAlignment.Start, MarkdownTableAlignment.End), block.table?.alignments)
        assertEquals(listOf("Ryntra", "iOS | Android"), block.table?.rows?.single()?.cells)
    }

    @Test
    fun gfmTaskListPreservesCheckedState() {
        val blocks = MarkdownParser.parse("- [x] Released\n- [ ] Documented")

        assertEquals(listOf(true, false), blocks.map { it.checked })
        assertEquals(listOf("Released", "Documented"), blocks.map { it.content })
    }

    @Test
    fun gfmSoftLineBreakStaysInOneParagraph() {
        val block = MarkdownParser.parse("First line\ncontinues here").single()

        assertEquals("First line continues here", block.content)
    }

    @Test
    fun bareUrlBecomesClickableLink() {
        val block = MarkdownParser.parse("See https://modrinth.com/project/ryntra").single()

        assertEquals(MarkdownSpanType.Link, block.spans.single().type)
        assertEquals("https://modrinth.com/project/ryntra", block.spans.single().linkUrl)
    }

    @Test
    fun gfmUnderscoreEmphasisAndNestedListsArePreserved() {
        val blocks = MarkdownParser.parse("__Bold__ and _italic_\n\n- Parent\n  - Child")

        assertEquals(listOf(MarkdownSpanType.Bold, MarkdownSpanType.Italic), blocks.first().spans.map { it.type })
        assertEquals(listOf(0, 1), blocks.filter { it.type == MarkdownBlockType.ListItem }.map { it.level })
    }

    @Test
    fun shieldsBadgeWithUrlEmbeddedInAltIsRecovered() {
        val markdown =
            "![Fabric (https://img.shields.io/badge/Loader-Fabric-7BE0C3?style=for-the-badge)](https://fabricmc.net/)"
        val image = MarkdownParser.parse(markdown).single().images.single()

        assertEquals(
            "https://img.shields.io/badge/Loader-Fabric-7BE0C3?style=for-the-badge",
            image.url,
        )
        assertEquals("https://fabricmc.net/", image.linkUrl)
        assertEquals(true, image.isBadge)
    }

    @Test
    fun standardLinkedShieldsBadgeParses() {
        val markdown =
            "[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-A9B8FF?style=for-the-badge)](https://minecraft.net/)"
        val image = MarkdownParser.parse(markdown).single().images.single()

        assertEquals(
            "https://img.shields.io/badge/Minecraft-1.21.x-A9B8FF?style=for-the-badge",
            image.url,
        )
        assertEquals("https://minecraft.net/", image.linkUrl)
        assertEquals(true, image.isBadge)
    }

    @Test
    fun badgeRowOfThreeShieldsStaysOneImageBlock() {
        val markdown = listOf(
            "![Fabric (https://img.shields.io/badge/Loader-Fabric-7BE0C3?style=for-the-badge)](https://fabricmc.net/)",
            "![Minecraft (https://img.shields.io/badge/Minecraft-1.21.x-A9B8FF?style=for-the-badge)](https://minecraft.net/)",
            "![License (https://img.shields.io/badge/License-GPL--3.0-FFC36B?style=for-the-badge)](LICENSE.txt)",
        ).joinToString("\n")

        val blocks = MarkdownParser.parse(markdown)
        val images = blocks.flatMap { it.images }

        assertEquals(3, images.size)
        assertEquals(true, images.all { it.isBadge })
        assertEquals(true, images.all { "shields.io" in it.url })
    }
}
