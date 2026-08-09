package com.ryntra.mobile.ui.dashboard.project.sharecard

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.PixelCopy
import android.view.Window
import androidx.compose.ui.unit.IntRect
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun Context.createShareCardUri(
    bitmap: Bitmap,
    projectSlug: String,
): Uri = withContext(Dispatchers.IO) {
    val directory = File(cacheDir, "shared-images").apply { mkdirs() }
    val safeSlug = projectSlug
        .lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "-")
        .trim('-')
        .ifBlank { "project" }
        .take(48)
    val output = File(directory, "$safeSlug-share-card.png")
    FileOutputStream(output, false).use { stream ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
            "Could not encode the share card."
        }
    }
    FileProvider.getUriForFile(
        this@createShareCardUri,
        "$packageName.fileprovider",
        output,
    )
}

internal suspend fun View.captureRegion(
    bounds: IntRect,
    window: Window,
): Bitmap = suspendCancellableCoroutine { continuation ->
    check(width > 0 && height > 0) { "The share card is not laid out yet." }
    val left = bounds.left.coerceIn(0, width - 1)
    val top = bounds.top.coerceIn(0, height - 1)
    val regionWidth = bounds.width.coerceAtMost(width - left).coerceAtLeast(1)
    val regionHeight = bounds.height.coerceAtMost(height - top).coerceAtLeast(1)
    val location = IntArray(2)
    getLocationInWindow(location)
    val source = Rect(
        location[0] + left,
        location[1] + top,
        location[0] + left + regionWidth,
        location[1] + top + regionHeight,
    )
    val bitmap = Bitmap.createBitmap(regionWidth, regionHeight, Bitmap.Config.ARGB_8888)
    PixelCopy.request(
        window,
        source,
        bitmap,
        { result ->
            if (!continuation.isActive) {
                bitmap.recycle()
            } else if (result == PixelCopy.SUCCESS) {
                continuation.resume(bitmap)
            } else {
                bitmap.recycle()
                continuation.resumeWithException(IllegalStateException("PixelCopy failed with code $result."))
            }
        },
        Handler(Looper.getMainLooper()),
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
