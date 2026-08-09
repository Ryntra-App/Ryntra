package com.ryntra.shared.model

private const val MODRINTH_WEB_ORIGIN = "https://modrinth.com"

/** Canonical public Modrinth path for this project's concrete content type. */
fun Project.modrinthPath(): String {
    val identifier = slug?.trim()?.takeIf(String::isNotEmpty) ?: id
    val encodedIdentifier = identifier.encodeModrinthPathSegment() ?: id
    return "/${modrinthRoute()}/$encodedIdentifier"
}

fun Project.modrinthUrl(): String = MODRINTH_WEB_ORIGIN + modrinthPath()

private fun Project.modrinthRoute(): String = when (displayKind()) {
    ProjectDisplayKind.Mod -> "mod"
    ProjectDisplayKind.Plugin -> "plugin"
    ProjectDisplayKind.Hybrid -> "mod"
    ProjectDisplayKind.Modpack -> "modpack"
    ProjectDisplayKind.ResourcePack -> "resourcepack"
    ProjectDisplayKind.Shader -> "shader"
    ProjectDisplayKind.DataPack -> "datapack"
    ProjectDisplayKind.Server -> "server"
    ProjectDisplayKind.Project -> "project"
}

private fun String.encodeModrinthPathSegment(): String? {
    // Modrinth slugs are ASCII. Use the stable project ID if an unexpected
    // Unicode slug reaches this model instead of corrupting a UTF-8 path.
    if (any { it.code > 0x7f }) return null
    return buildString {
        this@encodeModrinthPathSegment.forEach { character ->
            if (character.isLetterOrDigit() || character in "-._~") {
                append(character)
            } else {
                append('%')
                append(HEX_DIGITS[character.code shr 4])
                append(HEX_DIGITS[character.code and 0x0f])
            }
        }
    }
}

private const val HEX_DIGITS = "0123456789ABCDEF"
