package com.ryntra.shared.network.modrinth

import com.ryntra.shared.model.ProjectFileUpload

internal fun ProjectFileUpload.imageExtension(): String {
    val fromName = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (fromName in imageExtensions) return fromName
    return when (contentType.lowercase()) {
        "image/jpeg" -> "jpg"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/svg+xml" -> "svg"
        else -> "png"
    }
}

internal fun ProjectFileUpload.safeFileName(): String =
    fileName.substringAfterLast('/').substringAfterLast('\\').replace('"', '_').ifBlank { "upload.bin" }

private val imageExtensions = setOf("png", "jpg", "jpeg", "bmp", "gif", "webp", "svg", "svgz", "rgb")
