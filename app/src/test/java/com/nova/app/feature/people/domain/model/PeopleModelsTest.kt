package com.nova.app.feature.people.domain.model

import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.posts.domain.model.NovaPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class PeopleModelsTest {
    @Test
    fun `person page defaults privacy metadata to empty`() {
        val page = NovaPersonPage(
            people = listOf(person(1L)),
            nextCursor = "next",
        )

        assertEquals("next", page.nextCursor)
        assertTrue(page.privacyByUserId.isEmpty())
    }

    @Test
    fun `profile post page preserves incoming order duplicates and cursor exactly`() {
        val page = NovaProfilePostPage(
            posts = listOf(post(4L), post(4L), post(9L)),
            nextCursor = "cursor-2",
        )

        assertEquals(listOf(4L, 4L, 9L), page.posts.map { it.id })
        assertEquals("cursor-2", page.nextCursor)
    }

    private fun person(id: Long) = NovaPerson(
        id = id,
        username = "person$id",
        name = "Person $id",
        avatarUrl = "",
        followersCount = 0,
        followingCount = 0,
        postsCount = 0,
        isFollowing = false,
    )

    private fun post(id: Long) = NovaPost(
        id = id,
        author = NovaPostAuthor(
            id = 7L,
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
