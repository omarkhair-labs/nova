package com.nova.app.feature.stories.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.stories.domain.model.NovaStory
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import com.nova.app.feature.stories.domain.model.NovaStoryViewer


interface StoriesRepository {
    suspend fun stories(): ApiResult<List<NovaStoryGroup>>

    suspend fun createStory(
        mediaUri: Uri,
        caption: String = "",
        audience: String = "followers",
    ): ApiResult<NovaStory>

    suspend fun createTextStory(
        text: String,
        backgroundStyle: String = "midnight",
        audience: String = "followers",
    ): ApiResult<NovaStory>

    suspend fun markViewed(storyId: Long): ApiResult<Unit>

    suspend fun react(storyId: Long, emoji: String): ApiResult<String>

    suspend fun removeReaction(storyId: Long): ApiResult<Unit>

    suspend fun reply(storyId: Long, body: String): ApiResult<Unit>

    suspend fun viewers(storyId: Long): ApiResult<List<NovaStoryViewer>>

    suspend fun deleteStory(storyId: Long): ApiResult<Unit>
}
