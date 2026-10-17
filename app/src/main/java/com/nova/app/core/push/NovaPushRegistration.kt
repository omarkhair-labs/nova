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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


object NovaPushRegistration {
    const val CHANNEL_ID = "nova_activity"
    const val CHANNEL_NAME = "Plantupia · نشاط المجتمع"
    const val MESSAGE_CHANNEL_ID = "nova_messages"
    const val MESSAGE_CHANNEL_NAME = "Plantupia · الرسائل"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lifecycleMutex = Mutex()

    fun syncForCurrentSession(context: Context) {
        val appContext = context.applicationContext
        ensureChannel(appContext)

        val store = NovaSessionStore(appContext)
        val owner = store.identity()
        if (store.load() != null) {
            activate(appContext)
        } else {
            backgroundScope.launch {
                lifecycleMutex.withLock {
                    if (store.isCurrent(owner) && store.load() == null) FirebaseMessaging.getInstance().setAutoInitEnabled(false)
                }
            }
        }
    }

    fun activate(context: Context) {
        val appContext = context.applicationContext
        ensureChannel(appContext)

        // Firebase's FID-backed registration path performs blocking task waits internally.
        // Keep the whole registration sequence off Android's main application thread.
        val store = NovaSessionStore(appContext)
        val owner = store.load()?.identity ?: return
        backgroundScope.launch {
            lifecycleMutex.withLock {
                if (store.current(owner) == null) return@withLock
                FirebaseMessaging.getInstance().setAutoInitEnabled(true)
                FirebaseMessaging.getInstance().register()
            }
        }
    }

    fun logout(
        context: Context,
        accessToken: String?,
        signedOut: NovaSessionStore.LoginIdentity,
    ) {
        val appContext = context.applicationContext
        val pushStore = NovaPushStore(appContext)
        val installationId = pushStore.installationId()

        backgroundScope.launch {
            if (!installationId.isNullOrBlank() && !accessToken.isNullOrBlank()) {
                NovaPushApiClient().remove(
                    accessToken = accessToken,
                    installationId = installationId,
                )
            }

            lifecycleMutex.withLock {
                val store = NovaSessionStore(appContext)
                if (!store.isCurrent(signedOut) || store.load() != null) return@withLock
                FirebaseMessaging.getInstance().setAutoInitEnabled(false)
                FirebaseMessaging.getInstance().unregister()
                if (store.isCurrent(signedOut) && store.load() == null) pushStore.clear()
            }
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val activityChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "تفاعلات المجتمع والتعليقات والمتابعات في Plantupia"
        }
        val messageChannel = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            MESSAGE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "الرسائل المباشرة في Plantupia"
        }
        manager.createNotificationChannel(activityChannel)
        manager.createNotificationChannel(messageChannel)
    }
}
