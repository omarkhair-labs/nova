package com.nova.app.feature.feed

import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.posts.domain.model.NovaPost
import org.junit.Assert.assertEquals
import org.junit.Test


class FeedPageMergePolicyTest {
    @Test
    fun `later page skips posts already present in the feed`() {
        val merged = mergeFeedPage(
            existing = listOf(post(1L), post(2L)),
            incoming = listOf(post(2L), post(3L)),
        )

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.id })
    }

    @Test
    fun `duplicate ids inside the same incoming page remain unchanged`() {
        val merged = mergeFeedPage(
            existing = listOf(post(1L)),
            incoming = listOf(post(3L), post(3L)),
        )

        assertEquals(listOf(1L, 3L, 3L), merged.map { it.id })
    }

    private fun post(id: Long) = NovaPost(
        id = id,
        author = NovaPostAuthor(
            id = 10L,
            username = "author",
            name = "Author",
            avatarUrl = "",
        ),
        imageUrl = "",
        caption = "",
        createdAt = "",
        isMine = false,
    )
}
