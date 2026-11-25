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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
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
            if (LocalDensity.current.fontScale < 1.3f) Text("LIVING SPACES", style = PlantupiaText.sectionLabel)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "A living space starts here.",
            style = PlantupiaText.pageHeading.copy(fontSize = 34.sp, lineHeight = 40.sp),
            modifier = Modifier.fillMaxWidth(0.92f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Connect what Plantupia prepared for you, or begin with the plants already around you. Care stays private. Sharing is always your choice.",
            style = PlantupiaText.body.copy(fontSize = 15.sp, lineHeight = 23.sp),
            modifier = Modifier.fillMaxWidth(0.94f),
        )

        Spacer(modifier = Modifier.height(14.dp))
        WelcomeLivingFrame()
        Spacer(modifier = Modifier.height(16.dp))

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
    val largeText = LocalDensity.current.fontScale >= 1.5f
    Surface(
        modifier = Modifier.fillMaxWidth().height(if (largeText) 190.dp else 132.dp),
        color = PlantupiaColors.forest,
        shape = RoundedCornerShape(19.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)
                val mineral = Color(0xFFC5D5A9)
                val moss = Color(0xFF72966B)
                val clay = Color(0xFFC08C6B)
                drawPath(Path().apply {
                    moveTo(size.width * .53f, size.height)
                    lineTo(size.width * .53f, size.height * .37f)
                    cubicTo(size.width * .53f, -size.height * .02f, size.width * .94f, -size.height * .02f, size.width * .94f, size.height * .37f)
                    lineTo(size.width * .94f, size.height)
                    close()
                }, Color(0xFF315947))
                drawCircle(mineral.copy(alpha = .72f), size.height * .17f, p(.83f,.26f))
                drawLine(mineral.copy(alpha = .35f), p(.08f,.83f), p(.95f,.83f), 1.dp.toPx())
                val base = p(.72f,.8f)
                drawLine(mineral, base, p(.71f,.14f), 1.5.dp.toPx())
                val leafShapes = listOf(
                    listOf(.72f,.50f,.55f,.42f,.58f,.18f,.72f,.50f),
                    listOf(.72f,.42f,.78f,.10f,.93f,.17f,.72f,.42f),
                    listOf(.72f,.66f,.52f,.59f,.52f,.38f,.72f,.66f),
                    listOf(.72f,.60f,.79f,.35f,.94f,.41f,.72f,.60f),
                )
                leafShapes.forEachIndexed { i, a ->
                    val path = Path().apply {
                        moveTo(size.width*a[0],size.height*a[1])
                        quadraticBezierTo(size.width*a[2],size.height*a[3],size.width*a[4],size.height*a[5])
                        quadraticBezierTo(size.width*(a[4]+(if(i%2==0) .13f else -.12f)),size.height*(a[5]+.18f),size.width*a[6],size.height*a[7])
                        close()
                    }
                    drawPath(path, if(i%2==0) moss else mineral)
                    drawLine(PlantupiaColors.forest.copy(alpha=.55f),p(a[0],a[1]),p(a[4],a[5]),1.dp.toPx())
                }
                drawPath(Path().apply {
                    moveTo(size.width*.65f,size.height*.75f)
                    lineTo(size.width*.79f,size.height*.75f)
                    lineTo(size.width*.77f,size.height*.92f)
                    lineTo(size.width*.67f,size.height*.92f)
                    close()
                },clay)
            }
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(.49f).padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (!largeText) Text("LIVING FORM", style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage))
                Text("A place for life.", style = PlantupiaText.editorialHeading.copy(color = PlantupiaColors.paper, fontSize = 21.sp, lineHeight = 26.sp))
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
