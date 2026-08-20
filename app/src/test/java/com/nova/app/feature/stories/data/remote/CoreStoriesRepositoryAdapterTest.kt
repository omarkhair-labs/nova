package com.nova.app.feature.stories.data.remote

import com.nova.app.core.stories.NovaStory
import com.nova.app.core.stories.NovaStoryAuthor
import com.nova.app.core.stories.NovaStoryGroup
import com.nova.app.core.stories.NovaStorySharedPost
import com.nova.app.core.stories.NovaStorySharedReel
import com.nova.app.core.stories.NovaStoryViewer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class CoreStoriesRepositoryAdapterTest {
    @Test
    fun `story mapping preserves every current story field`() {
        val author = NovaStoryAuthor(7L, "omar", "Omar", "avatar")
        val sharedPost = NovaStorySharedPost(21L, author, "post-image", "post-caption")
        val sharedReel = NovaStorySharedReel(22L, author, "reel-video", "reel-caption")
        val source = NovaStory(
            id = 9L,
            author = author,
            mediaUrl = "story-media",
            mediaType = "video",
            caption = "caption",
            createdAt = "created",
            expiresAt = "expires",
            isMine = true,
            isViewed = false,
            myReaction = "🔥",
            viewsCount = 13,
            audience = "close_friends",
            backgroundStyle = "forest",
            sharedPost = sharedPost,
            sharedReel = sharedReel,
        )

        val mapped = source.toStable()

        assertEquals(source.id, mapped.id)
        assertEquals(source.author.id, mapped.author.id)
        assertEquals(source.author.username, mapped.author.username)
        assertEquals(source.author.name, mapped.author.name)
        assertEquals(source.author.avatarUrl, mapped.author.avatarUrl)
        assertEquals(source.mediaUrl, mapped.mediaUrl)
        assertEquals(source.mediaType, mapped.mediaType)
        assertEquals(source.caption, mapped.caption)
        assertEquals(source.createdAt, mapped.createdAt)
        assertEquals(source.expiresAt, mapped.expiresAt)
        assertEquals(source.isMine, mapped.isMine)
        assertEquals(source.isViewed, mapped.isViewed)
        assertEquals(source.myReaction, mapped.myReaction)
        assertEquals(source.viewsCount, mapped.viewsCount)
        assertEquals(source.audience, mapped.audience)
        assertEquals(source.backgroundStyle, mapped.backgroundStyle)
        assertEquals(21L, mapped.sharedPost?.id)
        assertEquals("post-image", mapped.sharedPost?.imageUrl)
        assertEquals("post-caption", mapped.sharedPost?.caption)
        assertEquals(22L, mapped.sharedReel?.id)
        assertEquals("reel-video", mapped.sharedReel?.videoUrl)
        assertEquals("reel-caption", mapped.sharedReel?.caption)
    }

    @Test
    fun `group mapping preserves order duplicates and flags`() {
        val author = NovaStoryAuthor(4L, "user", "", "")
        val first = story(3L, author)
        val duplicate = story(3L, author)
        val last = story(8L, author)
        val source = NovaStoryGroup(
            author = author,
            stories = listOf(first, duplicate, last),
            hasUnseen = true,
            isMine = false,
        )

        val mapped = source.toStable()

        assertEquals(listOf(3L, 3L, 8L), mapped.stories.map { it.id })
        assertTrue(mapped.hasUnseen)
        assertFalse(mapped.isMine)
        assertEquals("user", mapped.author.displayName)
    }

    @Test
    fun `viewer mapping preserves reaction timestamp and nullable story fields remain null`() {
        val author = NovaStoryAuthor(5L, "viewer", "Viewer", "avatar")
        val viewer = NovaStoryViewer(author, "2026-08-20T10:00:00Z", "❤️").toStable()
        val story = story(11L, author).toStable()

        assertEquals("2026-08-20T10:00:00Z", viewer.viewedAt)
        assertEquals("❤️", viewer.reaction)
        assertEquals("Viewer", viewer.user.displayName)
        assertNull(story.viewsCount)
        assertNull(story.sharedPost)
        assertNull(story.sharedReel)
    }

    private fun story(id: Long, author: NovaStoryAuthor) = NovaStory(
        id = id,
        author = author,
        mediaUrl = "",
        mediaType = "image",
        caption = "",
        createdAt = "",
        expiresAt = "",
        isMine = false,
        isViewed = false,
        myReaction = "",
        viewsCount = null,
    )
}
