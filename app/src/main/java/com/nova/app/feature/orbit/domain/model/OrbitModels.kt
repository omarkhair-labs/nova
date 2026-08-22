package com.nova.app.feature.orbit.domain.model


data class OrbitPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class OrbitPost(
    val id: Long,
    val author: OrbitPerson,
    val imageUrl: String,
    val caption: String,
)


data class OrbitPulse(
    val id: Long,
    val author: OrbitPerson,
    val mediaUrl: String,
    val mediaType: String,
    val note: String,
    val replyToId: Long?,
    val chainRootId: Long?,
)


data class OrbitEvent(
    val id: String,
    val kind: String,
    val actor: OrbitPerson,
    val createdAt: String,
    val post: OrbitPost?,
    val person: OrbitPerson?,
    val pulse: OrbitPulse?,
    val commentPreview: String,
)


data class OrbitPage(
    val events: List<OrbitEvent>,
    val nextCursor: String?,
)
