package com.rinthy.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectAttentionStateTest {
    @Test
    fun processingIsInReviewButNotAttention() {
        val project = Project(
            id = "1",
            title = "AnyBind",
            status = "processing",
            requestedStatus = "approved",
            queued = "2026-07-13T10:00:00Z",
        )
        val state = project.attentionState()
        assertEquals(ProjectAttentionKind.ReviewForPublication, state.kind)
        assertTrue(state.isInReview)
        assertFalse(state.needsAttention)
        assertTrue(project.isInReview())
        assertFalse(project.needsAttention())
    }

    @Test
    fun processingWithModeratorNoteNeedsAttention() {
        val project = Project(
            id = "1",
            title = "X",
            status = "processing",
            moderatorMessage = ModeratorMessage(message = "Please fix the icon"),
        )
        val state = project.attentionState()
        // Moderator feedback escalates to Needs attention (not the quiet "In review" list).
        assertFalse(state.isInReview)
        assertTrue(state.needsAttention)
        assertEquals("Please fix the icon", state.moderatorNote)
    }

    @Test
    fun rejectedNeedsAttentionWithNote() {
        val project = Project(
            id = "1",
            title = "X",
            status = "rejected",
            moderatorMessage = ModeratorMessage(message = "Missing description"),
        )
        val state = project.attentionState()
        assertEquals(ProjectAttentionKind.Rejected, state.kind)
        assertEquals("Missing description", state.moderatorNote)
        assertTrue(state.needsAttention)
        assertFalse(state.isInReview)
    }

    @Test
    fun approvedArchivedDraftDoNotNeedAttention() {
        assertFalse(Project(id = "1", title = "A", status = "approved").needsAttention())
        assertFalse(Project(id = "2", title = "B", status = "archived").needsAttention())
        assertFalse(Project(id = "3", title = "D", status = "draft").needsAttention())
        assertFalse(Project(id = "4", title = "U", status = "unlisted").needsAttention())
    }
}
