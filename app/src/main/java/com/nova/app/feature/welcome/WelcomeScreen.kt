package com.nova.app.feature.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlantupiaColors.cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaBrandGlyph(modifier = Modifier.size(32.dp))
            Text(
                text = "PLANTUPIA",
                modifier = Modifier.padding(start = 10.dp),
                color = PlantupiaColors.forestText,
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "NATURE × ARCHITECTURE",
                style = PlantupiaText.sectionLabel,
            )
        }

        Spacer(modifier = Modifier.height(42.dp))

        Text(
            text = "A living space starts here.",
            style = PlantupiaText.pageHeading.copy(fontSize = 39.sp, lineHeight = 45.sp),
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Connect what Plantupia prepared for you, or begin with the plants already around you. Care stays private. Sharing is always your choice.",
            style = PlantupiaText.body.copy(fontSize = 15.sp, lineHeight = 23.sp),
            modifier = Modifier.fillMaxWidth(0.94f),
        )

        Spacer(modifier = Modifier.height(24.dp))
        WelcomeLivingFrame()
        Spacer(modifier = Modifier.height(26.dp))

        Text("CHOOSE HOW YOU'RE ARRIVING", style = PlantupiaText.sectionLabel)
        Spacer(modifier = Modifier.height(10.dp))

        WelcomeJourneyCard(
            eyebrow = "FROM PLANTUPIA",
            title = "I received Plantupia plants",
            body = "Scan the delivery QR, review exactly what was prepared, then connect those plants to your account.",
            action = "Connect delivery",
            primary = true,
            onClick = onConnectPlants,
        )

        Spacer(modifier = Modifier.height(10.dp))

        WelcomeJourneyCard(
            eyebrow = "MY OWN SPACE",
            title = "I'm starting with my own plants",
            body = "Create an account, make a space, add your plants and let their care history grow from real moments.",
            action = "Create account",
            primary = false,
            onClick = onCreateAccount,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Already have a Plantupia space?", style = PlantupiaText.supporting)
            TextButton(onClick = onLogin) {
                Text(
                    "Log in",
                    color = PlantupiaColors.forest,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "By continuing, you agree to Plantupia's Terms of Use and Privacy Policy.",
            modifier = Modifier.fillMaxWidth(),
            style = PlantupiaText.supporting.copy(fontSize = 10.sp, lineHeight = 15.sp),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onTerms) {
                Text("Terms", color = PlantupiaColors.forest, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("·", color = PlantupiaColors.secondaryText, fontSize = 11.sp)
            TextButton(onClick = onPrivacy) {
                Text("Privacy", color = PlantupiaColors.forest, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WelcomeLivingFrame() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(178.dp),
        color = PlantupiaColors.softSage,
        shape = RoundedCornerShape(26.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val line = PlantupiaColors.mutedSage.copy(alpha = 0.48f)
                val strong = PlantupiaColors.forest.copy(alpha = 0.72f)
                drawLine(line, start = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .28f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .92f, size.height * .28f), strokeWidth = 1.5f)
                drawLine(line, start = androidx.compose.ui.geometry.Offset(size.width * .32f, size.height * .10f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .32f, size.height * .90f), strokeWidth = 1.5f)
                drawLine(line, start = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .10f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .90f), strokeWidth = 1.5f)
                drawCircle(strong, radius = size.minDimension * .14f,
                    center = androidx.compose.ui.geometry.Offset(size.width * .32f, size.height * .54f))
                drawCircle(PlantupiaColors.cream, radius = size.minDimension * .09f,
                    center = androidx.compose.ui.geometry.Offset(size.width * .68f, size.height * .58f))
                drawLine(strong, start = androidx.compose.ui.geometry.Offset(size.width * .50f, size.height * .28f),
                    end = androidx.compose.ui.geometry.Offset(size.width * .50f, size.height * .80f), strokeWidth = 4f)
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("SPACE  /  PLANT  /  CARE  /  HISTORY", style = PlantupiaText.sectionLabel)
                Text("The app remembers the relationship, not just the plant.", style = PlantupiaText.supporting)
            }
        }
    }
}

@Composable
private fun WelcomeJourneyCard(
    eyebrow: String,
    title: String,
    body: String,
    action: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val background = if (primary) PlantupiaColors.forest else PlantupiaColors.paper
    val titleColor = if (primary) PlantupiaColors.paper else PlantupiaColors.forestText
    val bodyColor = if (primary) PlantupiaColors.cream else PlantupiaColors.secondaryText
    val eyebrowColor = if (primary) PlantupiaColors.sage else PlantupiaColors.forest

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = background,
        shape = PlantupiaShapes.card,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(eyebrow, style = PlantupiaText.sectionLabel.copy(color = eyebrowColor))
            Text(
                title,
                style = PlantupiaText.editorialHeading.copy(color = titleColor),
            )
            Text(
                body,
                style = PlantupiaText.supporting.copy(color = bodyColor, fontSize = 12.5.sp, lineHeight = 19.sp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "$action →",
                style = PlantupiaText.sectionLabel.copy(color = titleColor),
            )
        }
    }
}
