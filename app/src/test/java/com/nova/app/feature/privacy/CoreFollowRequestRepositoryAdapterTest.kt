package com.nova.app.feature.privacy

import com.nova.app.core.network.NovaPerson
import com.nova.app.core.privacy.NovaFollowRequest as CoreFollowRequest
import com.nova.app.feature.privacy.data.remote.toStableFollowRequest
import org.junit.Assert.assertEquals
import org.junit.Test


class CoreFollowRequestRepositoryAdapterTest {
    @Test
    fun `follow request mapping preserves id requester and created at`() {
        val core = CoreFollowRequest(
            id = 77,
            requester = NovaPerson(
                id = 12,
                username = "Alice",
                name = "Alice A",
                avatarUrl = "https://example.com/a.jpg",
                followersCount = 10,
                followingCount = 20,
                postsCount = 30,
                isFollowing = true,
            ),
            createdAt = "2026-08-20T10:00:00Z",
        )

        val stable = core.toStableFollowRequest()

        assertEquals(core.id, stable.id)
        assertEquals(core.requester.id, stable.requester.id)
        assertEquals(core.requester.username, stable.requester.username)
        assertEquals(core.requester.name, stable.requester.name)
        assertEquals(core.requester.avatarUrl, stable.requester.avatarUrl)
        assertEquals(core.requester.followersCount, stable.requester.followersCount)
        assertEquals(core.requester.followingCount, stable.requester.followingCount)
        assertEquals(core.requester.postsCount, stable.requester.postsCount)
        assertEquals(core.requester.isFollowing, stable.requester.isFollowing)
        assertEquals(core.createdAt, stable.createdAt)
    }
}
