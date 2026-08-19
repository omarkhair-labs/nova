package com.nova.app.feature.calls.domain.model


enum class NovaCallKind(val wireValue: String) {
    Audio("audio"),
    Video("video");

    companion object {
        fun fromWire(value: String): NovaCallKind =
            if (value.equals("video", ignoreCase = true)) Video else Audio
    }
}


enum class NovaCallStatus(val wireValue: String) {
    Ringing("ringing"),
    Active("active"),
    Declined("declined"),
    Canceled("canceled"),
    Ended("ended"),
    Missed("missed"),
    Failed("failed");

    val isTerminal: Boolean
        get() = this !in setOf(Ringing, Active)

    companion object {
        fun fromWire(value: String): NovaCallStatus =
            entries.firstOrNull { it.wireValue == value } ?: Failed
    }
}


data class NovaCallPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
) {
    val displayName: String
        get() = name.ifBlank { username }
}


data class NovaCallSession(
    val id: String,
    val conversationId: Long,
    val kind: NovaCallKind,
    val status: NovaCallStatus,
    val caller: NovaCallPerson,
    val callee: NovaCallPerson,
    val peer: NovaCallPerson,
    val isCaller: Boolean,
    val createdAt: String,
    val answeredAt: String?,
    val endedAt: String?,
    val endReason: String,
    val ringTimeoutSeconds: Int,
)


data class NovaIceServer(
    val urls: List<String>,
    val username: String = "",
    val credential: String = "",
)


data class NovaIceConfig(
    val servers: List<NovaIceServer>,
    val turnConfigured: Boolean,
)
