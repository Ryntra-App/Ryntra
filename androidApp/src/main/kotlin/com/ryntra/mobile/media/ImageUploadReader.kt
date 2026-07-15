package com.ryntra.mobile.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.ryntra.shared.model.ProjectFileUpload
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Reads a user-picked image from content:// into a [ProjectFileUpload].
 * Enforces [maxBytes] while streaming so oversized files are rejected early.
 */
object ImageUploadReader {
    fun read(
        context: Context,
        uri: Uri,
        fallbackName: String,
        maxBytes: Int,
    ): ProjectFileUpload? {
        val contentType = context.contentResolver.getType(uri) ?: "image/png"
        if (!contentType.startsWith("image/")) return null
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytesLimited(maxBytes)
        } ?: return null
        var fileName = fallbackName
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)?.takeIf { it.isNotBlank() }?.let { fileName = it }
            }
        }
        return ProjectFileUpload(fileName = fileName, contentType = contentType, bytes = bytes)
    }
}

private fun InputStream.readBytesLimited(maxBytes: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count <= 0) break
        total += count
        if (total > maxBytes) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
