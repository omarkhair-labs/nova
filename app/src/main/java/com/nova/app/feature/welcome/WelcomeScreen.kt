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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.feature.plants.PlantupiaBrandGlyph
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
    onConnectPlants: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaBrandGlyph(modifier = Modifier.size(34.dp))
            Text(
                text = "PLANTUPIA",
                modifier = Modifier.padding(start = 10.dp),
                color = NovaInk,
                fontSize = 26.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(72.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "NATURE  ×  ARCHITECTURE", color = NovaAccent, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = NovaAccentSoft,
            ) {
                Text(
                    text = "A living space. An unfolding story.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = NovaAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Every plant has a story.",
                color = NovaInk,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Start with the plants around you. Capture their moments, record care, and see what changes over time. Share with others only if you choose.",
                modifier = Modifier.fillMaxWidth(0.88f),
                color = NovaMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(56.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Received plants from Plantupia?", color = NovaInk, fontWeight = FontWeight.SemiBold)
            NovaPrimaryButton(text = "Connect my Plantupia plants", onClick = onConnectPlants)
            Text("Or add your own plants after signing in.", color = NovaMuted)
            NovaPrimaryButton(
                text = "Add my own plants · Create account",
                onClick = onCreateAccount,
            )
            NovaSecondaryButton(
                text = "Log in",
                onClick = onLogin,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "By continuing, you agree to Plantupia's Terms of Use and Privacy Policy.",
                modifier = Modifier.fillMaxWidth(),
                color = NovaMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onTerms) {
                    Text(
                        text = "Terms",
                        color = NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "·",
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
                TextButton(onClick = onPrivacy) {
                    Text(
                        text = "Privacy",
                        color = NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
