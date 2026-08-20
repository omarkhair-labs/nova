package com.nova.app.feature.notifications

import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.notifications.NovaNotification as CoreNotification
import com.nova.app.core.notifications.NovaNotificationPage as CoreNotificationPage
import com.nova.app.feature.notifications.data.remote.toStableNotification
import com.nova.app.feature.notifications.data.remote.toStableNotificationPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class CoreNotificationsRepositoryAdapterTest {
    @Test
    fun `notification mapping preserves every live field`() {
        val actor = NovaPostAuthor(
            id = 7,
            username = "actor",
            name = "Actor Name",
            avatarUrl = "https://example.com/a.jpg",
        )
        val core = CoreNotification(
            id = 42,
            kind = "reel_comment",
            actor = actor,
            postId = 11,
            reelId = 23,
            reelAuthorUsername = "reel_author",
            commentPreview = "preview",
            createdAt = "2026-08-20T12:00:00Z",
            isRead = false,
        )

        val stable = core.toStableNotification()

        assertEquals(42L, stable.id)
        assertEquals("reel_comment", stable.kind)
        assertEquals(actor, stable.actor)
        assertEquals(11L, stable.postId)
        assertEquals(23L, stable.reelId)
        assertEquals("reel_author", stable.reelAuthorUsername)
        assertEquals("preview", stable.commentPreview)
        assertEquals("2026-08-20T12:00:00Z", stable.createdAt)
        assertFalse(stable.isRead)
    }

    @Test
    fun `page mapping preserves order cursor unread count and nullable targets`() {
        val actor = NovaPostAuthor(1, "actor", "", "")
        val first = CoreNotification(
            id = 1,
            kind = "follow",
            actor = actor,
            postId = null,
            reelId = null,
            reelAuthorUsername = "",
            commentPreview = "",
            createdAt = "2026-08-20T12:00:00Z",
            isRead = true,
        )
        val second = first.copy(id = 2, kind = "like", postId = 9)

        val stable = CoreNotificationPage(
            notifications = listOf(first, second),
            nextCursor = "next-page",
            unreadCount = 3,
        ).toStableNotificationPage()

        assertEquals(listOf(1L, 2L), stable.notifications.map { it.id })
        assertNull(stable.notifications.first().postId)
        assertNull(stable.notifications.first().reelId)
        assertEquals("next-page", stable.nextCursor)
        assertEquals(3, stable.unreadCount)
    }
}
