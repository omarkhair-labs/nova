package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Truthful care attention from the user's own plant records, not sensor measurements. */
@Composable
fun PlantupiaCareHub() {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var plants by remember { mutableStateOf<List<PlantupiaPlant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        scope.launch {
            loading = true
            when (val response = repository.plants()) {
                is ApiResult.Success -> {
                    plants = response.value.filter { it.active }
                    error = null
                }
                is ApiResult.Failure -> error = response.message
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }
    fun openPlant(plant: PlantupiaPlant, record: Boolean = false) {
        PlantupiaCreateIntent.requestOpenPlant(
            plant.id, if (record && plant.canRecord) PlantupiaCreateMode.OBSERVATION else null
        )
        NovaRootNavigationSignal.request(NovaRootTab.Home)
    }
    val today = remember { LocalDate.now() }
    val upcoming = plants.filter { it.nextFollowUpOn.isNotBlank() }.sortedBy { it.nextFollowUpOn }
    val due = upcoming.filter { runCatching { !LocalDate.parse(it.nextFollowUpOn).isAfter(today) }.getOrDefault(false) }
    val recent = plants.filter { it.lastEventAt.isNotBlank() }.sortedByDescending { it.lastEventAt }.take(8)

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Care,
                onHomeClick = { NovaRootNavigationSignal.request(NovaRootTab.Home) },
                onOrbitClick = { NovaRootNavigationSignal.request(NovaRootTab.Orbit) },
                onCreateClick = { NovaRootNavigationSignal.request(NovaRootTab.Create) },
                onProfileClick = { NovaRootNavigationSignal.request(NovaRootTab.Profile) },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NovaIcon(NovaIconAsset.Like, "الرعاية", modifier = Modifier.size(28.dp), tint = NovaAccent)
                    Text("الرعاية", color = NovaAccent, style = NovaType.screenTitle)
                }
                Text("هنا المواعيد اللي اخترتها وإيه اللي وثّقته فعلًا. مش توقعات عن عطش النبات.", color = NovaMuted, style = NovaType.bodyCompact)
            }
            if (loading) item { CircularProgressIndicator(color = NovaAccent) }
            if (error != null) item {
                Surface(color = NovaSurface, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error.orEmpty(), color = NovaMuted, fontSize = 13.sp)
                        OutlinedButton(onClick = { refresh() }) { Text("جرّب تاني") }
                    }
                }
            }
            if (!loading && error == null && plants.isEmpty()) item {
                Surface(color = NovaSurface, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ابدأ بنباتك الأول", style = NovaType.sectionTitle, color = NovaAccent)
                        Text("سجّل النبات والمكان، وبعد أول رعاية هيبقى ليه تاريخ هنا.", color = NovaMuted)
                        Button(onClick = { PlantupiaCreateIntent.requestAddPlant(); NovaRootNavigationSignal.request(NovaRootTab.Home) },
                            colors = ButtonDefaults.buttonColors(containerColor = NovaAccent)) { Text("أضف نبات") }
                    }
                }
            }
            if (!loading && plants.isNotEmpty()) {
                item {
                    Text("مراجعاتك", color = NovaAccent, style = NovaType.sectionTitle)
                    Text(
                        if (upcoming.isEmpty()) "مفيش متابعة مجدولة بإيدك لسه."
                        else if (due.isNotEmpty()) "${due.size} متابعة وصلت لمعادها"
                        else "${upcoming.size} متابعة جاية اخترتها بنفسك",
                        color = NovaMuted, style = NovaType.bodyCompact,
                    )
                }
                items(upcoming, key = { "due-${it.id}" }) { plant ->
                    CarePlantCard(plant, if (plant in due) "وقت مراجعة التغيير" else "راجع يوم ${plant.nextFollowUpOn}") {
                        openPlant(plant)
                    }
                }
                item {
                    Text("سجّل رعاية", style = NovaType.sectionTitle, color = NovaAccent)
                    Text("اختار نباتك، وبعدها اللي عملته فعلًا.", color = NovaMuted, style = NovaType.bodyCompact)
                }
                items(plants, key = { "plant-${it.id}" }) { plant ->
                    CarePlantCard(plant, plant.placement.ifBlank { "نبات في مساحتك" }) {
                        openPlant(plant, record = true)
                    }
                }
                if (recent.isNotEmpty()) {
                    item { Text("آخر أحداث مسجّلة", style = NovaType.sectionTitle, color = NovaAccent) }
                    items(recent, key = { "recent-${it.id}" }) { plant ->
                        CarePlantCard(plant, "آخر تغيير: ${plant.lastEventAt.take(10)}") { openPlant(plant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CarePlantCard(plant: PlantupiaPlant, supporting: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = NovaSurface, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (plant.coverPhoto.isNotBlank()) {
                AsyncImage(model = plant.coverPhoto, contentDescription = plant.name,
                    modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop)
            } else {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE9EDE4)) {
                    NovaIcon(NovaIconAsset.Photo, contentDescription = null, tint = NovaAccent,
                        modifier = Modifier.padding(18.dp).size(32.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(plant.name, style = NovaType.label, color = NovaAccent)
                Text(supporting, style = NovaType.meta, color = NovaMuted)
            }
            NovaIcon(NovaIconAsset.Back, contentDescription = "افتح النبات",
                tint = NovaAccent, modifier = Modifier.size(18.dp))
        }
    }
}
