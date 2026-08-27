package com.nova.app.feature.posts.data

import org.junit.Assert.assertEquals
import org.junit.Test


class PostJsonParserMediaTest {
    @Test
    fun `video post parses truthful media and thumbnail fields`() {
        val post = normalizePostMedia(
            legacyImageUrl = "",
            mediaType = "video",
            mediaUrl = "/media/posts/video/clip.mp4",
            thumbnailUrl = "/media/posts/thumbnails/clip.jpg",
        ) { raw -> raw.takeIf(String::isNotBlank)?.let { "https://nova.example$it" }.orEmpty() }

        assertEquals("video", post.mediaType)
        assertEquals("", post.imageUrl)
        assertEquals("https://nova.example/media/posts/video/clip.mp4", post.mediaUrl)
        assertEquals("https://nova.example/media/posts/thumbnails/clip.jpg", post.thumbnailUrl)
    }

    @Test
    fun `legacy image post remains compatible`() {
        val post = normalizePostMedia(
            legacyImageUrl = "/media/posts/legacy.jpg",
            mediaType = "",
            mediaUrl = "",
            thumbnailUrl = "",
        ) { raw -> raw.takeIf(String::isNotBlank)?.let { "https://nova.example$it" }.orEmpty() }

        assertEquals("image", post.mediaType)
        assertEquals(post.imageUrl, post.mediaUrl)
        assertEquals(post.imageUrl, post.thumbnailUrl)
    }
}
