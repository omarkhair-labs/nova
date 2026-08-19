package com.nova.app.feature.posts.comments

import com.nova.app.core.network.NovaComment
import com.nova.app.core.network.NovaPostAuthor
import org.junit.Assert.assertEquals
import org.junit.Test


class PostCommentsStateOwnerTest {
    private val author = NovaPostAuthor(1L, "author", "Author", "")

    @Test
    fun `append reply replaces same reply id and derives replies count from visible replies`() {
        val existingReply = comment(id = 20, parentId = 10)
        val parent = comment(
            id = 10,
            repliesCount = 9,
            replies = listOf(existingReply),
        )
        val replacement = existingReply.copy(body = "updated")

        val updated = appendReply(listOf(parent), replacement).single()

        assertEquals(listOf(20L), updated.replies.map { it.id })
        assertEquals("updated", updated.replies.single().body)
        assertEquals(1, updated.repliesCount)
    }

    @Test
    fun `remove reply leaves unrelated parents untouched`() {
        val reply = comment(id = 21, parentId = 10)
        val parent = comment(id = 10, repliesCount = 1, replies = listOf(reply))
        val other = comment(id = 11, repliesCount = 0)

        val updated = removeReply(listOf(parent, other), reply)

        assertEquals(0, updated[0].repliesCount)
        assertEquals(emptyList<NovaComment>(), updated[0].replies)
        assertEquals(other, updated[1])
    }

    private fun comment(
        id: Long,
        parentId: Long? = null,
        body: String = "body",
        repliesCount: Int = 0,
        replies: List<NovaComment> = emptyList(),
    ) = NovaComment(
        id = id,
        author = author,
        body = body,
        createdAt = "",
        isMine = true,
        parentId = parentId,
        repliesCount = repliesCount,
        replies = replies,
    )
}
