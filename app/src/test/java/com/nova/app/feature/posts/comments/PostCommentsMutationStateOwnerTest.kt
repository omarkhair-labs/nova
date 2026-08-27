package com.nova.app.feature.posts.comments

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class PostCommentsMutationStateOwnerTest {
    private val currentUser = NovaPostAuthor(2L, "me", "Me", "")
    private val post = NovaPost(
        id = 7L,
        author = NovaPostAuthor(1L, "owner", "Owner", ""),
        imageUrl = "",
        caption = "",
        createdAt = "",
        isMine = false,
        commentsCount = 3,
    )

    @Test
    fun `failed optimistic comment is removed and post count rolls back`() = runBlocking {
        val owner = owner(CommentMutationRepository(addResult = ApiResult.Failure("offline")))

        owner.sendCommentNow("  hello  ")

        assertTrue(owner.state.comments.isEmpty())
        assertEquals(3, owner.state.post?.commentsCount)
        assertEquals("offline", owner.state.errorMessage)
        assertFalse(owner.state.isSending)
    }

    @Test
    fun `successful optimistic reply is replaced by persisted reply`() = runBlocking {
        val parent = comment(10L)
        val persisted = comment(33L, parentId = 10L, body = "reply")
        val owner = owner(
            CommentMutationRepository(
                addResult = ApiResult.Success(NovaCommentMutation(persisted, post)),
                loadedPost = post,
                loadedComments = listOf(parent),
            )
        )
        owner.loadNow()

        owner.sendReplyNow(parent, "reply")

        assertEquals(listOf(33L), owner.state.comments.single().replies.map { it.id })
        assertEquals(1, owner.state.comments.single().repliesCount)
        assertFalse(owner.state.isReplySending)
    }

    @Test
    fun `failed comment reaction restores the original state`() = runBlocking {
        val comment = comment(10L, likesCount = 4, liked = false)
        val owner = owner(
            CommentMutationRepository(
                addResult = ApiResult.Failure("unused"),
                likeResult = ApiResult.Failure("offline"),
                loadedPost = post,
                loadedComments = listOf(comment),
            )
        )
        owner.loadNow()

        owner.toggleLike(comment)

        assertEquals(comment, owner.state.comments.single())
        assertTrue(owner.state.replyErrorMessage.orEmpty().contains("offline"))
        assertTrue(owner.state.likingCommentIds.isEmpty())
    }

    private fun owner(repository: PostRepository) = PostCommentsStateOwner(
        postId = post.id,
        initialPost = post,
        currentUser = currentUser,
        repository = repository,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    private fun comment(
        id: Long,
        parentId: Long? = null,
        body: String = "comment",
        likesCount: Int = 0,
        liked: Boolean = false,
    ) = NovaComment(
        id = id,
        author = currentUser,
        body = body,
        createdAt = "",
        isMine = true,
        parentId = parentId,
        likesCount = likesCount,
        isLiked = liked,
    )
}


private class CommentMutationRepository(
    private val addResult: ApiResult<NovaCommentMutation>,
    private val likeResult: ApiResult<NovaComment> = ApiResult.Failure("unused"),
    private val loadedPost: NovaPost? = null,
    private val loadedComments: List<NovaComment> = emptyList(),
) : PostRepository {
    override suspend fun addComment(postId: Long, body: String, parentId: Long?) = addResult
    override suspend fun setCommentLiked(commentId: Long, liked: Boolean, isReply: Boolean) = likeResult
    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> = unused()
    override suspend fun post(postId: Long): ApiResult<NovaPost> =
        loadedPost?.let { ApiResult.Success(it) } ?: unused()
    override suspend fun createPost(caption: String, imageUri: Uri): ApiResult<NovaPost> = unused()
    override suspend fun deletePost(postId: Long): ApiResult<Unit> = unused()
    override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = unused()
    override suspend fun comments(postId: Long): ApiResult<List<NovaComment>> =
        ApiResult.Success(loadedComments)
    override suspend fun deleteComment(commentId: Long): ApiResult<NovaPost> = unused()
    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost> = unused()

    private fun <T> unused(): T = error("not used")
}
