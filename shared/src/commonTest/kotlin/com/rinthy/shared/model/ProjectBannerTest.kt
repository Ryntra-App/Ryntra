package com.rinthy.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectBannerTest {
    @Test
    fun bannerUsesFeaturedGalleryImageBeforeOrdering() {
        val project = projectWithGallery(
            GalleryImage(url = "ordered-first", ordering = 0),
            GalleryImage(url = "featured", featured = true, ordering = 5),
        )

        assertEquals("featured", project.bannerUrl)
    }

    @Test
    fun bannerFallsBackToFirstOrderedGalleryImage() {
        val project = projectWithGallery(
            GalleryImage(url = "second", ordering = 2),
            GalleryImage(url = "first", ordering = 1),
        )

        assertEquals("first", project.bannerUrl)
    }

    private fun projectWithGallery(vararg images: GalleryImage) = Project(
        id = "project-id",
        title = "Project",
        gallery = images.toList(),
    )
}
