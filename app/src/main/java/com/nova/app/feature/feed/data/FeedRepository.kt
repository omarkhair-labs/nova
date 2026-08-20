package com.nova.app.feature.feed.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.domain.model.NovaPostPage


interface FeedRepository {
    suspend fun feed(cursor: String? = null): ApiResult<NovaPostPage>

    fun cachedFeed(userId: Long): NovaPostPage?
}
