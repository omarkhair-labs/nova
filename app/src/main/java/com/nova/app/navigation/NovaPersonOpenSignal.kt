package com.nova.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


object NovaPersonOpenSignal {
    var requestVersion by mutableIntStateOf(0)
        private set

    var pendingUsername by mutableStateOf<String?>(null)
        private set

    fun request(username: String) {
        val clean = username.trim().lowercase()
        if (clean.isBlank()) return
        pendingUsername = clean
        requestVersion += 1
    }

    fun consume(username: String) {
        if (pendingUsername == username) pendingUsername = null
    }
}
