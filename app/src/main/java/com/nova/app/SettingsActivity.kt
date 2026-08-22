package com.nova.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaTheme


private const val PRIVACY_POLICY_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/privacy/"
private const val ACCOUNT_DELETION_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/account-deletion/"


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = applicationContext.appContainer
        val username = appContainer.currentCachedUsername()

        fun openExternalUrl(url: String) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        setContent {
            NovaTheme {
                SettingsScreen(
                    username = username,
                    onBack = { finish() },
                    onPrivacy = {
                        startActivity(Intent(this, PrivacyActivity::class.java))
                    },
                    onSecurity = {
                        startActivity(
                            Intent(this, AccountSecurityActivity::class.java)
                                .putExtra(
                                    AccountSecurityActivity.EXTRA_MODE,
                                    AccountSecurityActivity.MODE_SECURITY,
                                )
                        )
                    },
                    onBlockedAccounts = {
                        startActivity(
                            Intent(this, AccountSecurityActivity::class.java)
                                .putExtra(
                                    AccountSecurityActivity.EXTRA_MODE,
                                    AccountSecurityActivity.MODE_BLOCKED,
                                )
                        )
                    },
                    onPrivacyPolicy = { openExternalUrl(PRIVACY_POLICY_URL) },
                    onAccountDeletion = { openExternalUrl(ACCOUNT_DELETION_URL) },
                    onLogout = {
                        appContainer.authRepository.logout()
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


@Composable
private fun SettingsScreen(
    username: String,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onSecurity: () -> Unit,
    onBlockedAccounts: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onAccountDeletion: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaBackButton(onClick = onBack)
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = "Settings",
                    color = NovaInk,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (username.isNotBlank()) {
                    Text(
                        text = "@$username",
                        color = NovaMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(26.dp))
        Text(
            text = "Account",
            color = NovaMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column {
                SettingsRow(
                    icon = NovaIconAsset.Privacy,
                    title = "Privacy",
                    subtitle = "Private account, requests and Close Friends",
                    onClick = onPrivacy,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Security,
                    title = "Security",
                    subtitle = "Password and account protection",
                    onClick = onSecurity,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Blocked,
                    title = "Blocked accounts",
                    subtitle = "Review people you've blocked",
                    onClick = onBlockedAccounts,
                )
            }
        }

        Spacer(modifier = Modifier.size(22.dp))
        Text(
            text = "Legal & support",
            color = NovaMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column {
                SettingsRow(
                    icon = NovaIconAsset.Policy,
                    title = "Privacy policy",
                    subtitle = "How Nova handles and protects your information",
                    onClick = onPrivacyPolicy,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.AccountDeletion,
                    title = "Account deletion",
                    subtitle = "Delete in-app or request deletion on the web",
                    onClick = onAccountDeletion,
                )
            }
        }

        Spacer(modifier = Modifier.size(22.dp))
        Text(
            text = "Session",
            color = NovaMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = NovaAccentSoft,
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Logout,
                            contentDescription = null,
                            tint = NovaAccent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = "Log out",
                    color = NovaInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}


@Composable
private fun SettingsRow(
    icon: NovaIconAsset,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = NovaSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = NovaAccentSoft,
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    NovaIcon(
                        asset = icon,
                        contentDescription = null,
                        tint = NovaAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NovaInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = NovaMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}


@Composable
private fun SettingsDivider() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 65.dp, end = 14.dp),
        color = NovaBorder,
    ) {
        Spacer(modifier = Modifier.size(width = 1.dp, height = 1.dp))
    }
}
