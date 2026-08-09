package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectCreationRulesTest {
    private fun validRequest() = CreateProjectRequest(
        slug = "ryntra-tools",
        title = "Ryntra Tools",
        description = "Creator tools",
        body = "# About",
        projectType = "mod",
        categories = listOf("utility"),
        licenseId = "MIT",
    )

    @Test
    fun rejectsFieldsShorterThanModrinthMinimums() {
        val errors = ProjectCreationRules.validate(
            validRequest().copy(title = "ab", description = "xy"),
        )

        assertTrue(errors.any { "Project name must be at least" in it })
        assertTrue(errors.any { "Summary must be at least" in it })
    }

    @Test
    fun rejectsTooManyPrimaryCategories() {
        val errors = ProjectCreationRules.validate(
            validRequest().copy(categories = listOf("one", "two", "three", "four")),
        )

        assertTrue(errors.any { "primary categories" in it })
    }

    @Test
    fun rejectsDescriptionBeyondModrinthLimit() {
        val errors = ProjectCreationRules.validate(
            validRequest().copy(body = "x".repeat(ProjectCreationRules.BODY_MAX_LENGTH + 1)),
        )

        assertTrue(errors.any { "Full description" in it })
    }

    @Test
    fun customLicenseRequiresPublicTermsUrl() {
        val missingUrl = ProjectCreationRules.validate(
            validRequest().copy(licenseId = "LicenseRef-Ryntra"),
        )
        val withUrl = ProjectCreationRules.validate(
            validRequest().copy(
                licenseId = "LicenseRef-Ryntra",
                licenseUrl = "https://example.com/license",
            ),
        )

        assertTrue(missingUrl.any { "custom license terms" in it })
        assertTrue(withUrl.none { "custom license terms" in it })
    }

    @Test
    fun rejectsInvalidLicenseSentinels() {
        listOf("LicenseRef-", "LicenseRef-Unknown", "NOASSERTION", "LicenseRef-NOASSERTION").forEach { id ->
            val errors = ProjectCreationRules.validate(validRequest().copy(licenseId = id))
            assertTrue(errors.any { "valid license" in it }, "Expected $id to be rejected")
        }
    }
}
