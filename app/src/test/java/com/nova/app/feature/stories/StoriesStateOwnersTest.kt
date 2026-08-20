package com.nova.app.feature.stories

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.stories.data.StoriesRepository
import com.nova.app.feature.stories.domain.model.NovaStory
import com.nova.app.feature.stories.domain.model.NovaStoryAuthor
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import com.nova.app.feature.stories.domain.model.NovaStoryViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class StoriesStateOwnersTest {
    @Test
    fun `rail first load 401 emits terminal effect and stops spinner`() = runBlocking {
        val repository = FakeStoriesRepository(
            storiesResults = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = StoriesStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.loadNow(showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `text create success emits completion and launches legacy sibling reload`() = runBlocking {
        val refreshed = listOf(group(story(9)))
        val repository = FakeStoriesRepository(
            storiesResults = mutableListOf(ApiResult.Success(refreshed)),
            textCreateResults = mutableListOf(ApiResult.Success(story(8))),
        )
        val owner = StoriesStateOwner(repository, CoroutineScope(Dispatchers.Unconfined))

        owner.createTextStoryNow("hello", "midnight", "followers")

        assertEquals(1, owner.state.textCreatedVersion)
        assertFalse(owner.state.uploading)
        assertEquals(refreshed, owner.state.groups)
        assertEquals(1, repository.storiesCalls)
    }

    @Test
    fun `viewer starts on first unseen story`() {
        val owner = StoryViewerStateOwner(
            initialGroup = group(
                story(1, viewed = true),
                story(2, viewed = true),
                story(3, viewed = false),
                story(4, viewed = false),
            ),
            repository = FakeStoriesRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        assertEquals(2, owner.state.index)
        assertEquals(3L, owner.state.currentStory?.id)
    }

    @Test
    fun `mark viewed non401 failure stays silent while 401 expires session`() = runBlocking {
        val repository = FakeStoriesRepository(
            markViewedResults = mutableListOf(
                ApiResult.Failure("offline", 500),
                ApiResult.Failure("expired", 401),
            ),
        )
        val owner = StoryViewerStateOwner(
            initialGroup = group(story(1, viewed = false)),
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.markViewedNow(1)
        assertNull(owner.state.message)
        assertEquals(0, owner.state.sessionExpiryVersion)

        owner.markViewedNow(1)
        assertNull(owner.state.message)
        assertEquals(1, owner.state.sessionExpiryVersion)
    }

    @Test
    fun `reaction toggle ignores server reaction body and uses requested emoji`() = runBlocking {
        val repository = FakeStoriesRepository(
            reactResults = mutableListOf(ApiResult.Success("server-different")),
            removeReactionResults = mutableListOf(ApiResult.Success(Unit)),
        )
        val owner = StoryViewerStateOwner(
            initialGroup = group(story(1, reaction = "")),
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.toggleReactionNow(1, "", "🔥")
        assertEquals("🔥", owner.state.currentStory?.myReaction)

        owner.toggleReactionNow(1, "🔥", "🔥")
        assertEquals("", owner.state.currentStory?.myReaction)
    }

    @Test
    fun `reply body survives navigation then clears on successful send`() = runBlocking {
        val repository = FakeStoriesRepository(
            replyResults = mutableListOf(ApiResult.Success(Unit)),
        )
        val owner = StoryViewerStateOwner(
            initialGroup = group(story(1), story(2)),
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.setReplyBody("hello")
        owner.advance()
        assertEquals("hello", owner.state.replyBody)

        owner.sendReplyNow(2, owner.state.replyBody)

        assertEquals("", owner.state.replyBody)
        assertEquals("Reply sent to Messages.", owner.state.message)
        assertFalse(owner.state.mutationBusy)
    }

    @Test
    fun `viewers 401 is terminal without inline viewers error`() = runBlocking {
        val repository = FakeStoriesRepository(
            viewersResults = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = StoryViewerStateOwner(
            initialGroup = group(story(1, mine = true)),
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.openViewers()

        assertTrue(owner.state.viewersVisible)
        assertFalse(owner.state.viewersLoading)
        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.viewersError)
    }

    @Test
    fun `delete success emits completion and releases mutation lock`() = runBlocking {
        val repository = FakeStoriesRepository(
            deleteResults = mutableListOf(ApiResult.Success(Unit)),
        )
        val owner = StoryViewerStateOwner(
            initialGroup = group(story(1, mine = true)),
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.deleteStoryNow(1)

        assertEquals(1, owner.state.deletedVersion)
        assertFalse(owner.state.mutationBusy)
    }
}


private class FakeStoriesRepository(
    private val storiesResults: MutableList<ApiResult<List<NovaStoryGroup>>> = mutableListOf(),
    private val mediaCreateResults: MutableList<ApiResult<NovaStory>> = mutableListOf(),
    private val textCreateResults: MutableList<ApiResult<NovaStory>> = mutableListOf(),
    private val markViewedResults: MutableList<ApiResult<Unit>> = mutableListOf(),
    private val reactResults: MutableList<ApiResult<String>> = mutableListOf(),
    private val removeReactionResults: MutableList<ApiResult<Unit>> = mutableListOf(),
    private val replyResults: MutableList<ApiResult<Unit>> = mutableListOf(),
    private val viewersResults: MutableList<ApiResult<List<NovaStoryViewer>>> = mutableListOf(),
    private val deleteResults: MutableList<ApiResult<Unit>> = mutableListOf(),
) : StoriesRepository {
    var storiesCalls: Int = 0
        private set

    override suspend fun stories(): ApiResult<List<NovaStoryGroup>> {
        storiesCalls += 1
        return storiesResults.removeFirst()
    }

    override suspend fun createStory(
        mediaUri: Uri,
        caption: String,
        audience: String,
    ): ApiResult<NovaStory> = mediaCreateResults.removeFirst()

    override suspend fun createTextStory(
        text: String,
        backgroundStyle: String,
        audience: String,
    ): ApiResult<NovaStory> = textCreateResults.removeFirst()

    override suspend fun markViewed(storyId: Long): ApiResult<Unit> = markViewedResults.removeFirst()

    override suspend fun react(storyId: Long, emoji: String): ApiResult<String> = reactResults.removeFirst()

    override suspend fun removeReaction(storyId: Long): ApiResult<Unit> = removeReactionResults.removeFirst()

    override suspend fun reply(storyId: Long, body: String): ApiResult<Unit> = replyResults.removeFirst()

    override suspend fun viewers(storyId: Long): ApiResult<List<NovaStoryViewer>> = viewersResults.removeFirst()

    override suspend fun deleteStory(storyId: Long): ApiResult<Unit> = deleteResults.removeFirst()
}


private fun author(id: Long = 7L) = NovaStoryAuthor(
    id = id,
    username = "author$id",
    name = "Author $id",
    avatarUrl = "",
)

private fun story(
    id: Long,
    viewed: Boolean = false,
    mine: Boolean = false,
    reaction: String = "",
) = NovaStory(
    id = id,
    author = author(),
    mediaUrl = "https://example.com/$id.jpg",
    mediaType = "image",
    caption = "caption$id",
    createdAt = "created",
    expiresAt = "expires",
    isMine = mine,
    isViewed = viewed,
    myReaction = reaction,
    viewsCount = if (mine) 2 else null,
)

private fun group(vararg stories: NovaStory) = NovaStoryGroup(
    author = stories.firstOrNull()?.author ?: author(),
    stories = stories.toList(),
    hasUnseen = stories.any { !it.isViewed },
    isMine = stories.firstOrNull()?.isMine == true,
)
