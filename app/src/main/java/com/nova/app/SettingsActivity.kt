package com.nova.app

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.plants.PlantupiaBrandGlyph
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaTheme


private const val PRIVACY_POLICY_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/privacy/"
private const val ACCOUNT_DELETION_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/account-deletion/"
private const val SUPPORT_EMAIL = "omar.khair70@gmail.com"


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        val appContainer = applicationContext.appContainer
        val username = appContainer.currentCachedUsername()
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

        // QA devices often have no configured email client or browser app.
        // An unsupported external Intent must never crash the Plantupia task.
        fun launchOptionalExternal(intent: Intent, unavailableMessage: String) {
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, unavailableMessage, Toast.LENGTH_LONG).show()
            } catch (_: SecurityException) {
                Toast.makeText(this, unavailableMessage, Toast.LENGTH_LONG).show()
            }
        }

        fun openExternalUrl(url: String) {
            launchOptionalExternal(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                "No browser available to open this link.",
            )
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
                        launchOptionalExternal(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            },
                            "Device settings could not be opened.",
                        )
                    },
                    onHelpSupport = {
                        launchOptionalExternal(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL?subject=Plantupia%20support")),
                            "No email app is available. Support address: $SUPPORT_EMAIL",
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


private data class SettingsAction(val icon: NovaIconAsset, val title: String, val subtitle: String, val onClick: () -> Unit)

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
    onPrivacyPolicy: () -> Unit,
    onAccountDeletion: () -> Unit,
    onLogout: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    if (showAbout) {
        AboutPlantupiaDialog(
            appVersion = appVersion,
            onDismiss = { showAbout = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlantupiaColors.cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaBackButton(onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Settings", style = PlantupiaText.pageHeading, modifier = Modifier.semantics { heading() })
                    if (username.isNotBlank()) Text("@$username", style = PlantupiaText.supporting)
                }
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.OutlinedTextField(
                value = query, onValueChange = { query = it.take(100) }, singleLine = true,
                label = { Text("Find a setting") }, modifier = Modifier.fillMaxWidth(),
            )
            val sections = listOf(
                "ACCOUNT & PRIVACY" to listOf(
                    SettingsAction(icon = NovaIconAsset.Security, title = "Security", subtitle = "Password, signed-in devices and app lock", onClick = onSecurity),
                    SettingsAction(icon = NovaIconAsset.Privacy, title = "Privacy", subtitle = "Audience and follow requests", onClick = onPrivacy),
                    SettingsAction(icon = NovaIconAsset.Blocked, title = "Blocked accounts", subtitle = "", onClick = onBlockedAccounts),
                ),
                "NOTIFICATIONS & DEVICE" to listOf(
                    SettingsAction(icon = NovaIconAsset.Notifications, title = "Notifications", subtitle = "Messages and community activity; care reminders stay contextual", onClick = onNotifications),
                    SettingsAction(icon = NovaIconAsset.Settings, title = "Android app settings", subtitle = "Permissions and storage", onClick = onDataStorage),
                ),
                "HELP & PRODUCT" to listOf(
                    SettingsAction(icon = NovaIconAsset.Inbox, title = "Help & support", subtitle = SUPPORT_EMAIL, onClick = onHelpSupport),
                    SettingsAction(icon = NovaIconAsset.Policy, title = "Privacy policy", subtitle = "", onClick = onPrivacyPolicy),
                    SettingsAction(icon = NovaIconAsset.AccountDeletion, title = "Account deletion", subtitle = "Read the deletion process or delete directly from Security", onClick = onAccountDeletion),
                    SettingsAction(icon = NovaIconAsset.Info, title = "About Plantupia", subtitle = "Version $appVersion", onClick = { showAbout = true }),
                ),
                "SESSION" to listOf(
                    SettingsAction(icon = NovaIconAsset.Logout, title = "Log out", subtitle = "End the session on this device", onClick = onLogout),
                ),
            )
            val words = query.trim().split(" ").filter { it.isNotBlank() }
            val visible = sections.map { (section, actions) ->
                section to actions.filter { action ->
                    words.all { word -> "${action.title} ${action.subtitle} $section".contains(word, ignoreCase = true) }
                }
            }.filter { it.second.isNotEmpty() }
            if (visible.isEmpty()) {
                Text("No settings match your search.", modifier = Modifier.padding(vertical = 16.dp),
                    style = PlantupiaText.body)
                androidx.compose.material3.TextButton(onClick = { query = "" }) { Text("Show all settings") }
            }
            visible.forEach { (section, actions) ->
                Spacer(Modifier.height(24.dp))
                SettingsSectionLabel(section)
                SettingsGroup {
                    actions.forEachIndexed { index, action ->
                        if (index > 0) SettingsDivider()
                        SettingsRow(action.icon, action.title, action.subtitle, action.onClick)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun AboutPlantupiaDialog(
    appVersion: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = PlantupiaColors.paper,
            border = androidx.compose.foundation.BorderStroke(1.dp, PlantupiaColors.border),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                PlantupiaBrandGlyph(Modifier.size(42.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Plantupia",
                    modifier = Modifier.semantics { heading() },
                    style = PlantupiaText.editorialHeading,
                )
                Spacer(Modifier.height(20.dp))
                Text("NATURE & ARCHITECTURE", style = PlantupiaText.sectionLabel)
                Spacer(Modifier.height(7.dp))
                Text(
                    "A living record for spaces, plants and the care that happens over time.",
                    style = PlantupiaText.body,
                )
                Spacer(Modifier.height(14.dp))
                Text("Version $appVersion", style = PlantupiaText.supporting)
                Spacer(Modifier.height(20.dp))
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.forest,
                    shape = PlantupiaShapes.inset,
                ) {
                    Text(
                        "Done",
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = PlantupiaColors.paper,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text,
        style = PlantupiaText.sectionLabel,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PlantupiaColors.paper,
        shape = PlantupiaShapes.card,
        border = androidx.compose.foundation.BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsIcon(icon: NovaIconAsset) {
    Surface(shape = CircleShape, color = PlantupiaColors.softSage) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            NovaIcon(
                asset = icon,
                contentDescription = null,
                tint = PlantupiaColors.forest,
                modifier = Modifier.size(20.dp),
            )
        }
    }
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
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsIcon(icon)
            Column(Modifier.weight(1f)) {
                Text(title, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = PlantupiaText.supporting)
                }
            }
            if (onClick != null) SettingsForwardIcon()
        }
    }
    if (onClick == null) {
        Box(modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        Surface(onClick = onClick, color = PlantupiaColors.paper, modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingsForwardIcon() {
    NovaIcon(
        asset = NovaIconAsset.Back,
        contentDescription = null,
        tint = PlantupiaColors.secondaryText,
        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 180f },
    )
}

@Composable
private fun SettingsDivider() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 67.dp, end = 14.dp),
        color = PlantupiaColors.border,
    ) {
        Spacer(Modifier.height(1.dp))
    }
}
