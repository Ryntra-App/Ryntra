package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Modrinth's sanitizer accepts `<iframe>` only from YouTube and the Discord widget, so those are
 * the two the app turns into cards. Everything else is dropped exactly as the site drops it.
 */
class MarkdownEmbedTest {
    /** Private-use boundary MarkdownHtml leaves around an extracted embed. */
    private val PLACEHOLDER = ''

    @Test
    fun aYouTubeIframeBecomesAnEmbedBlock() {
        val blocks = MarkdownParser.parse(
            """<iframe width="560" height="315" src="https://www.youtube.com/embed/dQw4w9WgXcQ"></iframe>""",
        )

        val embed = blocks.single { it.type == MarkdownBlockType.Embed }.embed!!
        assertEquals(MarkdownEmbedProvider.YouTube, embed.provider)
        assertEquals("dQw4w9WgXcQ", embed.embedId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", embed.url)
        assertEquals("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg", embed.thumbnailUrl)
    }

    @Test
    fun theNoCookieDomainIsRecognisedToo() {
        val embed = MarkdownParser.parse(
            """<iframe src="https://www.youtube-nocookie.com/embed/abcdefghijk?start=30"></iframe>""",
        ).single { it.type == MarkdownBlockType.Embed }.embed!!

        assertEquals("abcdefghijk", embed.embedId)
    }

    @Test
    fun aDiscordWidgetBecomesAnEmbedBlock() {
        val embed = MarkdownParser.parse(
            """<iframe src="https://discord.com/widget?id=123456789012345678&theme=dark"></iframe>""",
        ).single { it.type == MarkdownBlockType.Embed }.embed!!

        assertEquals(MarkdownEmbedProvider.Discord, embed.provider)
        assertEquals("123456789012345678", embed.embedId)
        assertNull(embed.thumbnailUrl)
    }

    @Test
    fun escapedAmpersandsInTheSourceStillParse() {
        val embed = MarkdownParser.parse(
            """<iframe src="https://discord.com/widget?theme=dark&amp;id=123456789012345678"></iframe>""",
        ).single { it.type == MarkdownBlockType.Embed }.embed!!

        assertEquals("123456789012345678", embed.embedId)
    }

    @Test
    fun anIframeFromAnywhereElseIsDroppedWithoutATrace() {
        val blocks = MarkdownParser.parse(
            """Before<iframe src="https://evil.example.com/tracker"></iframe>after""",
        )

        assertTrue(blocks.none { it.type == MarkdownBlockType.Embed })
        assertFalse(blocks.any { "iframe" in it.content || "evil" in it.content })
    }

    @Test
    fun anEmbedInsideATextParagraphSplitsOutAsItsOwnBlock() {
        val source = """
            Watch the trailer:
            <iframe src="https://www.youtube.com/embed/dQw4w9WgXcQ"></iframe>
            Then install it.
        """.trimIndent()

        val blocks = MarkdownParser.parse(source)

        assertEquals(1, blocks.count { it.type == MarkdownBlockType.Embed })
        assertTrue(blocks.any { "Watch the trailer" in it.content })
        assertTrue(blocks.any { "Then install it" in it.content })
        assertFalse(blocks.any { PLACEHOLDER in it.content })
    }

    @Test
    fun severalEmbedsKeepTheirOwnIdentities() {
        val source = """
            <iframe src="https://www.youtube.com/embed/aaaaaaaaaaa"></iframe>
            <iframe src="https://www.youtube.com/embed/bbbbbbbbbbb"></iframe>
        """.trimIndent()

        val ids = MarkdownParser.parse(source)
            .filter { it.type == MarkdownBlockType.Embed }
            .map { it.embed!!.embedId }

        assertEquals(listOf("aaaaaaaaaaa", "bbbbbbbbbbb"), ids)
    }

    @Test
    fun anIframeInsideFencedCodeStaysAsText() {
        val source = """
            ```html
            <iframe src="https://www.youtube.com/embed/dQw4w9WgXcQ"></iframe>
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parse(source)

        assertTrue(blocks.none { it.type == MarkdownBlockType.Embed })
        assertTrue(blocks.single { it.type == MarkdownBlockType.CodeBlock }.content.contains("<iframe"))
    }

    @Test
    fun aShortenedYouTubeIdIsNotMistakenForAVideo() {
        val blocks = MarkdownParser.parse("""<iframe src="https://www.youtube.com/embed/short"></iframe>""")

        assertTrue(blocks.none { it.type == MarkdownBlockType.Embed })
    }

    /**
     * Verbatim markup from Create, Complementary Reimagined and Cobblemon. Real embeds put `src`
     * after other attributes and wrap the tag in `<p>` or `<center>`, which a stricter pattern
     * would miss.
     */
    @Test
    fun realProjectPagesParse() {
        val sources = listOf(
            """<p><iframe allowfullscreen="allowfullscreen" src="https://www.youtube.com/embed/rR8W-f9YhYA" height="358" width="638"></iframe></p>""",
            """<iframe width="853" height="480" src="https://www.youtube.com/embed/q897tXWMccM" title="Reimagine Minecraft | Complementary Reimagined Reveal Trailer" frameborder="0" allow="accelerometer; autoplay" allowfullscreen></iframe>""",
            """<center><iframe width="560" height="315" src="https://www.youtube.com/embed/2tsCcWbiYgA?si=zAfUCC1MLsPih05i" title="YouTube video player" frameborder="0" allowfullscreen></iframe></center>""",
        )

        val ids = sources.map { source ->
            val blocks = MarkdownParser.parse(source)
            assertFalse(blocks.any { "iframe" in it.content }, "raw tag survived in: $source")
            blocks.single { it.type == MarkdownBlockType.Embed }.embed!!.embedId
        }

        assertEquals(listOf("rR8W-f9YhYA", "q897tXWMccM", "2tsCcWbiYgA"), ids)
    }
}
