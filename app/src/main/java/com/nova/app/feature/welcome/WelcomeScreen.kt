package com.nova.app.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaBrandMark
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(NovaBackground)
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NovaBrandMark(modifier = Modifier.size(34.dp))
            Text("Nova", Modifier.padding(start = 10.dp), NovaInk, 21.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(72.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            NovaBrandMark(modifier = Modifier.size(92.dp))
            Spacer(Modifier.height(22.dp))
            Surface(shape = RoundedCornerShape(28.dp), color = NovaAccentSoft) {
                Text("made for your real people", Modifier.padding(horizontal = 16.dp, vertical = 9.dp), NovaAccent, 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
            Text("A social space\nthat feels like yours.", color = NovaInk, fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Text("Share moments, stay close, and keep the people that matter in one calm place.", Modifier.fillMaxWidth(0.88f), NovaMuted, 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(56.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NovaPrimaryButton(text = "Create account", onClick = onCreateAccount)
            NovaSecondaryButton(text = "Log in", onClick = onLogin)
            Spacer(Modifier.height(2.dp))
            Text("By continuing, you agree to Nova's", Modifier.fillMaxWidth(), NovaMuted, 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onTerms) { Text("Terms of Use", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                Text("·", color = NovaMuted, fontSize = 11.sp)
                TextButton(onClick = onPrivacy) { Text("Privacy", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
