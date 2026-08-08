package com.nova.app.core.push

import android.content.Context


class NovaPushStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun saveInstallationId(installationId: String) {
        prefs.edit().putString(KEY_INSTALLATION_ID, installationId).apply()
    }

    fun installationId(): String? {
        return prefs.getString(KEY_INSTALLATION_ID, null)?.takeIf { it.isNotBlank() }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "nova_push"
        const val KEY_INSTALLATION_ID = "firebase_installation_id"
    }
}
