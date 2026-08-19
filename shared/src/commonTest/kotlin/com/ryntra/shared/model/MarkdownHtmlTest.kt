package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Modrinth renders descriptions with `markdown-it` in `html: true` mode, so creators embed HTML
 * freely. These cases come from real project pages that used to show the tags as text.
 */
class MarkdownHtmlTest {
    @Test
    fun htmlImagesBecomeImageBlocks() {
        val blocks = MarkdownParser.parse(
            """<img src="https://imgur.com/MyjMs5T.png" alt="Fresh Animations" width="600"/>""",
        )

        assertEquals(1, blocks.size)
        assertEquals(MarkdownBlockType.Image, blocks.first().type)
        assertEquals("https://imgur.com/MyjMs5T.png", blocks.first().images.single().url)
        assertEquals("Fresh Animations", blocks.first().images.single().alt)
    }

    @Test
    fun aBadgeRowOfLinkedHtmlImagesKeepsEveryLink() {
        val source = """
            [<img src="https://imgur.com/gwrUbOG.png" alt="CurseForge" width="50"/>](https://curseforge.com/x)
            [<img src="https://imgur.com/Wi0gG3J.png" alt="Modrinth" width="50"/>](https://modrinth.com/y)
        """.trimIndent()

        val images = MarkdownParser.parse(source).flatMap { it.images }

        assertEquals(2, images.size)
        assertEquals("https://curseforge.com/x", images.first().linkUrl)
        assertEquals("https://modrinth.com/y", images.last().linkUrl)
    }

    @Test
    fun layoutWrappersNeverReachTheReader() {
        val source = """
            <center>

            Fresh Animations is a work in progress resource pack.

            </center>
        """.trimIndent()

        val rendered = MarkdownParser.parse(source).joinToString("\n") { it.content }

        assertFalse("center" in rendered)
        assertTrue("work in progress" in rendered)
    }

    @Test
    fun aCollapsibleSummaryBecomesAHeadingAndItsBodyStaysVisible() {
        val source = """
            <details>
              <summary>Animated mobs Graphic</summary>
            Bat, Snow Golem, Squid
            </details>
        """.trimIndent()

        val blocks = MarkdownParser.parse(source)
        val heading = blocks.first { it.type == MarkdownBlockType.Heading }

        assertEquals("Animated mobs Graphic", heading.content)
        assertTrue(blocks.any { "Snow Golem" in it.content })
        assertFalse(blocks.any { "details" in it.content })
    }

    @Test
    fun inlineHtmlEmphasisSurvivesAsSpans() {
        val blocks = MarkdownParser.parse("This is <b>bold</b> and <i>italic</i> text.")

        val types = blocks.single().spans.map { it.type }
        assertTrue(MarkdownSpanType.Bold in types)
        assertTrue(MarkdownSpanType.Italic in types)
        assertEquals("This is bold and italic text.", blocks.single().content)
    }

    @Test
    fun htmlHeadingsBecomeHeadings() {
        val blocks = MarkdownParser.parse("<h2>Requirements</h2>")

        assertEquals(MarkdownBlockType.Heading, blocks.single().type)
        assertEquals(2, blocks.single().level)
        assertEquals("Requirements", blocks.single().content)
    }

    @Test
    fun lineBreakTagsSplitTheLine() {
        val blocks = MarkdownParser.parse("First line<br/>Second line")

        assertEquals(listOf("First line", "Second line"), blocks.map { it.content })
    }

    @Test
    fun entitiesAreDecodedOnce() {
        val blocks = MarkdownParser.parse("Tom &amp; Jerry &lt;3 &amp;amp;")

        assertEquals("Tom & Jerry <3 &amp;", blocks.single().content)
    }

    @Test
    fun aStrayHardBreakBackslashIsNotShown() {
        val source = """
            ---
            \
            Fresh Animations is a resource pack.
        """.trimIndent()

        val rendered = MarkdownParser.parse(source).joinToString("\n") { it.content }

        assertFalse("\\" in rendered)
        assertTrue("resource pack" in rendered)
    }

    @Test
    fun htmlInsideFencedCodeIsLeftAlone() {
        val source = """
            ```html
            <img src="example.png" alt="kept"/>
            ```
        """.trimIndent()

        val code = MarkdownParser.parse(source).single { it.type == MarkdownBlockType.CodeBlock }

        assertTrue("<img" in code.content)
    }

    @Test
    fun htmlInsideInlineCodeIsLeftAlone() {
        val blocks = MarkdownParser.parse("Use `<br>` to break a line.")

        assertTrue("<br>" in blocks.single().content)
    }

    @Test
    fun anImageWithoutASourceIsDroppedRatherThanRenderedBroken() {
        val blocks = MarkdownParser.parse("""Before <img alt="no source"/> after""")

        assertTrue(blocks.none { it.type == MarkdownBlockType.Image })
        assertFalse(blocks.any { "img" in it.content })
    }

    @Test
    fun aRealModrinthDescriptionLeavesNoTagsBehind() {
        val source = """
            <center>

            Early access available ↓
            [<img src="https://imgur.com/MK6bBx8.png" alt="Early Access" width="600"/>](https://ko-fi.com/freshlx)

            ---
            \
            <img src="https://imgur.com/MyjMs5T.png" alt="88% complete" width="600"/>

            Fresh Animations is a work in progress resource pack.

            </center>

            <details>
              <summary>Animated mobs Graphic</summary>
            <img src="https://imgur.com/mFs6Rs4.png" alt="Mob Graphic"/>
            </details>
        """.trimIndent()

        val blocks = MarkdownParser.parse(source)
        val text = blocks.joinToString("\n") { it.content }

        assertFalse(Regex("</?[a-zA-Z]").containsMatchIn(text), "raw tags survived: $text")
        assertEquals(3, blocks.flatMap { it.images }.size)
        assertTrue(blocks.any { "work in progress" in it.content })
    }
}
