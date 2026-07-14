package com.rinthy.shared.app

import com.rinthy.shared.model.ProjectFileUpload
import com.rinthy.shared.model.ProjectUploadLimits
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AppControllerUploadValidationTest {
    @Test
    fun galleryImageLargerThanApiLimitIsRejectedBeforeAuthentication() = runTest {
        val upload = ProjectFileUpload(
            fileName = "large.png",
            contentType = "image/png",
            bytes = ByteArray(ProjectUploadLimits.GALLERY_IMAGE_BYTES + 1),
        )

        assertFailsWith<IllegalArgumentException> {
            AppController().addGalleryImage("project", upload)
        }
    }

    @Test
    fun galleryUploadMustHaveImageContentType() = runTest {
        val upload = ProjectFileUpload(
            fileName = "notes.txt",
            contentType = "text/plain",
            bytes = byteArrayOf(1),
        )

        assertFailsWith<IllegalArgumentException> {
            AppController().addGalleryImage("project", upload)
        }
    }
}
