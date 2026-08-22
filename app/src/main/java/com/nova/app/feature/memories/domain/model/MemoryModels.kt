package com.nova.app.feature.memories.domain.model


data class MemoryPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class MemoryRoom(
    val id: Long,
    val title: String,
    val avatarUrl: String,
)


data class MemoryStats(
    val pulses: Int,
    val posts: Int,
    val roomItems: Int,
    val rooms: Int,
    val people: Int,
    val nights: Int,
    val highlights: Int,
)


data class MemoryHighlight(
    val source: String,
    val id: Long,
    val occurredAt: String,
    val title: String,
    val text: String,
    val url: String,
    val mediaType: String,
    val mediaUrl: String,
    val person: MemoryPerson?,
    val room: MemoryRoom?,
)


data class MemoryPersonRow(
    val person: MemoryPerson,
    val sharedCount: Int,
)


data class MemoryRoomRow(
    val room: MemoryRoom,
    val sharedCount: Int,
)


data class WeeklyMemory(
    val startsAt: String,
    val endsAt: String,
    val utcOffsetMinutes: Int,
    val weeksAgo: Int,
    val generatedAt: String,
    val stats: MemoryStats,
    val highlights: List<MemoryHighlight>,
    val people: List<MemoryPersonRow>,
    val rooms: List<MemoryRoomRow>,
)


data class MemoryFilmScene(
    val index: Int,
    val source: String,
    val sourceId: Long,
    val mediaType: String,
    val mediaUrl: String,
    val occurredAt: String,
    val durationMs: Long,
    val trimStartMs: Long,
    val caption: String,
    val person: MemoryPerson?,
    val room: MemoryRoom?,
)


data class MemoryFilmPlan(
    val renderVersion: Int,
    val selectionVersion: String,
    val startsAt: String,
    val endsAt: String,
    val utcOffsetMinutes: Int,
    val weeksAgo: Int,
    val filmReady: Boolean,
    val mood: String,
    val targetDurationMs: Long,
    val totalDurationMs: Long,
    val coverMediaUrl: String,
    val scenes: List<MemoryFilmScene>,
)
