package com.nova.app.feature.messages.group.data.remote

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.social.NovaSocialPagingRepository
import com.nova.app.feature.messages.group.data.GroupPeopleRepository


/** Preserves the existing first-page `/people/` paging transport for group pickers. */
class GroupPeoplePagingRepository(
    context: Context,
    private val pagingRepository: NovaSocialPagingRepository = NovaSocialPagingRepository(context.applicationContext),
) : GroupPeopleRepository {
    override suspend fun people(query: String): ApiResult<List<NovaPerson>> {
        return when (val result = pagingRepository.people(query = query)) {
            is ApiResult.Success -> ApiResult.Success(result.value.people)
            is ApiResult.Failure -> result
        }
    }
}
