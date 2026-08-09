package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectSubmissionTest {
    private val completeDraft = Project(
        id = "project-1",
        title = "Ryntra Tools",
        description = "Creator tools",
        body = "# About",
        iconUrl = "https://cdn.example/icon.png",
        license = ProjectLicense("MIT"),
        status = "draft",
    )

    @Test
    fun completeDraftWithVersionCanSubmit() {
        assertTrue(completeDraft.submissionReadiness(versionCount = 1).canSubmit)
        assertTrue(completeDraft.canEnterModeration())
    }

    @Test
    fun missingRequirementsAreReportedTogether() {
        val readiness = completeDraft.copy(
            description = "",
            body = "",
            iconUrl = null,
            license = null,
        ).submissionReadiness(versionCount = 0)

        assertFalse(readiness.canSubmit)
        assertEquals(ProjectSubmissionRequirement.entries, readiness.missingRequirements)
    }

    @Test
    fun approvedProjectCannotBeSubmittedAgain() {
        assertFalse(completeDraft.copy(status = "approved").canEnterModeration())
    }
}
