package com.nova.app.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NovaAccent,
            ) {
                Text(
                    text = "N",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Text(
                text = "nova",
                modifier = Modifier.padding(start = 10.dp),
                color = NovaInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(bottom = 76.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = NovaAccentSoft,
            ) {
                Text(
                    text = "made for your real people",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    color = NovaAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "A social space\nthat feels like yours.",
                color = NovaInk,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Share moments, stay close, and keep the people that matter in one calm place.",
                modifier = Modifier.fillMaxWidth(0.88f),
                color = NovaMuted,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaPrimaryButton(
                text = "Create account",
                onClick = onCreateAccount,
            )

            NovaSecondaryButton(
                text = "Log in",
                onClick = onLogin,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "By continuing, you agree to Nova's Terms & Privacy Policy.",
                modifier = Modifier.fillMaxWidth(),
                color = NovaMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
