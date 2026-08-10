package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nova.app.feature.auth.AccountSecurityScreen
import com.nova.app.feature.auth.PasswordRecoveryScreen
import com.nova.app.ui.theme.NovaTheme


class AccountSecurityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SECURITY
        setContent {
            NovaTheme {
                if (mode == MODE_RECOVERY) {
                    PasswordRecoveryScreen(onBack = { finish() })
                } else {
                    AccountSecurityScreen(
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
    }
}
