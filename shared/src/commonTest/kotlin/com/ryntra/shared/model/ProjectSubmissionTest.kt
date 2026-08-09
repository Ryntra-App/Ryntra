package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectSubmissionTest {
    private val completeDraft = Project(
        id = "project-1",
        title = "Ryntra Tools",
        description = "Creator tools with a clear short summary",
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
    fun hardBlockersAreReportedSeparatelyFromQualityWarnings() {
        val readiness = completeDraft.copy(
            description = "Short",
            body = "",
            iconUrl = null,
            license = null,
        ).submissionReadiness(versionCount = 0)

        assertFalse(readiness.canSubmit)
        assertEquals(
            listOf(
                ProjectSubmissionRequirement.Version,
                ProjectSubmissionRequirement.Description,
                ProjectSubmissionRequirement.License,
            ),
            readiness.blockingRequirements,
        )
        assertEquals(
            listOf(ProjectSubmissionRequirement.Icon, ProjectSubmissionRequirement.Summary),
            readiness.warningRequirements,
        )
        assertEquals(readiness.blockingRequirements, readiness.missingRequirements)
    }

    @Test
    fun iconAndShortSummaryDoNotBlockSubmission() {
        val readiness = completeDraft.copy(description = "Short", iconUrl = null)
            .submissionReadiness(versionCount = 1)

        assertTrue(readiness.canSubmit)
        assertEquals(listOf("icon", "summary"), readiness.warningRequirementKeys)
    }

    @Test
    fun invalidAndIncompleteCustomLicensesBlockSubmission() {
        val sentinel = completeDraft.copy(license = ProjectLicense("NOASSERTION"))
            .submissionReadiness(versionCount = 1)
        val customWithoutUrl = completeDraft.copy(license = ProjectLicense("LicenseRef-My-License"))
            .submissionReadiness(versionCount = 1)
        val customWithUrl = completeDraft.copy(
            license = ProjectLicense("LicenseRef-My-License", url = "https://example.com/license"),
        ).submissionReadiness(versionCount = 1)

        assertEquals(listOf(ProjectSubmissionRequirement.License), sentinel.blockingRequirements)
        assertEquals(listOf(ProjectSubmissionRequirement.LicenseUrl), customWithoutUrl.blockingRequirements)
        assertTrue(customWithUrl.canSubmit)
        assertEquals(listOf("license_url"), customWithoutUrl.missingRequirementKeys)
        assertTrue("LicenseRef-My-License".isCustomLicenseReference())
        assertFalse("LicenseRef-Unknown".isCustomLicenseReference())
        assertFalse("MIT".isCustomLicenseReference())
    }

    @Test
    fun serverDoesNotRequireVersion() {
        val readiness = completeDraft.copy(projectType = "minecraft_java_server")
            .submissionReadiness(versionCount = 0)

        assertTrue(readiness.canSubmit)
    }

    @Test
    fun galleryRulesFollowVisualProjectType() {
        val shader = completeDraft.copy(projectType = "shader")
        val resourcePack = completeDraft.copy(projectType = "resourcepack")
        val audioPack = resourcePack.copy(categories = listOf("audio"))
        val images = List(3) { index -> GalleryImage(url = "https://cdn.example/$index.png") }

        assertEquals(
            listOf(ProjectSubmissionRequirement.Gallery),
            shader.submissionReadiness(versionCount = 1).blockingRequirements,
        )
        assertTrue(shader.copy(gallery = images).submissionReadiness(versionCount = 1).canSubmit)
        assertFalse(resourcePack.submissionReadiness(versionCount = 1).canSubmit)
        assertTrue(audioPack.submissionReadiness(versionCount = 1).canSubmit)
    }

    @Test
    fun approvedProjectCannotBeSubmittedAgain() {
        assertFalse(completeDraft.copy(status = "approved").canEnterModeration())
    }
}
