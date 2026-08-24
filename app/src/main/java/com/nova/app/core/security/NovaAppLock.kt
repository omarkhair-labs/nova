package com.nova.app.core.security

import android.app.KeyguardManager
import android.content.Context


class NovaAppLock(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("nova_app_lock", Context.MODE_PRIVATE)

    val enabled: Boolean
        get() = preferences.getBoolean("enabled", false)

    val deviceCanLock: Boolean
        get() = appContext.getSystemService(KeyguardManager::class.java).isDeviceSecure

    fun setEnabled(value: Boolean) {
        preferences.edit().putBoolean("enabled", value).apply()
        if (!value) markUnlocked()
    }

    fun markUnlocked() {
        preferences.edit().putLong("unlocked_until", System.currentTimeMillis() + UNLOCK_GRACE_MILLIS).apply()
    }

    fun requiresUnlock(): Boolean = enabled &&
        System.currentTimeMillis() >= preferences.getLong("unlocked_until", 0L)

    companion object {
        private const val UNLOCK_GRACE_MILLIS = 30_000L
    }
}
