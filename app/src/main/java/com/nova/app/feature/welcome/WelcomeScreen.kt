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
    Box(
        Modifier.fillMaxSize().background(PlantupiaColors.cream)
            .statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PlantupiaBrandGlyph(Modifier.size(30.dp))
                Text(
                    "PLANTUPIA", modifier = Modifier.padding(start = 10.dp),
                    color = PlantupiaColors.forestText, fontSize = 23.sp,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(26.dp))
            WelcomeLivingFrame()
            Spacer(Modifier.height(26.dp))
            Text(
                "A living space starts here.",
                style = PlantupiaText.pageHeading.copy(fontSize = 36.sp, lineHeight = 43.sp),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "A place for your plants, the care you give them, and the life you build around them.",
                style = PlantupiaText.body.copy(fontSize = 15.sp, lineHeight = 23.sp),
            )
            Spacer(Modifier.height(24.dp))
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
                    Text("RECEIVED PLANTUPIA PLANTS?", style = PlantupiaText.sectionLabel)
                    Text("Your delivery has a story.", style = PlantupiaText.editorialHeading)
                    Text("Scan its QR to review your plants and connect them to your account.", style = PlantupiaText.supporting)
                    Text("Connect delivery", style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
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
    }
}

@Composable
private fun WelcomeLivingFrame() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PlantupiaColors.forest,
        shape = RoundedCornerShape(19.dp),
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 148.dp)) {
            Canvas(Modifier.matchParentSize()) {
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
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(.49f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("LIVING FORM", style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage))
                Text("A place for life.", style = PlantupiaText.editorialHeading.copy(color = PlantupiaColors.paper, fontSize = 21.sp, lineHeight = 26.sp))
            }
        }
    }
}
