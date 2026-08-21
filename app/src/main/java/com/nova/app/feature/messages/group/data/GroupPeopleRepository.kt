package com.nova.app.feature.messages.group.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.domain.model.NovaPerson


/** First-page people lookup used by group creation and add-member pickers. */
interface GroupPeopleRepository {
    suspend fun people(query: String): ApiResult<List<NovaPerson>>
}
