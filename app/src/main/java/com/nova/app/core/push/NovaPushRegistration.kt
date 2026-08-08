package com.nova.app.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.nova.app.core.auth.NovaSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


object NovaPushRegistration {
    const val CHANNEL_ID = "nova_activity"
    const val CHANNEL_NAME = "Nova activity"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncForCurrentSession(context: Context) {
        val appContext = context.applicationContext
        ensureChannel(appContext)

        if (NovaSessionStore(appContext).load() != null) {
            activate(appContext)
        } else {
            FirebaseMessaging.getInstance().setAutoInitEnabled(false)
        }
    }

    fun activate(context: Context) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        FirebaseMessaging.getInstance().setAutoInitEnabled(true)
        FirebaseMessaging.getInstance().register()
    }

    fun logout(
        context: Context,
        accessToken: String?,
    ) {
        val appContext = context.applicationContext
        val pushStore = NovaPushStore(appContext)
        val installationId = pushStore.installationId()

        if (!installationId.isNullOrBlank() && !accessToken.isNullOrBlank()) {
            backgroundScope.launch {
                NovaPushApiClient().remove(
                    accessToken = accessToken,
                    installationId = installationId,
                )
            }
        }

        FirebaseMessaging.getInstance().setAutoInitEnabled(false)
        FirebaseMessaging.getInstance().unregister()
        pushStore.clear()
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Likes, comments, follows and other Nova activity"
        }
        manager.createNotificationChannel(channel)
    }
}
