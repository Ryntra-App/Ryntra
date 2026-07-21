package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModrinthNotificationLinkTest {
    @Test
    fun parsesProjectAndVersionLinks() {
        val link = ModrinthNotificationLink.parse("https://modrinth.com/mod/sodium/version/mc1.21.5-0.6.13#files")

        assertEquals("sodium", link?.projectIdOrSlug)
        assertEquals("mc1.21.5-0.6.13", link?.versionId)
    }

    @Test
    fun parsesRelativeProjectLinks() {
        assertEquals("fabric-api", ModrinthNotificationLink.parse("/project/fabric-api?tab=versions")?.projectIdOrSlug)
        assertEquals("fabric-api", ModrinthNotificationLink.parse("ryntra://modrinth/project/fabric-api")?.projectIdOrSlug)
    }

    @Test
    fun rejectsNonProjectDestinations() {
        assertNull(ModrinthNotificationLink.parse("https://modrinth.com/dashboard/notifications"))
        assertNull(ModrinthNotificationLink.parse("https://modrinth.com/organization/example"))
    }

    @Test
    fun acceptsOnlySafeTeamJoinActions() {
        assertEquals(
            "team_123",
            ModrinthNotificationAction(actionRoute = listOf("POST", "team/team_123/join")).teamJoinId,
        )
        assertNull(ModrinthNotificationAction(actionRoute = listOf("DELETE", "team/team_123/join")).teamJoinId)
        assertNull(ModrinthNotificationAction(actionRoute = listOf("POST", "project/team_123/delete")).teamJoinId)
        assertNull(ModrinthNotificationAction(actionRoute = listOf("POST", "team/../join")).teamJoinId)
    }
}
