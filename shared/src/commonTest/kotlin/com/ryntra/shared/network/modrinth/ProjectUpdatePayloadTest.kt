package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ProjectUpdate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProjectUpdatePayloadTest {
    @Test
    fun changingToSpdxLicenseExplicitlyClearsCustomLicenseUrl() {
        val payload = encodeProjectUpdatePayload(ProjectUpdate(licenseId = "MIT"))

        assertEquals(JsonPrimitive("MIT"), payload["license_id"])
        assertEquals(JsonNull, payload["license_url"])
    }

    @Test
    fun unrelatedUpdateDoesNotTouchLicenseUrl() {
        val payload = encodeProjectUpdatePayload(ProjectUpdate(title = "New title"))

        assertFalse("license_url" in payload)
    }

    @Test
    fun customLicenseUrlIsEncoded() {
        val payload = encodeProjectUpdatePayload(
            ProjectUpdate(
                licenseId = "LicenseRef-Custom",
                licenseUrl = "https://example.com/license",
            ),
        )

        assertEquals(JsonPrimitive("https://example.com/license"), payload["license_url"])
    }
}
