package com.nova.app.feature.messages.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.appearance.data.ConversationAppearanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


/** Conversation-scoped owner for theme loading, optimistic saving, picker state, and session effects. */
class ConversationAppearanceViewModel internal constructor(
    private val conversationId: Long,
    private val repository: ConversationAppearanceRepository,
    private val resolveThemeKey: (String?) -> String,
    private val workScope: CoroutineScope? = null,
) : ViewModel() {
    var state by mutableStateOf(ConversationAppearanceUiState())
        private set

    private val scope: CoroutineScope
        get() = workScope ?: viewModelScope

    private var loadJob: Job? = null
    private var saveJob: Job? = null

    init {
        loadJob = scope.launch { loadPreference() }
    }

    fun openPicker() {
        state = state.copy(pickerOpen = true)
    }

    fun dismissPicker() {
        if (state.savingThemeKey != null) return
        state = state.copy(pickerOpen = false)
    }

    fun selectTheme(themeKey: String) {
        if (state.savingThemeKey != null || themeKey == state.themeKey) return
        val previousKey = state.themeKey
        state = state.copy(
            themeKey = themeKey,
            savingThemeKey = themeKey,
            errorMessage = null,
        )

        saveJob?.cancel()
        saveJob = scope.launch {
            when (val result = repository.setTheme(conversationId, themeKey)) {
                is ApiResult.Success -> state = state.copy(
                    themeKey = resolveThemeKey(result.value.themeKey),
                    errorMessage = null,
                )
                is ApiResult.Failure -> {
                    state = state.copy(themeKey = previousKey)
                    handleFailure(result)
                }
            }
            state = state.copy(savingThemeKey = null)
        }
    }

    internal suspend fun loadPreference() {
        when (val result = repository.preference(conversationId)) {
            is ApiResult.Success -> state = state.copy(
                themeKey = resolveThemeKey(result.value.themeKey),
                errorMessage = null,
            )
            is ApiResult.Failure -> handleFailure(result)
        }
    }

    private fun handleFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(errorMessage = result.message)
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        saveJob?.cancel()
    }

    companion object {
        fun factory(
            conversationId: Long,
            repository: ConversationAppearanceRepository,
            resolveThemeKey: (String?) -> String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ConversationAppearanceViewModel::class.java))
                return ConversationAppearanceViewModel(
                    conversationId = conversationId,
                    repository = repository,
                    resolveThemeKey = resolveThemeKey,
                ) as T
            }
        }
    }
}
