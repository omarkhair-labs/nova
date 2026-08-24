package com.nova.app.feature.privacy.domain.model

import com.nova.app.feature.people.domain.model.NovaPerson


data class NovaFollowRequest(
    val id: Long,
    val requester: NovaPerson,
    val createdAt: String,
    val target: NovaPerson? = null,
)
