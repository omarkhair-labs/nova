package com.nova.app.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaSecondaryButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.PlantupiaArrivalArtwork
import com.nova.app.feature.plants.PlantupiaBrandGlyph
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogin: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onConnectPlants: () -> Unit = {},
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(PlantupiaColors.cream).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val large = maxWidth / LocalDensity.current.fontScale >= 680.dp
        val actionsFirst = maxHeight / LocalDensity.current.fontScale < 600.dp
        val introduction: @Composable () -> Unit = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PlantupiaBrandGlyph(Modifier.size(30.dp))
                Text(
                    "PLANTUPIA", modifier = Modifier.padding(start = 10.dp),
                    color = PlantupiaColors.forestText, fontSize = 23.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "A place for life.",
                style = PlantupiaText.display,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text("Know the plants you live with. Keep their story, in the place they belong.",
                style = PlantupiaText.body)
            if (!actionsFirst || large) {
                Spacer(Modifier.height(18.dp))
                PlantupiaArrivalArtwork()
            }
        }
        val actions: @Composable () -> Unit = {
            NovaPrimaryButton(text = "Create account", onClick = onCreateAccount)
            Spacer(Modifier.height(10.dp))
            NovaSecondaryButton(text = "Log in", onClick = onLogin)
            Spacer(Modifier.height(22.dp))
            Surface(
                onClick = onConnectPlants,
                color = PlantupiaColors.softSage,
                shape = PlantupiaShapes.card,
                modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Connect delivery", style = PlantupiaText.itemTitle)
                    Text("Scan a QR or enter your delivery code", style = PlantupiaText.supporting)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Your care records stay private. Sharing is your choice.",
                style = PlantupiaText.supporting, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("By creating an account, you agree to our Terms of Use and Privacy Policy.",
                style = PlantupiaText.supporting, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onTerms, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Terms", color = PlantupiaColors.forest)
                }
                TextButton(onClick = onPrivacy, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Privacy", color = PlantupiaColors.forest)
                }
            }
        }
        Column(Modifier.widthIn(max = if (large) 900.dp else 480.dp).fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp)) {
            if (large) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.weight(1f)) { introduction() }
                    Column(Modifier.weight(1f)) { actions() }
                }
            } else {
                introduction()
                Spacer(Modifier.height(17.dp))
                actions()
                if (actionsFirst) { Spacer(Modifier.height(18.dp)); PlantupiaArrivalArtwork() }
            }
        }
    }
}

