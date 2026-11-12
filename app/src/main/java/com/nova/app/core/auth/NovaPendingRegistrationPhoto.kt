package com.nova.app.core.auth

import android.net.Uri


/**
 * Keeps the optional onboarding avatar only for the in-process registration flow.
 * It is consumed after account creation and is never persisted to disk.
 */
object NovaPendingRegistrationPhoto {
    private var pendingUri: Uri? = null
    private var generation = 0L

    internal data class Selection(val uri: Uri?, val generation: Long)

    @Synchronized
    internal fun snapshot(): Selection = Selection(pendingUri, generation)

    @Synchronized
    fun set(uri: Uri?) {
        generation += 1
        pendingUri = uri
    }

    @Synchronized
    internal fun consume(selection: Selection): Uri? {
        if (selection.generation != generation) return null
        val value = pendingUri
        generation += 1
        pendingUri = null
        return value
    }

    @Synchronized
    fun clear() {
        generation += 1
        pendingUri = null
    }
}
