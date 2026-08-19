package com.nova.app.feature.messages.group

import com.nova.app.feature.messages.group.model.ManagedGroupDetail


data class GroupInfoUiState(
    val detail: ManagedGroupDetail? = null,
    val currentUsername: String = "",
    val loading: Boolean = true,
    val busyAction: String? = null,
    val leaving: Boolean = false,
    val deleting: Boolean = false,
    val showAddMembers: Boolean = false,
    val editingTitle: Boolean = false,
    val titleDraft: String = "",
    val errorMessage: String? = null,
    val groupUpdatedVersion: Int = 0,
    val groupLeftVersion: Int = 0,
    val sessionExpiryVersion: Int = 0,
) {
    val blocked: Boolean
        get() = leaving || deleting || busyAction != null
}
