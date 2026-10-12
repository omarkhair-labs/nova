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

    val live = plants.filter { it.active }
    val featured = spaces.first()
    val portrait = live.firstOrNull {
        it.spaceId == featured.id && it.coverPhoto.isNotBlank()
    }?.coverPhoto.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PlantupiaSectionHeader(
            eyebrow = "PRIVATE / YOUR SPACES",
            title = "مساحاتك الخاصة",
            description = "تاريخ النباتات والرعاية ظاهر ليك وللناس المصرّح لهم فقط.",
        )
        PlantupiaSpaceHero(
            name = featured.name,
            plantCount = live.count { it.spaceId == featured.id },
            imageUrl = portrait,
            location = featured.location,
            onOpen = onOpenSpaces,
        )
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = PlantupiaVisual.mint, shape = RoundedCornerShape(15.dp),
                modifier = Modifier.weight(1f), onClick = onOpenSpaces) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${spaces.size}", color = PlantupiaVisual.ink,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("مساحات", color = PlantupiaVisual.muted, fontSize = 12.sp)
                }
            }
            Surface(color = PlantupiaVisual.mint, shape = RoundedCornerShape(15.dp),
                modifier = Modifier.weight(1f), onClick = onOpenSpaces) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${live.size}", color = PlantupiaVisual.ink,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("نباتات", color = PlantupiaVisual.muted, fontSize = 12.sp)
                }
            }
        }
        Text("البروفايل العام ومنشورات الدائرة مش بيعرضوا الصور أو الملاحظات الخاصة تلقائيًا.",
            color = PlantupiaVisual.muted, fontSize = 12.sp, lineHeight = 18.sp)
    }

}
