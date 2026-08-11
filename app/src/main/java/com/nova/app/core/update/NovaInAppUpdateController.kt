package com.nova.app.core.update

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class NovaInAppUpdateController(
    private val activity: ComponentActivity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val onReadyToInstall: (Boolean) -> Unit,
) {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val preferences = activity.getSharedPreferences(PREFERENCES_NAME, Activity.MODE_PRIVATE)

    private var listenerRegistered = false
    private var checkInFlight = false
    private var flowInProgress = false

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            onReadyToInstall(true)
        }
    }

    fun start() {
        if (!listenerRegistered) {
            manager.registerListener(installListener)
            listenerRegistered = true
        }
        checkForUpdate()
    }

    fun onResume() {
        checkForUpdate()
    }

    fun onDestroy() {
        if (listenerRegistered) {
            manager.unregisterListener(installListener)
            listenerRegistered = false
        }
    }

    fun onActivityResult(resultCode: Int) {
        flowInProgress = false
        if (resultCode != Activity.RESULT_OK) {
            // Play closed the flow or the user declined it. The prompt cooldown
            // below prevents us from immediately nagging again on the next resume.
            checkForUpdate()
        }
    }

    fun completeUpdate() {
        onReadyToInstall(false)
        manager.completeUpdate().addOnFailureListener {
            onReadyToInstall(true)
        }
    }

    private fun checkForUpdate() {
        if (checkInFlight) return
        checkInFlight = true

        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                checkInFlight = false
                handleInfo(info)
            }
            .addOnFailureListener {
                checkInFlight = false
            }
    }

    private fun handleInfo(info: AppUpdateInfo) {
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            onReadyToInstall(true)
            return
        }

        if (
            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
            info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        ) {
            launchUpdate(info, NovaUpdateMode.Immediate, bypassCooldown = true)
            return
        }

        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return

        val mode = NovaUpdatePolicy.chooseMode(
            updatePriority = info.updatePriority(),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
        )
        if (mode == NovaUpdateMode.None) return

        launchUpdate(info, mode, bypassCooldown = false)
    }

    private fun launchUpdate(
        info: AppUpdateInfo,
        mode: NovaUpdateMode,
        bypassCooldown: Boolean,
    ) {
        if (flowInProgress || mode == NovaUpdateMode.None) return

        val versionCode = info.availableVersionCode()
        if (!bypassCooldown && !shouldPrompt(versionCode, mode)) return

        val playType = when (mode) {
            NovaUpdateMode.Immediate -> AppUpdateType.IMMEDIATE
            NovaUpdateMode.Flexible -> AppUpdateType.FLEXIBLE
            NovaUpdateMode.None -> return
        }
        val options = AppUpdateOptions.newBuilder(playType).build()

        val started = runCatching {
            manager.startUpdateFlowForResult(info, launcher, options)
        }.getOrDefault(false)

        if (started) {
            flowInProgress = true
            rememberPrompt(versionCode, mode)
        }
    }

    private fun shouldPrompt(versionCode: Int, mode: NovaUpdateMode): Boolean {
        val lastVersionCode = preferences.getInt(KEY_LAST_PROMPTED_VERSION, -1)
        if (lastVersionCode != versionCode) return true

        val lastPromptedAt = preferences.getLong(KEY_LAST_PROMPTED_AT, 0L)
        val elapsed = System.currentTimeMillis() - lastPromptedAt
        return elapsed >= NovaUpdatePolicy.promptCooldownMs(mode)
    }

    private fun rememberPrompt(versionCode: Int, mode: NovaUpdateMode) {
        preferences.edit()
            .putInt(KEY_LAST_PROMPTED_VERSION, versionCode)
            .putLong(KEY_LAST_PROMPTED_AT, System.currentTimeMillis())
            .putString(KEY_LAST_PROMPTED_MODE, mode.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "nova_in_app_updates"
        const val KEY_LAST_PROMPTED_VERSION = "last_prompted_version"
        const val KEY_LAST_PROMPTED_AT = "last_prompted_at"
        const val KEY_LAST_PROMPTED_MODE = "last_prompted_mode"
    }
}
