package com.ryntra.shared.network

import com.ryntra.shared.network.modrinth.OrganizationProjectDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrgProjectsFixtureDecodeTest {
    @Test
    fun decodesFullLiveCaffeineMcFile() {
        val stream = this::class.java.classLoader!!.getResourceAsStream("caffeinemc_org_projects_v3.json")
            ?: error("missing test resource caffeinemc_org_projects_v3.json")
        val json = stream.bufferedReader().readText()
        val decoded = apiJson.decodeFromString<List<OrganizationProjectDto>>(json)
        assertEquals(4, decoded.size)
        val titles = decoded.map { it.toProject().title }.toSet()
        assertTrue("Sodium" in titles)
        assertTrue("Lithium" in titles)
        assertTrue(decoded.all { it.toProject().title.isNotBlank() })
        assertTrue(decoded.any { it.toProject().gallery.isNotEmpty() })
    }
}
