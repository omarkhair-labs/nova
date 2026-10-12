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
        containerColor = PlantupiaVisual.paper,
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "care-heading") {
                PlantupiaSectionHeader(
                    eyebrow = "CARE / YOUR LIVING WORLD",
                    title = "رعايتك، على مهل.",
                    description = "اللي عملته فعلًا، واللي اخترت ترجع تراجعه.",
                )
            }
            if (loading) item {
                CircularProgressIndicator(color = PlantupiaVisual.ink)
            }
            if (error != null) item {
                Surface(color = PlantupiaVisual.white, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(error.orEmpty(), color = PlantupiaVisual.muted, fontSize = 13.sp)
                        OutlinedButton(onClick = { refresh() }) { Text("حاول تاني") }
                    }
                }
            }
            if (!loading && error == null && plants.isEmpty()) item {
                Surface(color = PlantupiaVisual.mint, shape = RoundedCornerShape(23.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        NovaIcon(NovaIconAsset.Care, "الرعاية", tint = PlantupiaVisual.ink,
                            modifier = Modifier.size(36.dp))
                        Text("ابدأ من نباتك الأول", style = NovaType.sectionTitle,
                            color = PlantupiaVisual.ink)
                        Text("سجّل اسم النبات ومكانه. كل خطوة رعاية تضيفها هتبقى جزءًا من تاريخه.",
                            color = PlantupiaVisual.muted, style = NovaType.bodyCompact)
                        Button(onClick = {
                            PlantupiaCreateIntent.requestAddPlant()
                            NovaRootNavigationSignal.request(NovaRootTab.Home)
                        }, colors = ButtonDefaults.buttonColors(containerColor = PlantupiaVisual.ink)) {
                            Text("أضف نباتك")
                        }
                    }
                }
            }
            if (!loading && error == null && plants.isNotEmpty()) {
                item(key = "care-overview") {
                    Surface(color = PlantupiaVisual.ink, shape = RoundedCornerShape(25.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            NovaIcon(NovaIconAsset.Care, "الرعاية", tint = PlantupiaVisual.white,
                                modifier = Modifier.size(43.dp))
                            Column(Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${due.size} مراجعة تستحق انتباهك",
                                    style = NovaType.sectionTitle, color = PlantupiaVisual.white)
                                Text(
                                    if (due.isNotEmpty()) "دي مواعيد متابعة اخترتها. افتح النبات وشوف النتيجة."
                                    else if (upcoming.isNotEmpty()) "عندك ${upcoming.size} متابعة جاية، ومفيش حاجة متأخرة."
                                    else "مفيش متابعة محددة لسه. تقدر تختار واحدة بعد أي رعاية.",
                                    color = Color(0xFFD3E3D5), style = NovaType.bodyCompact,
                                )
                            }
                        }
                    }
                }
                item(key = "followup-header") {
                    PlantupiaSectionHeader(
                        eyebrow = "CHECK IN / 01",
                        title = "راجع اللي اتغيّر",
                        description = "المتابعة مش أمر بالسقي؛ افتح نباتك وقارن باللي سجلته قبل كده.",
                    )
                }
                if (upcoming.isEmpty()) {
                    item(key = "no-followup") {
                        Surface(color = PlantupiaVisual.white, shape = RoundedCornerShape(18.dp)) {
                            Text("لسه مفيش موعد متابعة اختارته.",
                                modifier = Modifier.padding(18.dp), color = PlantupiaVisual.muted,
                                style = NovaType.bodyCompact)
                        }
                    }
                } else items(upcoming, key = { "due-${it.id}" }) { plant ->
                    PlantupiaPlantPortrait(
                        plant = plant,
                        subtitle = if (plant in due) "موعد المراجعة وصل · ${plant.nextFollowUpOn}"
                                   else "متابعة اخترتها · ${plant.nextFollowUpOn}",
                        onClick = { openPlant(plant) },
                    )
                }
                item(key = "care-record-header") {
                    PlantupiaSectionHeader(
                        eyebrow = "YOUR PLANTS / 02",
                        title = "سجّل رعاية",
                        description = "اختار النبات اللي عملت له حاجة. تسجيل الرعاية خاص بيك.",
                    )
                }
                items(plants, key = { "record-${it.id}" }) { plant ->
                    PlantupiaPlantPortrait(
                        plant = plant,
                        subtitle = if (plant.canRecord) plant.placement.ifBlank { "افتح لتسجيل رعاية أو ملاحظة" }
                            else "تقدر تشوف السجل لكن مش تعدّله",
                        onClick = { openPlant(plant, record = true) },
                    )
                }
                if (recent.isNotEmpty()) {
                    item(key = "care-history-head") {
                        PlantupiaSectionHeader(
                            eyebrow = "LIVING HISTORY / 03",
                            title = "آخر التغييرات",
                            description = "دي أفعال وملاحظات سجّلها ناس، مش قياسات أو تشخيصات تلقائية.",
                        )
                    }
                    items(recent, key = { "history-${it.id}" }) { plant ->
                        PlantupiaPlantPortrait(plant = plant, onClick = { openPlant(plant) })
                    }
                }
            }
        }
    }
}
