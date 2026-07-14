package com.rinthy.mobile.ui.dashboard.project.versions

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectUploadLimits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal suspend fun readVersionUploads(
    context: Context,
    uris: List<Uri>,
    existingFiles: List<ProjectFileUpload>,
): Result<List<ProjectFileUpload>> = withContext(Dispatchers.IO) {
    try {
        val uploads = uris.map(context::readVersionUpload)
        val merged = (existingFiles + uploads).distinctBy(ProjectFileUpload::fileName)
        require(merged.sumOf { it.bytes.size.toLong() } <= ProjectUploadLimits.VERSION_FILES_BYTES) {
            "Version files must be 128 MiB or smaller in total."
        }
        Result.success(merged)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}

private fun Context.readVersionUpload(uri: Uri): ProjectFileUpload {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytesLimited(MAX_VERSION_BYTES) }
        ?: error("Unable to read the selected file.")
    var fileName = "version-file"
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) fileName = cursor.getString(0)
    }
    return ProjectFileUpload(fileName, contentResolver.getType(uri) ?: "application/octet-stream", bytes)
}

private fun InputStream.readBytesLimited(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "Version files must be 128 MiB or smaller in total." }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

private val MAX_VERSION_BYTES = ProjectUploadLimits.VERSION_FILES_BYTES.toInt()
