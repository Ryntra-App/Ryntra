package com.ryntra.shared.network

import com.ryntra.shared.network.modrinth.OrganizationProjectDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrgProjectsLiveDecodeTest {
    @Test
    fun decodesRealCaffeineMcV3Payload() {
        val json = """
        [{
          "id": "AANobbMI",
          "slug": "sodium",
          "project_types": ["mod"],
          "games": ["minecraft-java"],
          "team_id": "4reLOHKp",
          "organization": "LjcZDkRW",
          "name": "Sodium",
          "summary": "Modern rendering engine",
          "description": "long body",
          "published": "2021-01-17T21:23:01.030011Z",
          "updated": "2021-07-15T22:33:20.513972Z",
          "approved": "2021-01-17T21:23:01.030011Z",
          "queued": null,
          "status": "approved",
          "requested_status": null,
          "moderator_message": null,
          "license": {
            "id": "LicenseRef-Polyform-Shield-1.0.0",
            "name": "",
            "url": "https://example.com"
          },
          "downloads": 100,
          "followers": 10,
          "categories": ["optimization"],
          "additional_categories": [],
          "loaders": ["fabric"],
          "versions": ["a","b"],
          "icon_url": "https://cdn.example/icon.png",
          "gallery": [{
            "url": "https://cdn.example/g.webp",
            "raw_url": "https://cdn.example/g.webp",
            "featured": false,
            "name": "Underwater Lighting Improvements",
            "description": "desc",
            "created": "2023-06-25T21:35:39.891789Z",
            "ordering": 0
          }],
          "color": 123,
          "thread_id": "abc",
          "monetization_status": "monetized",
          "environment": ["client_only"],
          "side_types_migration_review_status": "done",
          "link_urls": {},
          "game_versions": ["1.20"]
        }]
        """.trimIndent()

        val decoded = apiJson.decodeFromString<List<OrganizationProjectDto>>(json)
        assertEquals(1, decoded.size)
        val project = decoded.single().toProject()
        assertEquals("Sodium", project.title)
        assertEquals("Modern rendering engine", project.description)
        assertEquals("mod", project.projectType)
        assertEquals(1, project.gallery.size)
        assertEquals("https://cdn.example/g.webp", project.gallery.single().url)
        assertTrue(project.license != null)
    }

}
