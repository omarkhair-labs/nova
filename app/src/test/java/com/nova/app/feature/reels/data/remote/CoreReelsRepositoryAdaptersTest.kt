package com.nova.app.feature.reels.data.remote

import com.nova.app.core.reels.NovaReel
import com.nova.app.core.reels.NovaReelAuthor
import com.nova.app.core.reels.NovaReelComment
import com.nova.app.core.reels.NovaReelCommentMutation
import com.nova.app.core.reels.NovaReelPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class CoreReelsRepositoryAdaptersTest {
    private val author = NovaReelAuthor(
        id = 7L,
        username = "nova_user",
        name = "Nova User",
        avatarUrl = "https://cdn.example/avatar.jpg",
    )

    private val reel = NovaReel(
        id = 42L,
        author = author,
        videoUrl = "https://cdn.example/reel.mp4",
        caption = "caption",
        createdAt = "2026-08-20T10:00:00Z",
        isMine = true,
        likesCount = 11,
        commentsCount = 4,
        repostsCount = 3,
        isLiked = true,
        isReposted = false,
        repostedBy = NovaReelAuthor(
            id = 8L,
            username = "friend",
            name = "",
            avatarUrl = "https://cdn.example/friend.jpg",
        ),
    )

    @Test
    fun reelMappingPreservesEveryFieldAndDisplayNameFallback() {
        val stable = reel.toStable()

        assertEquals(42L, stable.id)
        assertEquals("Nova User", stable.author.displayName)
        assertEquals("https://cdn.example/reel.mp4", stable.videoUrl)
        assertEquals("caption", stable.caption)
        assertEquals("2026-08-20T10:00:00Z", stable.createdAt)
        assertEquals(true, stable.isMine)
        assertEquals(11, stable.likesCount)
        assertEquals(4, stable.commentsCount)
        assertEquals(3, stable.repostsCount)
        assertEquals(true, stable.isLiked)
        assertEquals(false, stable.isReposted)
        assertEquals("friend", stable.repostedBy?.displayName)
    }

    @Test
    fun pageMappingPreservesOrderDuplicatesAndCursorExactly() {
        val page = NovaReelPage(
            reels = listOf(reel, reel.copy(caption = "duplicate")),
            nextCursor = "cursor-42",
        ).toStable()

        assertEquals(listOf(42L, 42L), page.reels.map { it.id })
        assertEquals(listOf("caption", "duplicate"), page.reels.map { it.caption })
        assertEquals("cursor-42", page.nextCursor)
    }

    @Test
    fun nestedCommentMappingPreservesParentReplyShapeAndCounts() {
        val reply = NovaReelComment(
            id = 101L,
            author = author,
            body = "reply",
            createdAt = "reply-time",
            isMine = false,
            parentId = 100L,
        )
        val parent = NovaReelComment(
            id = 100L,
            author = author,
            body = "parent",
            createdAt = "parent-time",
            isMine = true,
            parentId = null,
            repliesCount = 9,
            replies = listOf(reply),
        )

        val stable = NovaReelCommentMutation(parent, reel).toStable()

        assertNull(stable.comment.parentId)
        assertEquals(9, stable.comment.repliesCount)
        assertEquals(listOf(101L), stable.comment.replies.map { it.id })
        assertEquals(100L, stable.comment.replies.single().parentId)
        assertEquals(42L, stable.reel.id)
    }
}
