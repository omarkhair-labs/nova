package com.nova.app.feature.posts.detail

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.data.PostRepostRepository
import com.nova.app.feature.posts.data.PostRepostResult
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


class PostDetailStateOwnerTest {
    private val post = NovaPost(
        id = 4L,
        author = NovaPostAuthor(2L, "friend", "Friend", ""),
        imageUrl = "",
        caption = "",
        createdAt = "",
        isMine = false,
        likesCount = 2,
    )

    @Test
    fun `failed detail like restores the previous post`() = runBlocking {
        val owner = owner(likeResult = ApiResult.Failure("offline"))
        owner.loadNow()

        owner.toggleLikeNow(post)

        assertEquals(post, owner.state.post)
        assertFalse(owner.state.isLiking)
        assertEquals("offline", owner.state.errorMessage)
    }

    @Test
    fun `detail repost commits authoritative server state`() = runBlocking {
        val owner = owner(
            repostResult = ApiResult.Success(PostRepostResult(4L, 6, true, true, null)),
        )
        owner.loadNow()

        owner.toggleRepostNow(post)

        assertTrue(owner.state.post?.isReposted == true)
        assertEquals(6, owner.state.post?.repostsCount)
        assertFalse(owner.state.isReposting)
        assertEquals(1, owner.state.contentMutationVersion)
    }

    private fun owner(
        likeResult: ApiResult<NovaPost> = ApiResult.Success(post.copy(isLiked = true, likesCount = 3)),
        repostResult: ApiResult<PostRepostResult> = ApiResult.Failure("unused"),
    ) = PostDetailStateOwner(
        postId = post.id,
        repository = DetailPostRepository(post, likeResult),
        repostRepository = DetailRepostRepository(repostResult),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
}


private class DetailRepostRepository(
    private val result: ApiResult<PostRepostResult>,
) : PostRepostRepository {
    override suspend fun setPostReposted(postId: Long, reposted: Boolean) = result
}


private class DetailPostRepository(
    private val post: NovaPost,
    private val likeResult: ApiResult<NovaPost>,
) : PostRepository {
    override suspend fun post(postId: Long) = ApiResult.Success(post)
    override suspend fun setLiked(postId: Long, liked: Boolean) = likeResult
    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> = unused()
    override suspend fun createPost(caption: String, imageUri: Uri): ApiResult<NovaPost> = unused()
    override suspend fun deletePost(postId: Long): ApiResult<Unit> = unused()
    override suspend fun comments(postId: Long): ApiResult<List<NovaComment>> = unused()
    override suspend fun addComment(postId: Long, body: String, parentId: Long?): ApiResult<NovaCommentMutation> = unused()
    override suspend fun deleteComment(commentId: Long): ApiResult<NovaPost> = unused()
    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost> = unused()

    private fun <T> unused(): T = error("not used")
}
