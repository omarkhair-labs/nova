package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import kotlinx.coroutines.launch

private val deepGreen = Color(0xFF123B2E)
private val cream = Color(0xFFF3EEDE)
private val sage = Color(0xFF9FB79A)
private val forestText = Color(0xFF16302A)
private val muted = Color(0xFF4A5A50)
private val white = Color(0xFFFFFDF7)

/**
 * Actual Plantupia client-space experience inside the existing Nova Home.
 * Pulls signed-in customer data from the Oracle-hosted API. NO demo plants.
 * The social engine remains available beneath this content on the same screen.
 */
@Composable
fun PlantupiaClientSection(onCommunityCreateClick: () -> Unit, onOpenLinkedPost: (Long) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var spaces by remember { mutableStateOf<List<PlantupiaSpace>>(emptyList()) }
    var plants by remember { mutableStateOf<List<PlantupiaPlant>>(emptyList()) }
    var currentPlantId by remember { mutableStateOf<String?>(null) }
    var expandedSpaceId by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<PlantupiaCareEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var detailLoading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plantName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var carePhotoUri by remember { mutableStateOf<Uri?>(null) }
    val carePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> carePhotoUri = uri }
    val selectedPlant = plants.firstOrNull { it.id == currentPlantId }

    suspend fun refresh() {
        loading = true
        error = null
        val spaceResult = repository.spaces()
        val plantResult = repository.plants()
        if (spaceResult is ApiResult.Failure && spaceResult.statusCode == 404 ||
            plantResult is ApiResult.Failure && plantResult.statusCode == 404
        ) {
            spaces = emptyList()
            plants = emptyList()
            error = "خدمة المساحات والنباتات مش متاحة على السيرفر الحالي. دي نسخة تجربة، وبيانات نباتاتك مش هتظهر قبل تفعيل Plantupia Backend."
        } else {
            when (spaceResult) {
                is ApiResult.Failure -> { spaces = emptyList(); error = spaceResult.message }
                is ApiResult.Success -> spaces = spaceResult.value
            }
            when (plantResult) {
                is ApiResult.Failure -> { plants = emptyList(); error = plantResult.message }
                is ApiResult.Success -> plants = plantResult.value
            }
        }
        loading = false
    }

    suspend fun openPlant(plant: PlantupiaPlant) {
        currentPlantId = plant.id
        plantName = plant.name
        note = ""
        carePhotoUri = null
        detailLoading = true
        error = null
        when (val response = repository.history(plant.id)) {
            is ApiResult.Success -> events = response.value
            is ApiResult.Failure -> error = response.message
        }
        detailLoading = false
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(spaces) {
        if (spaces.size == 1 && expandedSpaceId == null) expandedSpaceId = spaces.first().id
        if (expandedSpaceId != null && spaces.none { it.id == expandedSpaceId }) expandedSpaceId = null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cream, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (selectedPlant == null) {
            BrandEyebrow("YOUR LIVING SPACES / مساحاتك")
            Text(
                "Every space has a life.",
                fontFamily = FontFamily.Serif,
                fontSize = 27.sp,
                color = deepGreen,
                lineHeight = 31.sp,
            )
            Text(
                "نباتات المكان، تاريخها، ورعايتها — بعد التركيب، مش بس يوم التسليم.",
                color = muted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
            )
            if (loading) {
                Text("جارٍ تحميل مساحتك…", color = muted, fontSize = 13.sp)
            } else {
                if (spaces.isEmpty() && error == null) {
                    PlantupiaPanel {
                        Text("مساحتك لسه بتتجهّز.", color = forestText, fontWeight = FontWeight.Medium)
                        Text("بعد ما فريق Plantupia يربط مساحتك بحسابك، نباتاتها هتظهر هنا.", color = muted, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
                spaces.forEach { space ->
                    PlantupiaPanel {
                        Text(space.name, color = forestText, fontFamily = FontFamily.Serif, fontSize = 21.sp)
                        if (space.location.isNotBlank()) Text(space.location, color = muted, fontSize = 12.sp)
                        if (space.description.isNotBlank()) Text(space.description, color = muted, fontSize = 13.sp)
                        Text(
                            "${plants.count { it.spaceId == space.id && it.active }} living units",
                            color = deepGreen,
                            fontSize = 12.sp,
                        )
                        TextButton(onClick = {
                            expandedSpaceId = if (expandedSpaceId == space.id) null else space.id
                        }) {
                            Text(
                                if (expandedSpaceId == space.id) "اخفِ نباتات المساحة ↑" else "استكشف نباتات المساحة ↓",
                                color = deepGreen,
                            )
                        }
                    }
                    if (expandedSpaceId == space.id) {
                    plants.filter { it.spaceId == space.id && it.active }.forEach { plant ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { scope.launch { openPlant(plant) } },
                            color = white,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (plant.coverPhoto.isNotBlank()) {
                                    AsyncImage(model = plant.coverPhoto, contentDescription = "صورة ${plant.name}", modifier = Modifier.fillMaxWidth().height(166.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                                }
                                Text(plant.name, color = forestText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                                val subtitle = listOf(plant.species, plant.placement).filter(String::isNotBlank).joinToString(" · ")
                                if (subtitle.isNotBlank()) Text(subtitle, color = muted, fontSize = 12.sp)
                                if (plant.lastEventAt.isNotBlank()) {
                                    val lastKind = when (plant.lastEventKind) {
                                        "issue" -> "ملاحظة تحتاج متابعة"
                                        "care" -> "رعاية مسجلة"
                                        "outcome" -> "نتيجة موثقة"
                                        else -> "ملاحظة مسجلة"
                                    }
                                    Text("آخر سجل: ${plant.lastEventAt.take(10)} · $lastKind", color = muted, fontSize = 12.sp)
                                } else {
                                    Text("لم يُسجَّل تاريخ لهذا النبات بعد", color = muted, fontSize = 12.sp)
                                }
                                Text("شوف تاريخ النبات ←", color = deepGreen, fontSize = 12.sp)
                            }
                        }
                    }
                    }
                }
                if (spaces.isNotEmpty() && plants.none { it.active }) {
                    Text("الفريق لسه ما أضافش نباتات للمساحة.", color = muted, fontSize = 12.sp)
                }
            }
            if (error != null) Text(error.orEmpty(), color = Color(0xFF9A4A31), fontSize = 12.sp)
            TextButton(onClick = { scope.launch { refresh() } }) {
                Text("تحديث البيانات", color = deepGreen)
            }
        } else {
            TextButton(onClick = { currentPlantId = null; error = null }) {
                Text("← الرجوع للمساحة", color = deepGreen)
            }
            BrandEyebrow("LIVING RECORD / تاريخ النبات")
            if (selectedPlant.coverPhoto.isNotBlank()) {
                AsyncImage(model = selectedPlant.coverPhoto, contentDescription = "صورة النبات", modifier = Modifier.fillMaxWidth().height(236.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            }
            Text(selectedPlant.name, fontFamily = FontFamily.Serif, fontSize = 35.sp, color = deepGreen)
            val meta = listOf(selectedPlant.species, selectedPlant.placement).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, fontSize = 13.sp, color = muted)

            PlantupiaPanel {
                Text("سمّيه بالاسم اللي تحبه", color = forestText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = plantName,
                    onValueChange = { plantName = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم النبات") },
                    singleLine = true,
                    enabled = !saving,
                )
                Button(
                    enabled = !saving && plantName.trim().isNotBlank(),
                    onClick = {
                        scope.launch {
                            saving = true
                            when (val result = repository.nickname(selectedPlant.id, plantName)) {
                                is ApiResult.Failure -> error = result.message
                                is ApiResult.Success -> {
                                    plants = plants.map {
                                        if (it.id == selectedPlant.id) it.copy(name = plantName.trim()) else it
                                    }
                                    error = null
                                }
                            }
                            saving = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                ) { Text("احفظ الاسم", color = white) }
            }

            BrandEyebrow("CARE & OBSERVATIONS")
            if (selectedPlant.canRecord) {
            PlantupiaPanel {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(1500) },
                    label = { Text("إيه اللي لاحظته أو عملته؟") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !saving,
                )
                OutlinedButton(onClick = { carePhotoPicker.launch("image/*") }) {
                    Text(if (carePhotoUri == null) "أضف صورة للملاحظة" else "الصورة جاهزة — غيّرها لو حابب", color = deepGreen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !saving && note.trim().isNotEmpty(),
                        onClick = {
                            scope.launch {
                                saving = true
                                when (val r = carePhotoUri?.let { repository.recordWithPhoto(selectedPlant.id, "observation", note, it) } ?: repository.record(selectedPlant.id, "observation", note)) {
                                    is ApiResult.Failure -> error = r.message
                                    is ApiResult.Success -> { note = ""; openPlant(selectedPlant) }
                                }
                                saving = false
                            }
                        },
                    ) { Text("ملاحظة", color = deepGreen) }
                    Button(
                        enabled = !saving && note.trim().isNotEmpty(),
                        onClick = {
                            scope.launch {
                                saving = true
                                when (val r = carePhotoUri?.let { repository.recordWithPhoto(selectedPlant.id, "care", note, it) } ?: repository.record(selectedPlant.id, "care", note)) {
                                    is ApiResult.Failure -> error = r.message
                                    is ApiResult.Success -> { note = ""; openPlant(selectedPlant) }
                                }
                                saving = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                    ) { Text("رعاية", color = white) }
                }
            }
            } else {
                Text("لديك صلاحية مشاهدة سجل الرعاية فقط. يمكن للمسؤول منحك صلاحية التوثيق.", color = muted, fontSize = 12.sp)
            }
            if (error != null) Text(error.orEmpty(), color = Color(0xFF9A4A31), fontSize = 12.sp)
            if (detailLoading) Text("جارٍ تحميل التاريخ…", color = muted, fontSize = 13.sp)
            else if (events.isEmpty()) Text("أول ملاحظة منك هتبدأ تاريخ النبات هنا.", color = muted, fontSize = 13.sp)
            events.forEach { event ->
                PlantupiaPanel {
                    Text("${event.kind.uppercase()}  ·  ${event.observedAt.take(10)}", color = deepGreen, fontSize = 11.sp, letterSpacing = 1.sp)
                    if (event.photoUrl.isNotBlank()) {
                        AsyncImage(model = event.photoUrl, contentDescription = "صورة من تاريخ النبات", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    }
                    Text(event.note.ifBlank { "حدث رعاية مسجل" }, color = forestText, fontSize = 13.sp, lineHeight = 20.sp)
                    Text("Source: ${event.evidence}", color = muted, fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = sage)
            PlantupiaQuestionsPanel(
                plantId = selectedPlant.id,
                repository = repository,
                canPublish = selectedPlant.canRecord,
                onOpenPost = onOpenLinkedPost,
                onOutcomeSaved = {
                    when (val response = repository.history(selectedPlant.id)) {
                        is ApiResult.Success -> { events = response.value; error = null }
                        is ApiResult.Failure -> error = response.message
                    }
                },
            )
            HorizontalDivider(color = sage)
            Text("أو شارك لحظة عامة مع الناس", color = forestText, fontSize = 14.sp)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCommunityCreateClick,
            ) {
                Text("منشور عام مستقل", color = deepGreen)
            }
            Text(
                "اللحظات العامة والستوريز من Nova متاحة؛ أسئلة النبات المنشورة أعلاه مرتبطة بتاريخ النبات فقط بعد موافقتك.",
                color = muted,
                fontSize = 11.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun BrandEyebrow(text: String) {
    Text(text, color = deepGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
}

@Composable
private fun PlantupiaPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = white,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = content,
        )
    }
}
