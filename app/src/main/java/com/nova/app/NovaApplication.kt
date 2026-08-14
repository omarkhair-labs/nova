package com.nova.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.nova.app.app.AppContainer
import com.nova.app.core.calls.NovaTelecomRegistration
import com.nova.app.core.presence.NovaAppPresence


class NovaApplication : Application(), Application.ActivityLifecycleCallbacks {
    lateinit var appContainer: AppContainer
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var startedActivities = 0
    private var backgroundRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        NovaAppPresence.initialize(this)
        NovaTelecomRegistration.register(this)
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        backgroundRunnable?.let(mainHandler::removeCallbacks)
        backgroundRunnable = null

        startedActivities += 1
        if (startedActivities == 1) {
            NovaAppPresence.enterForeground()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities != 0) return

        val pending = Runnable {
            if (startedActivities == 0) {
                NovaAppPresence.leaveForeground()
            }
            backgroundRunnable = null
        }
        backgroundRunnable = pending
        mainHandler.postDelayed(pending, BACKGROUND_GRACE_MS)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val BACKGROUND_GRACE_MS = 800L
    }
}
