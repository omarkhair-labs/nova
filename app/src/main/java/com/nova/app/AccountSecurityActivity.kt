package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.app.KeyguardManager
import androidx.compose.runtime.mutableStateOf
import com.nova.app.core.security.NovaAppLock
import com.nova.app.feature.auth.AccountSecurityScreen
import com.nova.app.feature.auth.BlockedAccountsScreen
import com.nova.app.feature.auth.PasswordRecoveryScreen
import com.nova.app.ui.theme.NovaTheme


class AccountSecurityActivity : ComponentActivity() {
    private val appLock by lazy { NovaAppLock(this) }
    private val appLockEnabled = mutableStateOf(false)
    private val appLockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            appLock.setEnabled(true)
            appLock.markUnlocked()
            appLockEnabled.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SECURITY
        appLockEnabled.value = appLock.enabled
        setContent {
            NovaTheme {
                when (mode) {
                    MODE_RECOVERY -> PasswordRecoveryScreen(onBack = { finish() })
                    MODE_BLOCKED -> BlockedAccountsScreen(
                        onBack = { finish() },
                        onSessionExpired = {
                            startActivity(
                                Intent(this, MainActivity::class.java).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                                )
                            )
                            finish()
                        },
                    )
                    else -> AccountSecurityScreen(
                        appLockEnabled = appLockEnabled.value,
                        appLockAvailable = appLock.deviceCanLock,
                        onAppLockChange = { enabled ->
                            if (!enabled) {
                                appLock.setEnabled(false)
                                appLockEnabled.value = false
                            } else {
                                val prompt = getSystemService(KeyguardManager::class.java)
                                    .createConfirmDeviceCredentialIntent(
                                        "Enable Nova app lock",
                                        "Confirm your device screen lock.",
                                    )
                                if (prompt != null) appLockLauncher.launch(prompt)
                            }
                        },
                        onBack = { finish() },
                        onAccountDeleted = {
                            startActivity(
                                Intent(this, MainActivity::class.java).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                                )
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "account_security_mode"
        const val MODE_RECOVERY = "recovery"
        const val MODE_SECURITY = "security"
        const val MODE_BLOCKED = "blocked"
    }
}
