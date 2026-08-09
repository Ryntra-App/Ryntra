package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectStatusPolicyTest {
    @Test
    fun approvedFamilyUpdatesCurrentStatusDirectly() {
        assertEquals(
            ProjectVisibilityUpdate(status = "unlisted"),
            ProjectStatusPolicy.updateFor("approved", null, "unlisted"),
        )
    }

    @Test
    fun nonApprovedProjectUpdatesRequestedStatus() {
        assertEquals(
            ProjectVisibilityUpdate(requestedStatus = "private"),
            ProjectStatusPolicy.updateFor("draft", null, "private"),
        )
    }

    @Test
    fun processingCannotBeSelectedInEditor() {
        assertNull(ProjectStatusPolicy.updateFor("draft", null, "processing"))
    }

    @Test
    fun unchangedVisibilityProducesNoUpdate() {
        assertNull(ProjectStatusPolicy.updateFor("approved", null, "approved"))
        assertNull(ProjectStatusPolicy.updateFor("processing", "approved", "approved"))
    }
}
