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
    var openDetailMode by remember { mutableStateOf<PlantupiaCreateMode?>(null) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showCareEditor by remember { mutableStateOf(false) }
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
        openDetailMode = PlantupiaCreateIntent.pending
        showNameEditor = false
        showCareEditor = openDetailMode == PlantupiaCreateMode.OBSERVATION
        PlantupiaCreateIntent.clear()
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
    LaunchedEffect(PlantupiaCreateIntent.pending, plants, loading) {
        if (!loading && PlantupiaCreateIntent.pending != null) {
            val available = plants.filter { it.active }
            if (available.size == 1) openPlant(available.first())
        }
    }
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
            if (PlantupiaCreateIntent.pending != null) {
                PlantupiaPanel {
                    Text(
                        if (PlantupiaCreateIntent.pending == PlantupiaCreateMode.QUESTION)
                            "اختار النبات اللي عايز تسأل الناس عنه"
                        else "اختار النبات اللي عايز توثّق لحظة من حياته",
                        color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    )
                    Text(
                        "اللحظات الخاصة بتفضل في سجل النبات. النشر العام محتاج موافقتك.",
                        color = muted, fontSize = 12.sp,
                    )
                    TextButton(onClick = { PlantupiaCreateIntent.clear() }) {
                        Text("إلغاء", color = deepGreen)
                    }
                }
            }
            BrandEyebrow("YOUR LIVING SPACE / مساحتك")
            Text(
                "المكان بيتغيّر مع الوقت.",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = deepGreen,
                lineHeight = 33.sp,
            )
            Text(
                "لكل نبات مكان وحكاية ورعاية حقيقية. هنا بنحتفظ باللي حصل، مش بنخمن صحته.",
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
                if (spaces.isNotEmpty()) {
                    val featured = spaces.first()
                    val featuredCount = plants.count { it.spaceId == featured.id && it.active }
                    Surface(
                        color = Color(0xFFD6E2D4),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BrandEyebrow("YOUR SPACE / مساحة حيّة")
                            Text(featured.name, color = deepGreen, fontFamily = FontFamily.Serif, fontSize = 27.sp)
                            if (featured.location.isNotBlank()) {
                                Text(featured.location, color = muted, fontSize = 12.sp)
                            }
                            Text("$featuredCount وحدة نباتية · السجل بيتكوّن من ملاحظاتكم الحقيقية", color = muted, fontSize = 12.sp)
                            TextButton(onClick = { expandedSpaceId = featured.id }) {
                                Text("اكتشف نباتات المساحة ←", color = deepGreen)
                            }
                        }
                    }

                    val recent = plants
                        .filter { it.active && it.lastEventAt.isNotBlank() }
                        .sortedByDescending { it.lastEventAt }
                    if (recent.isNotEmpty()) {
                        val latest = recent.first()
                        BrandEyebrow("LATEST OBSERVATION / آخر تغيير موثّق")
                        PlantupiaPanel {
                            Text(latest.name, color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "${latest.lastEventAt.take(10)} · ${eventLabel(latest.lastEventKind)}",
                                color = muted, fontSize = 12.sp,
                            )
                            TextButton(onClick = { scope.launch { openPlant(latest) } }) {
                                Text("شوف تاريخها ←", color = deepGreen)
                            }
                        }
                        BrandEyebrow("RECENT MOMENTS / لحظات موثّقة")
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            recent.take(4).forEach { plant ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { scope.launch { openPlant(plant) } },
                                    shape = RoundedCornerShape(12.dp),
                                    color = white,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(7.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        if (plant.coverPhoto.isNotBlank()) {
                                            AsyncImage(
                                                model = plant.coverPhoto,
                                                contentDescription = plant.name,
                                                modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(7.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                        Text(plant.name, color = forestText, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "أول صورة أو ملاحظة هتبدأ حكاية المكان هنا، من غير بيانات وهمية.",
                            color = muted, fontSize = 12.sp,
                        )
                    }
                    BrandEyebrow("YOUR PLANTS / نباتاتك")
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
                                    Text(
                                        "آخر سجل: ${plant.lastEventAt.take(10)} · ${eventLabel(plant.lastEventKind)}",
                                        color = muted, fontSize = 12.sp,
                                    )
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
            TextButton(onClick = { currentPlantId = null; scope.launch { refresh() } }) {
                Text("← الرجوع للمساحة", color = deepGreen)
            }
            BrandEyebrow("LIVING RECORD / تاريخ النبات")
            if (openDetailMode != null) {
                BrandEyebrow(
                    if (openDetailMode == PlantupiaCreateMode.QUESTION)
                        "ASK ABOUT THIS PLANT / اسأل عن نباتك"
                    else "RECORD A MOMENT / لحظة جديدة"
                )
                Text(
                    if (openDetailMode == PlantupiaCreateMode.QUESTION)
                        "اسأل عن النبات ده من غير ما تشارك سجله الخاص."
                    else "سجّل اللي لاحظته أو عملته؛ اللحظة هتفضل في تاريخه.",
                    color = muted, fontSize = 13.sp,
                )
            }
            if (selectedPlant.coverPhoto.isNotBlank()) {
                AsyncImage(model = selectedPlant.coverPhoto, contentDescription = "صورة النبات", modifier = Modifier.fillMaxWidth().height(236.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            }
            Text(selectedPlant.name, fontFamily = FontFamily.Serif, fontSize = 35.sp, color = deepGreen)
            val meta = listOf(selectedPlant.species, selectedPlant.placement).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, fontSize = 13.sp, color = muted)

            if (openDetailMode == PlantupiaCreateMode.QUESTION) {
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
            }

            PlantupiaPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("الاسم اللي اخترته", color = muted, fontSize = 11.sp)
                        Text(selectedPlant.name, color = deepGreen, fontWeight = FontWeight.Medium, fontSize = 19.sp)
                    }
                    TextButton(onClick = { showNameEditor = !showNameEditor }) {
                        Text(if (showNameEditor) "إلغاء" else "غيّر الاسم", color = deepGreen)
                    }
                }
                if (showNameEditor) {
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
                                    showNameEditor = false
                                }
                            }
                            saving = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                ) { Text("احفظ الاسم", color = white) }
                }
            }

            BrandEyebrow("CARE & OBSERVATIONS / الرعاية والملاحظة")
            if (selectedPlant.canRecord) {
                Button(
                    onClick = { showCareEditor = !showCareEditor },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                ) {
                    Text(if (showCareEditor) "اخفِ تسجيل اللحظة ↑" else "+ سجّل لحظة جديدة للنبات", color = white)
                }
            if (showCareEditor) PlantupiaPanel {
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
            BrandEyebrow("LIVING TIME / من أول صورة لآخر تغيير")
            if (detailLoading) {
                Text("جارٍ تحميل التاريخ…", color = muted, fontSize = 13.sp)
            } else if (events.isEmpty()) {
                Text("أول ملاحظة منك هتبدأ تاريخ النبات هنا.", color = muted, fontSize = 13.sp)
            } else {
                Text("${events.size} لحظات موثّقة · من الواقع، مش تقييم صحة تلقائي", color = muted, fontSize = 12.sp)
                val photos = events
                    .filter { it.photoUrl.isNotBlank() }
                    .sortedBy { it.observedAt }
                if (photos.size >= 2) {
                    // Compare actual dated photographs, without asserting
                    // growth, recovery or a causal improvement from appearance.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(photos.first(), photos.last()).forEachIndexed { index, event ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = white,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    AsyncImage(
                                        model = event.photoUrl,
                                        contentDescription = if (index == 0) "أقدم صورة مسجلة" else "أحدث صورة مسجلة",
                                        modifier = Modifier.fillMaxWidth().height(135.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Text(
                                        if (index == 0) "أقدم صورة · ${event.observedAt.take(10)}"
                                        else "أحدث صورة · ${event.observedAt.take(10)}",
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        color = muted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            events.sortedByDescending { it.observedAt }.forEach { event ->
                PlantupiaPanel {
                    Text(
                        "${eventLabel(event.kind)} · ${event.observedAt.take(10)}",
                        color = deepGreen, fontSize = 12.sp,
                    )
                    if (event.photoUrl.isNotBlank()) {
                        AsyncImage(model = event.photoUrl, contentDescription = "صورة من تاريخ النبات", modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    }
                    Text(event.note.ifBlank { "حدث رعاية مسجل" }, color = forestText, fontSize = 13.sp, lineHeight = 20.sp)
                    Text("مصدر المعلومة: ${evidenceLabel(event.evidence)}", color = muted, fontSize = 11.sp)
                }
            }
            HorizontalDivider(color = sage)
            if (openDetailMode != PlantupiaCreateMode.QUESTION) PlantupiaQuestionsPanel(
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

private fun eventLabel(kind: String): String = when (kind) {
    "care" -> "رعاية مسجلة"
    "issue" -> "ملاحظة تحتاج متابعة"
    "outcome" -> "نتيجة موثقة"
    else -> "ملاحظة مسجلة"
}

private fun evidenceLabel(kind: String): String = when (kind) {
    "measured" -> "قياس"
    "reported" -> "نقلًا عن شخص آخر"
    "inferred" -> "استنتاج غير مؤكّد"
    else -> "ملاحظة مباشرة"
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
