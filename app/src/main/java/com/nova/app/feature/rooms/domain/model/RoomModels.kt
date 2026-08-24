package com.nova.app.feature.rooms.domain.model


data class RoomPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class RoomConversation(
    val id: Long,
    val title: String,
    val avatarUrl: String,
    val membersCount: Int,
    val currentUserRole: String,
    val unreadCount: Int,
    val updatedAt: String,
)


data class RoomSummary(
    val conversation: RoomConversation,
    val description: String,
)


data class RoomMember(
    val person: RoomPerson,
    val role: String,
    val joinedAt: String,
)


data class RoomSections(
    val all: Int = 0,
    val note: Int = 0,
    val photo: Int = 0,
    val video: Int = 0,
    val music: Int = 0,
    val plan: Int = 0,
    val saved: Int = 0,
)


data class RoomDetail(
    val conversation: RoomConversation,
    val description: String,
    val sections: RoomSections,
    val members: List<RoomMember>,
)


data class RoomItem(
    val id: Long,
    val kind: String,
    val createdBy: RoomPerson?,
    val title: String,
    val body: String,
    val url: String,
    val mediaUrl: String,
    val scheduledFor: String?,
    val pinned: Boolean,
    val reminderSet: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
)


data class RoomItemPage(
    val pinned: List<RoomItem>,
    val items: List<RoomItem>,
    val nextBefore: Long?,
)


data class RoomTonightRow(
    val room: RoomSummary,
    val momentsCount: Int,
    val myMomentsCount: Int,
    val latestItem: RoomItem,
)


data class RoomTonightSnapshot(
    val isTonight: Boolean,
    val localHour: Int,
    val utcOffsetMinutes: Int,
    val startsAt: String,
    val endsAt: String,
    val roomsCount: Int,
    val momentsCount: Int,
    val rooms: List<RoomTonightRow>,
)
