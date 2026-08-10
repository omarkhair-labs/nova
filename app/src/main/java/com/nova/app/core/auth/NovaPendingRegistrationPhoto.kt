package com.nova.app.core.auth

import android.net.Uri


/**
 * Keeps the optional onboarding avatar only for the in-process registration flow.
 * It is consumed after account creation and is never persisted to disk.
 */
object NovaPendingRegistrationPhoto {
    @Volatile
    private var pendingUri: Uri? = null

    fun set(uri: Uri?) {
        pendingUri = uri
    }

    fun consume(): Uri? {
        val value = pendingUri
        pendingUri = null
        return value
    }

    fun clear() {
        pendingUri = null
    }
}
