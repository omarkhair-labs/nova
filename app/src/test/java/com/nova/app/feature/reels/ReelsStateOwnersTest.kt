package com.nova.app.feature.reels

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.reels.data.ProfileReelsRepository
import com.nova.app.feature.reels.data.ReelWatchRepository
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.reels.domain.model.NovaReelAuthor
import com.nova.app.feature.reels.domain.model.NovaReelComment
import com.nova.app.feature.reels.domain.model.NovaReelCommentMutation
import com.nova.app.feature.reels.domain.model.NovaReelPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class ReelsStateOwnersTest {
    @Test
    fun `root feed keeps duplicates inside incoming page while dropping existing ids`() = runBlocking {
        val repository = FakeReelsRepository(
            reelPages = mutableListOf(
                ApiResult.Success(page(listOf(reel(1)), "next")),
                ApiResult.Success(page(listOf(reel(1), reel(2), reel(2)), null)),
            ),
        )
        val owner = ReelsStateOwner(
            repository = repository,
            watchRepository = FakeWatchRepository(),
            scope = testScope(),
        )

        owner.loadNow(reset = true)
        owner.loadNow(reset = false)

        assertEquals(listOf(1L, 2L, 2L), owner.state.reels.map { it.id })
        assertNull(owner.state.nextCursor)
        assertFalse(owner.state.loading)
        assertFalse(owner.state.loadingMore)
    }

    @Test
    fun `root feed 401 emits terminal effect without inline error`() = runBlocking {
        val repository = FakeReelsRepository(
            reelPages = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = ReelsStateOwner(repository, FakeWatchRepository(), testScope())

        owner.loadNow(reset = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.error)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `watch telemetry skips own and short sessions and ignores terminal failures`() = runBlocking {
        val watch = FakeWatchRepository(
            results = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = ReelsStateOwner(FakeReelsRepository(), watch, testScope())

        owner.recordWatchNow(reel = reel(1, mine = true), sessionId = "mine", watchedMs = 500, durationMs = 1000, maxPositionMs = 500)
        owner.recordWatchNow(reel = reel(2), sessionId = "short", watchedMs = 249, durationMs = 1000, maxPositionMs = 249)
        owner.recordWatchNow(reel = reel(3), sessionId = "valid", watchedMs = 250, durationMs = 1000, maxPositionMs = 250)

        assertEquals(1, watch.calls.size)
        assertEquals(3L, watch.calls.single().reelId)
        assertEquals(0, owner.state.sessionExpiryVersion)
        assertNull(owner.state.error)
    }

    @Test
    fun `profile viewer searches pages until target and preserves incoming duplicates`() = runBlocking {
        val profile = FakeProfileReelsRepository(
            authored = mutableListOf(
                ApiResult.Success(page(listOf(reel(1), reel(2)), "cursor-a")),
                ApiResult.Success(page(listOf(reel(2), reel(3), reel(3)), "cursor-b")),
            ),
        )
        val owner = ProfileReelsViewerStateOwner(
            username = "author",
            initialReelId = 3,
            profileRepository = profile,
            interactionRepository = FakeReelsRepository(),
            scope = testScope(),
        )

        owner.loadInitialNow()

        assertEquals(2, profile.authoredCalls)
        assertEquals(listOf(1L, 2L, 3L, 3L), owner.state.reels.map { it.id })
        assertEquals("cursor-b", owner.state.nextCursor)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `profile viewer caps initial lookup at twenty pages`() = runBlocking {
        val results = MutableList<ApiResult<NovaReelPage>>(21) { index ->
            ApiResult.Success(page(listOf(reel(index.toLong() + 1)), "cursor-${index + 1}"))
        }
        val profile = FakeProfileReelsRepository(authored = results)
        val owner = ProfileReelsViewerStateOwner(
            username = "author",
            initialReelId = 999,
            profileRepository = profile,
            interactionRepository = FakeReelsRepository(),
            scope = testScope(),
        )

        owner.loadInitialNow()

        assertEquals(20, profile.authoredCalls)
        assertEquals(20, owner.state.reels.size)
        assertEquals("cursor-20", owner.state.nextCursor)
    }

    @Test
    fun `profile viewer 401 is terminal but profile grid 401 stays inline`() = runBlocking {
        val viewerProfile = FakeProfileReelsRepository(
            authored = mutableListOf(ApiResult.Failure("viewer expired", 401)),
        )
        val viewer = ProfileReelsViewerStateOwner(
            username = "author",
            initialReelId = 1,
            profileRepository = viewerProfile,
            interactionRepository = FakeReelsRepository(),
            scope = testScope(),
        )
        viewer.loadInitialNow()

        val gridProfile = FakeProfileReelsRepository(
            authored = mutableListOf(ApiResult.Failure("grid expired", 401)),
        )
        val grid = ProfileReelsGridStateOwner(
            username = "author",
            source = ProfileReelsSource.Authored,
            repository = gridProfile,
            scope = testScope(),
        )
        grid.loadFirstPageNow()

        assertEquals(1, viewer.state.sessionExpiryVersion)
        assertNull(viewer.state.error)
        assertEquals("grid expired", grid.state.error)
        assertFalse(grid.state.loading)
    }

    @Test
    fun `reposted grid uses reposted source and preserves page merge behavior`() = runBlocking {
        val profile = FakeProfileReelsRepository(
            reposted = mutableListOf(
                ApiResult.Success(page(listOf(reel(4)), "next")),
                ApiResult.Success(page(listOf(reel(4), reel(5), reel(5)), null)),
            ),
        )
        val owner = ProfileReelsGridStateOwner(
            username = "author",
            source = ProfileReelsSource.Reposted,
            repository = profile,
            scope = testScope(),
        )

        owner.loadFirstPageNow()
        owner.loadMoreNow()

        assertEquals(2, profile.repostedCalls)
        assertEquals(0, profile.authoredCalls)
        assertEquals(listOf(4L, 5L, 5L), owner.state.reels.map { it.id })
    }

    @Test
    fun `comment reply replacement keeps position semantics and recomputes count`() {
        val parent = comment(
            id = 10,
            repliesCount = 7,
            replies = listOf(comment(20, parentId = 10), comment(21, parentId = 10)),
        )
        val replacement = comment(20, parentId = 10, body = "replacement")

        val updated = appendComment(listOf(parent), replacement).single()

        assertEquals(listOf(21L, 20L), updated.replies.map { it.id })
        assertEquals("replacement", updated.replies.last().body)
        assertEquals(2, updated.repliesCount)
    }

    @Test
    fun `comment send clears composer after success and emits updated Reel`() = runBlocking {
        val parent = comment(10)
        val updatedReel = reel(1, comments = 3)
        val repository = FakeReelsRepository(
            commentPages = mutableListOf(ApiResult.Success(listOf(parent))),
            addCommentResults = mutableListOf(
                ApiResult.Success(
                    NovaReelCommentMutation(
                        comment = comment(11, parentId = 10),
                        reel = updatedReel,
                    )
                )
            ),
        )
        val owner = ReelCommentsStateOwner(reelId = 1, repository = repository, scope = testScope())
        owner.loadCommentsNow()
        owner.setBody("reply body")
        owner.beginReply(parent)

        owner.sendNow()

        assertEquals("", owner.state.body)
        assertNull(owner.state.replyingTo)
        assertEquals(1, owner.state.reelUpdatedVersion)
        assertEquals(updatedReel, owner.state.updatedReel)
        assertEquals(11L, owner.state.comments.single().replies.single().id)
        assertFalse(owner.state.sending)
    }

    @Test
    fun `comment 401 keeps draft and reply target while emitting terminal effect`() = runBlocking {
        val repository = FakeReelsRepository(
            addCommentResults = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = ReelCommentsStateOwner(reelId = 1, repository = repository, scope = testScope())
        val parent = comment(10)
        owner.setBody("keep me")
        owner.beginReply(parent)

        owner.sendNow()

        assertEquals("keep me", owner.state.body)
        assertEquals(parent, owner.state.replyingTo)
        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.error)
        assertFalse(owner.state.sending)
    }
}


private class FakeReelsRepository(
    private val reelPages: MutableList<ApiResult<NovaReelPage>> = mutableListOf(),
    private val createResults: MutableList<ApiResult<NovaReel>> = mutableListOf(),
    private val likeResults: MutableList<ApiResult<NovaReel>> = mutableListOf(),
    private val repostResults: MutableList<ApiResult<NovaReel>> = mutableListOf(),
    private val commentPages: MutableList<ApiResult<List<NovaReelComment>>> = mutableListOf(),
    private val addCommentResults: MutableList<ApiResult<NovaReelCommentMutation>> = mutableListOf(),
    private val deleteCommentResults: MutableList<ApiResult<NovaReel>> = mutableListOf(),
    private val deleteReplyResults: MutableList<ApiResult<NovaReel>> = mutableListOf(),
    private val deleteReelResults: MutableList<ApiResult<Unit>> = mutableListOf(),
) : ReelsRepository {
    override suspend fun reels(cursor: String?): ApiResult<NovaReelPage> = reelPages.removeFirst()

    override suspend fun createReel(videoUri: Uri, caption: String): ApiResult<NovaReel> = createResults.removeFirst()

    override suspend fun setLiked(reelId: Long, liked: Boolean): ApiResult<NovaReel> = likeResults.removeFirst()

    override suspend fun setReposted(reelId: Long, reposted: Boolean): ApiResult<NovaReel> = repostResults.removeFirst()

    override suspend fun comments(reelId: Long): ApiResult<List<NovaReelComment>> = commentPages.removeFirst()

    override suspend fun addComment(reelId: Long, body: String, parentId: Long?): ApiResult<NovaReelCommentMutation> =
        addCommentResults.removeFirst()

    override suspend fun deleteComment(commentId: Long): ApiResult<NovaReel> = deleteCommentResults.removeFirst()

    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaReel> = deleteReplyResults.removeFirst()

    override suspend fun deleteReel(reelId: Long): ApiResult<Unit> = deleteReelResults.removeFirst()
}


private class FakeProfileReelsRepository(
    private val authored: MutableList<ApiResult<NovaReelPage>> = mutableListOf(),
    private val reposted: MutableList<ApiResult<NovaReelPage>> = mutableListOf(),
) : ProfileReelsRepository {
    var authoredCalls = 0
        private set
    var repostedCalls = 0
        private set

    override suspend fun reels(username: String, cursor: String?): ApiResult<NovaReelPage> {
        authoredCalls += 1
        return authored.removeFirst()
    }

    override suspend fun repostedReels(username: String, cursor: String?): ApiResult<NovaReelPage> {
        repostedCalls += 1
        return reposted.removeFirst()
    }
}


private class FakeWatchRepository(
    private val results: MutableList<ApiResult<Unit>> = mutableListOf(),
) : ReelWatchRepository {
    data class Call(
        val reelId: Long,
        val sessionId: String,
        val watchedMs: Long,
        val durationMs: Long,
        val maxPositionMs: Long,
    )

    val calls = mutableListOf<Call>()

    override suspend fun record(
        reelId: Long,
        sessionId: String,
        watchedMs: Long,
        durationMs: Long,
        maxPositionMs: Long,
    ): ApiResult<Unit> {
        calls += Call(reelId, sessionId, watchedMs, durationMs, maxPositionMs)
        return if (results.isEmpty()) ApiResult.Success(Unit) else results.removeFirst()
    }
}


private fun testScope() = CoroutineScope(Dispatchers.Unconfined)

private fun author(id: Long = 7L) = NovaReelAuthor(
    id = id,
    username = "author$id",
    name = "Author $id",
    avatarUrl = "",
)

private fun reel(
    id: Long,
    mine: Boolean = false,
    comments: Int = 0,
) = NovaReel(
    id = id,
    author = author(),
    videoUrl = "https://example.com/$id.mp4",
    caption = "caption$id",
    createdAt = "created$id",
    isMine = mine,
    likesCount = 0,
    commentsCount = comments,
    repostsCount = 0,
    isLiked = false,
    isReposted = false,
    repostedBy = null,
)

private fun page(reels: List<NovaReel>, cursor: String?) = NovaReelPage(reels = reels, nextCursor = cursor)

private fun comment(
    id: Long,
    parentId: Long? = null,
    body: String = "comment$id",
    repliesCount: Int = 0,
    replies: List<NovaReelComment> = emptyList(),
) = NovaReelComment(
    id = id,
    author = author(),
    body = body,
    createdAt = "created$id",
    isMine = true,
    parentId = parentId,
    repliesCount = repliesCount,
    replies = replies,
)
