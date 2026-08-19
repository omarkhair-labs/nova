package com.nova.app.feature.messages.group

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.group.data.GroupManagementRepository
import com.nova.app.feature.messages.group.data.GroupMembershipRepository
import com.nova.app.feature.messages.group.model.GroupMember
import com.nova.app.feature.messages.group.model.ManagedGroupDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


/** Dialog-scoped owner for group-info loading, mutations, and terminal effects. */
class GroupInfoViewModel internal constructor(
    private val conversationId: Long,
    private val managementRepository: GroupManagementRepository,
    private val membershipRepository: GroupMembershipRepository,
    currentUsername: String,
    private val workScope: CoroutineScope? = null,
) : ViewModel() {
    var state by mutableStateOf(GroupInfoUiState(currentUsername = currentUsername))
        private set

    private val scope: CoroutineScope
        get() = workScope ?: viewModelScope

    init {
        load()
    }

    fun updateTitleDraft(value: String) {
        state = state.copy(titleDraft = value.take(80))
    }

    fun toggleTitleEditing() {
        val title = state.detail?.title ?: state.titleDraft
        state = state.copy(
            titleDraft = title,
            editingTitle = !state.editingTitle,
        )
    }

    fun openAddMembers() {
        if (state.blocked) return
        state = state.copy(showAddMembers = true)
    }

    fun dismissAddMembers() {
        state = state.copy(showAddMembers = false)
    }

    fun reload() {
        load()
    }

    fun rename() {
        if (state.busyAction != null) return
        val clean = state.titleDraft.trim()
        if (clean.isBlank()) {
            state = state.copy(errorMessage = "Give the group a name.")
            return
        }
        scope.launch {
            state = state.copy(busyAction = "rename", errorMessage = null)
            when (val result = managementRepository.rename(conversationId, clean)) {
                is ApiResult.Success -> {
                    applyDetail(result.value)
                    state = state.copy(editingTitle = false)
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(busyAction = null)
        }
    }

    fun updateAvatar(uri: Uri) {
        if (state.busyAction != null) return
        scope.launch {
            state = state.copy(busyAction = "avatar", errorMessage = null)
            when (val result = managementRepository.updateAvatar(conversationId, uri)) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(busyAction = null)
        }
    }

    fun removeAvatar() {
        if (state.busyAction != null) return
        scope.launch {
            state = state.copy(busyAction = "avatar", errorMessage = null)
            when (val result = managementRepository.removeAvatar(conversationId)) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(busyAction = null)
        }
    }

    fun changeRole(member: GroupMember, role: String) {
        if (state.busyAction != null) return
        scope.launch {
            state = state.copy(
                busyAction = "role:${member.user.username}",
                errorMessage = null,
            )
            when (
                val result = managementRepository.setRole(
                    conversationId = conversationId,
                    username = member.user.username,
                    role = role,
                )
            ) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(busyAction = null)
        }
    }

    fun removeMember(member: GroupMember) {
        if (state.busyAction != null || state.leaving || state.deleting) return
        scope.launch {
            state = state.copy(
                busyAction = "remove:${member.user.username}",
                errorMessage = null,
            )
            when (val result = membershipRepository.removeMember(conversationId, member.user.username)) {
                is ApiResult.Success -> {
                    if (result.value == null) {
                        state = state.copy(groupLeftVersion = state.groupLeftVersion + 1)
                    } else {
                        load()
                    }
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(busyAction = null)
        }
    }

    fun leave() {
        if (state.leaving || state.deleting || state.busyAction != null) return
        scope.launch {
            state = state.copy(leaving = true, errorMessage = null)
            when (val result = membershipRepository.leaveGroup(conversationId)) {
                is ApiResult.Success -> {
                    state = state.copy(groupLeftVersion = state.groupLeftVersion + 1)
                }
                is ApiResult.Failure -> {
                    handleFailure(result)
                    state = state.copy(leaving = false)
                }
            }
        }
    }

    fun deleteGroup() {
        if (state.leaving || state.deleting || state.busyAction != null) return
        scope.launch {
            state = state.copy(deleting = true, errorMessage = null)
            when (val result = membershipRepository.deleteGroup(conversationId)) {
                is ApiResult.Success -> {
                    state = state.copy(groupLeftVersion = state.groupLeftVersion + 1)
                }
                is ApiResult.Failure -> {
                    handleFailure(result)
                    state = state.copy(deleting = false)
                }
            }
        }
    }

    private fun load() {
        scope.launch {
            state = state.copy(loading = true, errorMessage = null)
            when (val result = managementRepository.detail(conversationId)) {
                is ApiResult.Success -> applyDetail(result.value)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(loading = false)
        }
    }

    private fun applyDetail(updated: ManagedGroupDetail) {
        state = state.copy(
            detail = updated,
            titleDraft = updated.title,
            groupUpdatedVersion = state.groupUpdatedVersion + 1,
        )
    }

    private fun handleFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(errorMessage = result.message)
        }
    }

    companion object {
        fun factory(
            conversationId: Long,
            managementRepository: GroupManagementRepository,
            membershipRepository: GroupMembershipRepository,
            currentUsername: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(GroupInfoViewModel::class.java))
                return GroupInfoViewModel(
                    conversationId = conversationId,
                    managementRepository = managementRepository,
                    membershipRepository = membershipRepository,
                    currentUsername = currentUsername,
                ) as T
            }
        }
    }
}
