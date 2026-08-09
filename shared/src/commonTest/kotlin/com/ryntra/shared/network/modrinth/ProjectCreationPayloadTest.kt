package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.CreateProjectRequest
import kotlin.test.Test
import kotlin.test.assertContains

class ProjectCreationPayloadTest {
    @Test
    fun emptyInitialVersionsAreIncludedBecauseModrinthRequiresTheField() {
        val encoded = encodeCreateProjectPayload(
            CreateProjectRequest(
                slug = "ryntra-tools",
                title = "Ryntra Tools",
                description = "Creator tools",
                categories = listOf("utility"),
                clientSide = "required",
                serverSide = "optional",
                body = "# About",
                licenseId = "MIT",
                projectType = "mod",
            ),
        )

        assertContains(encoded, "\"initial_versions\":[]")
        assertContains(encoded, "\"is_draft\":true")
    }
}
