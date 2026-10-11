package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace

private val pine = Color(0xFF123B2E)
private val sage = Color(0xFFE2EADD)

/**
 * Private authenticated profile context, separate from the public Nova
 * profile/grid. Reads only records authorized by the existing session.
 */
@Composable
fun PlantupiaProfileSpaces(onOpenSpaces: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    var spaces by remember { mutableStateOf<List<PlantupiaSpace>>(emptyList()) }
    var plants by remember { mutableStateOf<List<PlantupiaPlant>>(emptyList()) }
    var available by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        val sites = repository.spaces()
        val units = repository.plants()
        if (sites is ApiResult.Success && units is ApiResult.Success) {
            spaces = sites.value
            plants = units.value
            available = true
        }
    }

    // Until the Plantupia API is on the connected server, there is no
    // misleading demo space, fabricated count, or raw HTML error.
    if (!available || spaces.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "PRIVATE / مساحاتك الخاصة",
            color = pine,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        Surface(
            onClick = onOpenSpaces,
            color = sage,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFC6D4C5)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "المكان اللي بتشارك في رعايته",
                    color = pine,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                )
                val live = plants.filter { it.active }
                Text(
                    "${spaces.size} مساحة · ${live.size} وحدة نباتية",
                    color = Color(0xFF4D6654),
                    fontSize = 12.sp,
                )
                val image = live.firstOrNull { it.coverPhoto.isNotBlank() }?.coverPhoto
                if (image != null) {
                    AsyncImage(
                        model = image,
                        contentDescription = "صورة من نباتات مساحاتك",
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(spaces.first().name, color = pine, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("افتح مساحاتك ←", color = pine, fontSize = 13.sp)
                }
            }
        }
        Text(
            "السجل والرعاية خاصين بأعضاء المساحة المصرح لهم، مش جزء من البروفايل العام.",
            color = Color(0xFF62736A),
            fontSize = 11.sp,
            lineHeight = 17.sp,
        )
    }
}
