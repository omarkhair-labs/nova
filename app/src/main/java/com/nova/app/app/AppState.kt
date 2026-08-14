package com.nova.app.app

import com.nova.app.core.network.NovaUser
import com.nova.app.navigation.AppDestination


/** Durable application-shell state owned by [AppViewModel]. */
data class AppState(
    val primaryOverlay: AppDestination? = null,
    val currentUser: NovaUser? = null,
    val isBootstrapping: Boolean = true,
)
