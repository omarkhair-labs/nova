package com.nova.app.feature.tonight.domain.model


data class TonightPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class TonightPulse(
    val id: Long,
    val author: TonightPerson,
    val mediaUrl: String,
    val mediaType: String,
    val audience: String,
    val note: String,
    val createdAt: String,
    val expiresAt: String,
    val isMine: Boolean,
    val thumbnailUrl: String = "",
) {
    val previewUrl: String
        get() = thumbnailUrl.ifBlank { mediaUrl }
}


data class TonightPersonRow(
    val person: TonightPerson,
    val momentsCount: Int,
    val latestPulse: TonightPulse,
)


data class TonightSnapshot(
    val isTonight: Boolean,
    val localHour: Int,
    val utcOffsetMinutes: Int,
    val startsAt: String,
    val endsAt: String,
    val peopleCount: Int,
    val momentsCount: Int,
    val myMomentsCount: Int,
    val people: List<TonightPersonRow>,
)
