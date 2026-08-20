package com.nova.app.feature.stories.data.remote

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.core.stories.NovaStoriesRepository
import com.nova.app.core.stories.NovaStory as CoreStory
import com.nova.app.core.stories.NovaStoryAuthor as CoreStoryAuthor
import com.nova.app.core.stories.NovaStoryGroup as CoreStoryGroup
import com.nova.app.core.stories.NovaStorySharedPost as CoreStorySharedPost
import com.nova.app.core.stories.NovaStorySharedReel as CoreStorySharedReel
import com.nova.app.core.stories.NovaStoryViewer as CoreStoryViewer
import com.nova.app.feature.stories.data.StoriesRepository
import com.nova.app.feature.stories.domain.model.NovaStory
import com.nova.app.feature.stories.domain.model.NovaStoryAuthor
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import com.nova.app.feature.stories.domain.model.NovaStorySharedPost
import com.nova.app.feature.stories.domain.model.NovaStorySharedReel
import com.nova.app.feature.stories.domain.model.NovaStoryViewer


/** Production bridge while the existing HTTP/auth/media implementation remains behaviorally untouched. */
class CoreStoriesRepositoryAdapter(
    private val delegate: NovaStoriesRepository,
) : StoriesRepository {
    override suspend fun stories(): ApiResult<List<NovaStoryGroup>> =
        delegate.stories().mapValue { groups -> groups.map { it.toStable() } }

    override suspend fun createStory(
        mediaUri: Uri,
        caption: String,
        audience: String,
    ): ApiResult<NovaStory> = delegate.createStory(mediaUri, caption, audience).mapValue { it.toStable() }

    override suspend fun createTextStory(
        text: String,
        backgroundStyle: String,
        audience: String,
    ): ApiResult<NovaStory> =
        delegate.createTextStory(text, backgroundStyle, audience).mapValue { it.toStable() }

    override suspend fun markViewed(storyId: Long): ApiResult<Unit> = delegate.markViewed(storyId)

    override suspend fun react(storyId: Long, emoji: String): ApiResult<String> = delegate.react(storyId, emoji)

    override suspend fun removeReaction(storyId: Long): ApiResult<Unit> = delegate.removeReaction(storyId)

    override suspend fun reply(storyId: Long, body: String): ApiResult<Unit> = delegate.reply(storyId, body)

    override suspend fun viewers(storyId: Long): ApiResult<List<NovaStoryViewer>> =
        delegate.viewers(storyId).mapValue { viewers -> viewers.map { it.toStable() } }

    override suspend fun deleteStory(storyId: Long): ApiResult<Unit> = delegate.deleteStory(storyId)
}


private inline fun <T, R> ApiResult<T>.mapValue(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

internal fun CoreStoryAuthor.toStable() = NovaStoryAuthor(
    id = id,
    username = username,
    name = name,
    avatarUrl = avatarUrl,
)

internal fun CoreStorySharedPost.toStable() = NovaStorySharedPost(
    id = id,
    author = author.toStable(),
    imageUrl = imageUrl,
    caption = caption,
)

internal fun CoreStorySharedReel.toStable() = NovaStorySharedReel(
    id = id,
    author = author.toStable(),
    videoUrl = videoUrl,
    caption = caption,
)

internal fun CoreStory.toStable() = NovaStory(
    id = id,
    author = author.toStable(),
    mediaUrl = mediaUrl,
    mediaType = mediaType,
    caption = caption,
    createdAt = createdAt,
    expiresAt = expiresAt,
    isMine = isMine,
    isViewed = isViewed,
    myReaction = myReaction,
    viewsCount = viewsCount,
    audience = audience,
    backgroundStyle = backgroundStyle,
    sharedPost = sharedPost?.toStable(),
    sharedReel = sharedReel?.toStable(),
)

internal fun CoreStoryGroup.toStable() = NovaStoryGroup(
    author = author.toStable(),
    stories = stories.map { it.toStable() },
    hasUnseen = hasUnseen,
    isMine = isMine,
)

internal fun CoreStoryViewer.toStable() = NovaStoryViewer(
    user = user.toStable(),
    viewedAt = viewedAt,
    reaction = reaction,
)
