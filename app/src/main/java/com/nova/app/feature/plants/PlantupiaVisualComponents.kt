package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

/** Authored native presentation for REAL plant records. No fake installations or health scores. */
internal object PlantupiaVisual {
    val ink = Color(0xFF153B2E)
    val paper = Color(0xFFF7F6F0)
    val white = Color(0xFFFFFEFA)
    val sage = Color(0xFF799A83)
    val muted = Color(0xFF64796C)
    val mint = Color(0xFFE6EDE3)
    val sand = Color(0xFFECE2D0)
}

@Composable
internal fun PlantupiaSectionHeader(
    eyebrow: String,
    title: String,
    description: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(eyebrow, color = PlantupiaVisual.sage, fontSize = 10.sp,
            letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
        Text(title, color = PlantupiaVisual.ink, fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
        if (!description.isNullOrBlank()) {
            Text(description, color = PlantupiaVisual.muted, fontSize = 13.sp,
                lineHeight = 19.sp)
        }
    }
}

/** Only shows illustration when the user has not uploaded a real photo. */
@Composable
internal fun PlantupiaBotanicalPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.linearGradient(
        listOf(Color(0xFFD8E4D5), Color(0xFFAFCCB6), Color(0xFF799E80))
    ))) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val stem = Color(0xFF365F49)
            val leaves = listOf(
                Triple(.23f, .43f, -37f), Triple(.43f, .27f, -14f),
                Triple(.65f, .36f, 34f), Triple(.77f, .55f, 45f),
                Triple(.36f, .64f, -48f), Triple(.58f, .67f, 43f)
            )
            leaves.forEach { (x, y, a) ->
                drawLine(stem.copy(alpha = .70f),
                    Offset(w * .51f, h * .99f), Offset(w * x, h * y),
                    strokeWidth = w * .009f, cap = StrokeCap.Round)
                rotate(a, pivot = Offset(w * x, h * y)) {
                    drawOval(
                        color = if (x < .5f) Color(0xFF46765D) else Color(0xFF6D9878),
                        topLeft = Offset(w * (x - .075f), h * (y - .13f)),
                        size = androidx.compose.ui.geometry.Size(w * .15f, h * .26f)
                    )
                    drawLine(Color(0xFFB1CDB2).copy(alpha = .6f),
                        Offset(w * x, h * (y - .10f)), Offset(w * x, h * (y + .10f)),
                        strokeWidth = w * .003f)
                }
            }
        }
    }
}

@Composable
internal fun PlantupiaSpaceHero(
    name: String,
    plantCount: Int,
    imageUrl: String,
    location: String,
    onOpen: () -> Unit,
    heroHeight: androidx.compose.ui.unit.Dp = 304.dp,
) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(27.dp),
        color = PlantupiaVisual.mint, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(model = imageUrl, contentDescription = "نبات حقيقي من مساحة $name",
                    modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            } else PlantupiaBotanicalPlaceholder(Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color(0x00112920), Color(0x20112920), Color(0xDD112920))
            )))
            Row(Modifier.align(Alignment.TopStart).padding(17.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Surface(color = PlantupiaVisual.white.copy(alpha = .94f),
                    shape = RoundedCornerShape(40.dp)) {
                    Text("مساحة خاصة", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = PlantupiaVisual.ink, fontSize = 12.sp)
                }
                Surface(color = PlantupiaVisual.white.copy(alpha = .8f),
                    shape = RoundedCornerShape(40.dp)) {
                    Text("$plantCount نبات", modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        color = PlantupiaVisual.ink, fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("مساحتي / LIVING SPACE", color = Color(0xFFDBE8D9),
                        fontSize = 10.sp, letterSpacing = 1.1.sp)
                    Text(name, color = Color.White, fontSize = 31.sp,
                        fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(location.ifBlank {
                        if (imageUrl.isBlank()) "صوّر نباتك عشان تبقى دي مساحتك فعلًا"
                        else "$plantCount نبات مسجّل"
                    }, color = Color(0xFFF1F1E8), fontSize = 12.sp,
                        maxLines = 2, lineHeight = 17.sp)
                }
                Surface(color = PlantupiaVisual.white.copy(alpha = .96f),
                    shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("افتح", color = PlantupiaVisual.ink, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold)
                        NovaIcon(NovaIconAsset.Back, contentDescription = null,
                            tint = PlantupiaVisual.ink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlantupiaPlantPortrait(
    plant: PlantupiaPlant,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Surface(onClick = onClick, color = PlantupiaVisual.white,
        shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(13.dp))) {
                if (plant.coverPhoto.isNotBlank()) {
                    AsyncImage(model = plant.coverPhoto, contentDescription = plant.name,
                        modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                } else {
                    PlantupiaBotanicalPlaceholder(Modifier.matchParentSize())
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plant.name, color = PlantupiaVisual.ink,
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle?.takeIf { it.isNotBlank() } ?: listOf(plant.species, plant.placement)
                    .filter(String::isNotBlank).joinToString(" · ").ifBlank { "نبات في مساحتك" },
                    color = PlantupiaVisual.muted, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                if (plant.lastEventAt.isNotBlank()) {
                    Text("آخر ملاحظة · ${plant.lastEventAt.take(10)}",
                        color = PlantupiaVisual.sage, fontSize = 10.sp)
                }
            }
            NovaIcon(NovaIconAsset.Back, contentDescription = null, tint = PlantupiaVisual.ink,
                modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun PlantupiaPlantIdentityHero(plant: PlantupiaPlant) {
    Surface(color = PlantupiaVisual.mint, shape = RoundedCornerShape(24.dp)) {
        Box(Modifier.fillMaxWidth().height(285.dp)) {
            if (plant.coverPhoto.isNotBlank())
                AsyncImage(model = plant.coverPhoto, contentDescription = plant.name,
                    modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            else PlantupiaBotanicalPlaceholder(Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color(0x33132920), Color(0xE6122920)))))
            Column(Modifier.align(Alignment.BottomStart).padding(21.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("حياة نبات / LIVING RECORD",
                    fontSize = 10.sp, color = Color(0xFFE4EEDF), letterSpacing = 1.sp)
                Text(plant.name, fontFamily = FontFamily.Serif, fontSize = 34.sp,
                    fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)
                Text(listOf(plant.species, plant.placement).filter(String::isNotBlank)
                    .joinToString(" · ").ifBlank { "نباتك ليه مكان وتاريخ" },
                    color = Color(0xFFE8EDE2), fontSize = 12.sp)
            }
        }
    }
}
