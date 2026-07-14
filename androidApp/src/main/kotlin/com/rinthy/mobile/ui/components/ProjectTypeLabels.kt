package com.rinthy.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rinthy.mobile.R
import com.rinthy.shared.model.Project
import com.rinthy.shared.model.ProjectDisplayKind

@Composable
fun Project.displayTypeLabel(): String = displayKind().label()

@Composable
fun ProjectDisplayKind.label(): String = stringResource(
    when (this) {
        ProjectDisplayKind.Mod -> R.string.project_type_mod
        ProjectDisplayKind.Plugin -> R.string.project_type_plugin
        ProjectDisplayKind.Hybrid -> R.string.project_type_hybrid
        ProjectDisplayKind.Modpack -> R.string.project_type_modpack
        ProjectDisplayKind.ResourcePack -> R.string.project_type_resourcepack
        ProjectDisplayKind.Shader -> R.string.project_type_shader
        ProjectDisplayKind.DataPack -> R.string.project_type_datapack
        ProjectDisplayKind.Server -> R.string.project_type_server
        ProjectDisplayKind.Project -> R.string.project_type_project
    },
)
