package com.nova.app.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted


private data class LegalSection(
    val title: String,
    val body: String,
)


@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalScreen(
        title = "Terms of Use",
        subtitle = "The basic rules for using Nova.",
        intro = "By creating or using a Nova account, you agree to use the service lawfully and respectfully.",
        sections = listOf(
            LegalSection(
                "Your account",
                "Keep your sign-in details secure and provide information you have the right to use. You are responsible for activity performed through your account.",
            ),
            LegalSection(
                "Your content",
                "You keep ownership of content you create. You give Nova the permission needed to store, process and display that content so the product can work for you and the people you share it with.",
            ),
            LegalSection(
                "Respect and safety",
                "Do not use Nova for harassment, impersonation, spam, unlawful content, threats, exploitation or attempts to compromise another person or the service. Nova may restrict content or accounts when needed to protect people or the service.",
            ),
            LegalSection(
                "Calls and messages",
                "Messages and calls are meant for direct communication between people. Blocking stops new contact between the two accounts. Deleting an account removes its public identity while shared message history may remain for the other participant as Deleted user.",
            ),
            LegalSection(
                "Service changes",
                "Nova may change, improve or discontinue product features. Availability can also be affected by networks, devices and third-party infrastructure.",
            ),
        ),
        onBack = onBack,
    )
}


@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    LegalScreen(
        title = "Privacy",
        subtitle = "How Nova handles the data needed to run the product.",
        intro = "Nova uses account and product data to provide social, messaging, calling, security and notification features.",
        sections = listOf(
            LegalSection(
                "Information you provide",
                "This includes your email, username, display name, profile photo, posts, comments, messages, voice notes and reports you choose to submit.",
            ),
            LegalSection(
                "Product and device data",
                "Nova processes information such as follow relationships, message delivery/read state, presence, call state, notification tokens and security/session data so the app can function reliably.",
            ),
            LegalSection(
                "How it is used",
                "Data is used to operate Nova, deliver content and messages, connect calls, prevent abuse, keep accounts secure, troubleshoot reliability and improve the product.",
            ),
            LegalSection(
                "Blocking and reports",
                "Blocking separates the two accounts across discovery and new communication. Reports are stored for moderation review and are not shown to the reported account as a report from you.",
            ),
            LegalSection(
                "Deleting your account",
                "Account deletion removes your public profile and public social content and disables the account. Shared direct-message history is retained for the other participant with your identity replaced by Deleted user.",
            ),
            LegalSection(
                "Infrastructure",
                "Nova relies on service providers for hosting, storage, notifications and network delivery. Those providers process only the data needed for the services Nova uses from them.",
            ),
        ),
        onBack = onBack,
    )
}


@Composable
private fun LegalScreen(
    title: String,
    subtitle: String,
    intro: String,
    sections: List<LegalSection>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        NovaHeader(title = title, subtitle = subtitle, onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = intro,
            modifier = Modifier.fillMaxWidth(),
            color = NovaInk,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        )
        sections.forEach { section ->
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = section.title,
                color = NovaInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = section.body,
                color = NovaMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}
