package com.rinthy.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectDisplayKindTest {
    @Test
    fun classifiesPluginLoadersAsPluginEvenWhenTypeIsMod() {
        assertEquals(
            ProjectDisplayKind.Plugin,
            ProjectDisplayKind.from(
                projectType = "mod",
                loaders = listOf("bukkit", "paper", "spigot"),
            ),
        )
    }

    @Test
    fun classifiesFabricLoadersAsMod() {
        assertEquals(
            ProjectDisplayKind.Mod,
            ProjectDisplayKind.from(
                projectType = "mod",
                loaders = listOf("fabric", "quilt"),
            ),
        )
    }

    @Test
    fun classifiesHybridProjectsWithBothModAndPluginLoaders() {
        assertEquals(
            ProjectDisplayKind.Hybrid,
            ProjectDisplayKind.from(
                projectType = "mod",
                loaders = listOf("fabric", "paper", "velocity"),
            ),
        )
    }

    @Test
    fun keepsDedicatedProjectTypes() {
        assertEquals(ProjectDisplayKind.ResourcePack, ProjectDisplayKind.from("resourcepack"))
        assertEquals(ProjectDisplayKind.Modpack, ProjectDisplayKind.from("modpack"))
        assertEquals(ProjectDisplayKind.Shader, ProjectDisplayKind.from("shader"))
        assertEquals(ProjectDisplayKind.DataPack, ProjectDisplayKind.from("datapack"))
        assertEquals(ProjectDisplayKind.Plugin, ProjectDisplayKind.from("plugin"))
        assertEquals(ProjectDisplayKind.Server, ProjectDisplayKind.from("minecraft_java_server"))
    }

    @Test
    fun projectModelUsesLoadersForDisplayKind() {
        val plugin = Project(
            id = "1",
            title = "Essentials",
            projectType = "mod",
            loaders = listOf("paper", "spigot"),
        )
        assertEquals(ProjectDisplayKind.Plugin, plugin.displayKind())
    }
}
