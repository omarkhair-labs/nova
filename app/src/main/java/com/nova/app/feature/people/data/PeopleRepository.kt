package com.nova.app.feature.people.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.people.domain.model.NovaPersonPage
import com.nova.app.feature.people.domain.model.NovaProfilePostPage


interface PeopleRepository {
    suspend fun people(query: String = ""): ApiResult<List<NovaPerson>>

    suspend fun person(username: String): ApiResult<NovaPerson>

    suspend fun setFollowing(
        username: String,
        follow: Boolean,
    ): ApiResult<NovaPerson>

    suspend fun setBlocked(
        username: String,
        blocked: Boolean = true,
    ): ApiResult<Unit>

    suspend fun report(
        username: String,
        reason: String,
        details: String = "",
    ): ApiResult<String>
}


interface PeoplePagingRepository {
    suspend fun people(
        query: String = "",
        cursor: String? = null,
    ): ApiResult<NovaPersonPage>

    suspend fun followers(
        username: String,
        query: String = "",
        cursor: String? = null,
    ): ApiResult<NovaPersonPage>

    suspend fun following(
        username: String,
        query: String = "",
        cursor: String? = null,
    ): ApiResult<NovaPersonPage>

    suspend fun profilePosts(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaProfilePostPage>

    suspend fun profileReposts(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaProfilePostPage>
}
