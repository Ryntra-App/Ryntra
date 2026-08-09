package com.ryntra.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectLinksTest {
    @Test
    fun canonicalUrlUsesOfficialRouteForEachProjectType() {
        val cases = mapOf(
            "mod" to "mod",
            "modpack" to "modpack",
            "resourcepack" to "resourcepack",
            "shader" to "shader",
            "datapack" to "datapack",
            "minecraft_java_server" to "server",
        )

        cases.forEach { (projectType, route) ->
            val project = Project(id = "project-id", title = "Project", projectType = projectType)
            assertEquals("https://modrinth.com/$route/project-id", project.modrinthUrl())
        }
    }

    @Test
    fun loaderBasedPluginUsesPluginRoute() {
        val project = Project(
            id = "paper",
            title = "Paper",
            projectType = "mod",
            loaders = listOf("paper"),
        )

        assertEquals("/plugin/paper", project.modrinthPath())
    }

    @Test
    fun canonicalPathPrefersSlugAndFallsBackToId() {
        val withSlug = Project(id = "project-id", slug = "pretty-slug", title = "Project", projectType = "mod")
        val withoutSlug = withSlug.copy(slug = "  ")

        assertEquals("/mod/pretty-slug", withSlug.modrinthPath())
        assertEquals("/mod/project-id", withoutSlug.modrinthPath())
    }

    @Test
    fun canonicalPathEncodesSupportedSlugPunctuation() {
        val project = Project(id = "project-id", slug = "project\$name", title = "Project", projectType = "mod")

        assertEquals("/mod/project%24name", project.modrinthPath())
    }
}
