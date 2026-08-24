package com.nova.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaTheme
import com.nova.app.ui.theme.NovaType
import com.nova.app.navigation.NovaRootNavigationSignal


private const val PRIVACY_POLICY_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/privacy/"
private const val ACCOUNT_DELETION_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/account-deletion/"
private const val SUPPORT_EMAIL = "omar.khair70@gmail.com"


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = applicationContext.appContainer
        val username = appContainer.currentCachedUsername()
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

        fun openExternalUrl(url: String) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        setContent {
            NovaTheme {
                SettingsScreen(
                    username = username,
                    appVersion = appVersion,
                    onBack = { finish() },
                    onPrivacy = {
                        startActivity(Intent(this, PrivacyActivity::class.java))
                    },
                    onNotifications = {
                        startActivity(Intent(this, NotificationPreferencesActivity::class.java))
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
                    onDataStorage = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                    },
                    onHelpSupport = {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL?subject=Nova%20support")))
                    },
                    onTonight = {
                        NovaRootNavigationSignal.requestTonight()
                        startActivity(
                            Intent(this, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            )
                        )
                        finish()
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
    appVersion: String,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onNotifications: () -> Unit,
    onSecurity: () -> Unit,
    onBlockedAccounts: () -> Unit,
    onDataStorage: () -> Unit,
    onHelpSupport: () -> Unit,
    onTonight: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onAccountDeletion: () -> Unit,
    onLogout: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }
    if (showAbout) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About Nova") },
            text = { Text("Nova $appVersion\nQuiet Orbit. Real connections, when it matters.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAbout = false }) { Text("Done") }
            },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = NovaSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaBackButton(onClick = onBack)
            Spacer(modifier = Modifier.size(NovaSpacing.md))
            Column {
                Text(
                    text = "Settings",
                    color = NovaInk,
                    style = NovaType.screenTitle,
                )
                if (username.isNotBlank()) {
                    Text(
                        text = "@$username",
                        color = NovaMuted,
                        style = NovaType.meta,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.size(NovaSpacing.xxl))
        SettingsSectionLabel("Account")

        NovaCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    icon = NovaIconAsset.Security,
                    title = "Account",
                    subtitle = "Password, sessions and account protection",
                    onClick = onSecurity,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Privacy,
                    title = "Privacy",
                    subtitle = "Private account, requests and Close Friends",
                    onClick = onPrivacy,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Notifications,
                    title = "Notifications",
                    subtitle = "Activity, messages and live sessions",
                    onClick = onNotifications,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Home,
                    title = "Appearance",
                    subtitle = "Warm off-white · Light",
                    onClick = null,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.AccountDeletion,
                    title = "Data & storage",
                    subtitle = "Permissions, cache and app storage",
                    onClick = onDataStorage,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Policy,
                    title = "Language",
                    subtitle = "English",
                    onClick = null,
                )
            }
        }

        Spacer(modifier = Modifier.size(NovaSpacing.xxl))
        SettingsSectionLabel("Legal & support")

        NovaCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SettingsRow(
                    icon = NovaIconAsset.Inbox,
                    title = "Help & support",
                    subtitle = SUPPORT_EMAIL,
                    onClick = onHelpSupport,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Orbit,
                    title = "Tonight (Full)",
                    subtitle = "Open Nova's live night experience",
                    onClick = onTonight,
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Home,
                    title = "About Nova",
                    subtitle = "Version $appVersion",
                    onClick = { showAbout = true },
                )
                SettingsDivider()
                SettingsRow(
                    icon = NovaIconAsset.Blocked,
                    title = "Blocked accounts",
                    subtitle = "Review people you've blocked",
                    onClick = onBlockedAccounts,
                )
                SettingsDivider()
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

        Spacer(modifier = Modifier.size(NovaSpacing.xxl))
        SettingsSectionLabel("Session")

        NovaCard(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(NovaSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
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
                    style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}


@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        color = NovaMuted,
        style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = NovaSpacing.xs, bottom = NovaSpacing.sm),
    )
}


@Composable
private fun SettingsRow(
    icon: NovaIconAsset,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
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
                    style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = subtitle,
                    color = NovaMuted,
                    style = NovaType.meta,
                )
            }
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = NovaSurface) { content() }
    } else {
        Surface(modifier = Modifier.fillMaxWidth(), color = NovaSurface) { content() }
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
