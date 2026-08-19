package com.nova.app.feature.messages.appearance


data class ConversationAppearanceUiState(
    val themeKey: String = "nova",
    val savingThemeKey: String? = null,
    val errorMessage: String? = null,
    val pickerOpen: Boolean = false,
    val sessionExpiryVersion: Int = 0,
)
