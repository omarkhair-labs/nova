package com.nova.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


enum class NovaRootTab {
    Home,
    People,
    Profile,
}


object NovaRootNavigationSignal {
    var requestVersion by mutableIntStateOf(0)
        private set

    var pendingTab by mutableStateOf<NovaRootTab?>(null)
        private set

    fun request(tab: NovaRootTab) {
        pendingTab = tab
        requestVersion += 1
    }

    fun consume(tab: NovaRootTab) {
        if (pendingTab == tab) pendingTab = null
    }
}
