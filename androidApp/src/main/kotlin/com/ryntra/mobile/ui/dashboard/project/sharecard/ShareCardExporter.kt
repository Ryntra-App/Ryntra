package com.ryntra.mobile.ui.dashboard.project.sharecard

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun Context.createShareCardUri(
    bitmap: Bitmap,
    projectSlug: String,
): Uri = withContext(Dispatchers.IO) {
    val directory = File(cacheDir, "shared-images")
    check(directory.isDirectory || directory.mkdirs()) { "Could not prepare the share directory." }
    val safeSlug = projectSlug
        .lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "-")
        .trim('-')
        .ifBlank { "project" }
        .take(48)
    directory.listFiles { file ->
        file.name == "$safeSlug-share-card.png" || file.name.startsWith("$safeSlug-share-card-")
    }
        ?.forEach(File::delete)
    val output = File(directory, "$safeSlug-share-card-${System.currentTimeMillis()}.png")
    FileOutputStream(output, false).use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            "Could not encode the share card."
        }
        stream.fd.sync()
    }
    check(output.length() > 8) { "The exported share card is empty." }
    FileProvider.getUriForFile(
        this@createShareCardUri,
        "$packageName.fileprovider",
        output,
    )
}

internal fun Context.openShareCardChooser(
    uri: Uri,
    chooserTitle: String,
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("Ryntra share card", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, chooserTitle))
}
