package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
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
import com.nova.app.feature.plants.data.PlantupiaCareAction
import com.nova.app.feature.plants.data.buildPlantupiaCareNote
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import com.nova.app.feature.plants.data.findSpacePlants
import kotlinx.coroutines.launch
import java.time.LocalDate

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
fun PlantupiaClientSection(
    onCommunityCreateClick: () -> Unit,
    onOpenLinkedPost: (Long) -> Unit,
    onOpenPlant: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var spaces by remember { mutableStateOf<List<PlantupiaSpace>>(emptyList()) }
    var plants by remember { mutableStateOf<List<PlantupiaPlant>>(emptyList()) }
    var currentPlantId by remember { mutableStateOf<String?>(null) }
    var openDetailMode by remember { mutableStateOf<PlantupiaCreateMode?>(null) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showCareEditor by remember { mutableStateOf(false) }
    var showAssistant by remember { mutableStateOf(false) }
    var photoToView by remember { mutableStateOf<Pair<String, String>?>(null) }
    var expandedSpaceId by remember { mutableStateOf<String?>(null) }
    var plantSearch by remember { mutableStateOf("") }
    var visiblePlantCount by remember { mutableStateOf(8) }
    var events by remember { mutableStateOf<List<PlantupiaCareEvent>>(emptyList()) }
    var pendingFollowUps by remember { mutableStateOf<List<PlantupiaCareEvent>>(emptyList()) }
    var followUpTarget by remember { mutableStateOf<PlantupiaCareEvent?>(null) }
    var followUpResult by remember { mutableStateOf("") }
    var checkAgainInDays by remember { mutableStateOf<Int?>(null) }
    var nextHistoryOffset by remember { mutableStateOf<Int?>(null) }
    var olderEventsLoading by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var detailLoading by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plantName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCareAction by remember { mutableStateOf<PlantupiaCareAction?>(null) }
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
        onOpenPlant()
        plantName = plant.name
        note = ""
        selectedCareAction = null
        carePhotoUri = null
        checkAgainInDays = null
        followUpTarget = null
        followUpResult = ""
        pendingFollowUps = emptyList()
        detailLoading = true
        error = null
        nextHistoryOffset = null
        when (val response = repository.historyPage(plant.id)) {
            is ApiResult.Success -> {
                events = response.value.events
                nextHistoryOffset = response.value.nextOffset
            }
            is ApiResult.Failure -> { events = emptyList(); error = response.message }
        }
        when (val response = repository.historyPage(plant.id, pendingOnly = true)) {
            is ApiResult.Success -> pendingFollowUps = response.value.events
            is ApiResult.Failure -> if (error == null) error = response.message
        }
        detailLoading = false
    }

    fun closePlant() {
        showAssistant = false
        currentPlantId = null
        openDetailMode = null
        error = null
        scope.launch { refresh() }
    }

    BackHandler(enabled = selectedPlant != null) {
        // The first Android Back returns to the customer's living space,
        // instead of unexpectedly leaving the existing Nova tab.
        closePlant()
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
    LaunchedEffect(expandedSpaceId, plantSearch) {
        // Do not compose 100+ client units at once just because a site expands.
        visiblePlantCount = 8
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
            OutlinedButton(
                onClick = { showAssistant = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
            ) {
                com.nova.app.ui.icons.NovaIcon(
                    asset = com.nova.app.ui.icons.NovaIconAsset.Info,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 10.dp),
                    tint = deepGreen,
                )
                Text("اسأل مساعد Plantupia عن النباتات", color = deepGreen)
            }
            PlantupiaSelfServiceForms(
                spaces = spaces,
                repository = repository,
                onCreated = { scope.launch { refresh() } },
            )
            if (loading) {
                Text("جارٍ تحميل مساحتك…", color = muted, fontSize = 13.sp)
            } else {
                if (spaces.isEmpty() && error == null) {
                    PlantupiaPanel {
                        Text("لسه ما عندكش مساحات.", color = forestText, fontWeight = FontWeight.Medium)
                        Text("ابدأ بمساحة صغيرة، وبعدها أضف أول نبات وصورته وسجّل حكايته.", color = muted, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
                if (spaces.isNotEmpty()) {
                    val featured = spaces.first()
                    val featuredCount = plants.count { it.spaceId == featured.id && it.active }
                    val heroPhoto = plants.firstOrNull {
                        it.spaceId == featured.id && it.active && it.coverPhoto.isNotBlank()
                    }?.coverPhoto
                    val photoHero = heroPhoto != null
                    val heroInk = if (photoHero) white else deepGreen
                    val heroMuted = if (photoHero) Color(0xFFE0E7DD) else muted
                    Surface(
                        color = Color(0xFFD6E2D4),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (photoHero) 244.dp else 202.dp),
                        ) {
                            if (photoHero) {
                                AsyncImage(
                                    model = heroPhoto,
                                    contentDescription = "صورة حقيقية لنبات ضمن مساحة ${featured.name}",
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0f to Color(0x22112317),
                                                0.35f to Color(0x66112317),
                                                1f to Color(0xE6112317),
                                            )
                                        )
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(19.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    "LIVING SPACE / مساحة حيّة",
                                    color = heroInk,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                )
                                Text(
                                    featured.name,
                                    color = heroInk,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 27.sp,
                                )
                                if (featured.location.isNotBlank()) {
                                    Text(featured.location, color = heroMuted, fontSize = 12.sp)
                                }
                                Text(
                                    "$featuredCount وحدة نباتية · ملاحظات حقيقية عبر الوقت",
                                    color = heroMuted,
                                    fontSize = 12.sp,
                                )
                                TextButton(onClick = { expandedSpaceId = featured.id }) {
                                    Text("اكتشف نباتات المساحة ←", color = heroInk)
                                }
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
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(recent.take(12).size) { index ->
                                val plant = recent[index]
                                Surface(
                                    onClick = { scope.launch { openPlant(plant) } },
                                    modifier = Modifier.width(158.dp),
                                    color = white,
                                    shape = RoundedCornerShape(15.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(7.dp),
                                    ) {
                                        if (plant.coverPhoto.isNotBlank()) {
                                            AsyncImage(
                                                model = plant.coverPhoto,
                                                contentDescription = "صورة " + plant.name,
                                                modifier = Modifier.fillMaxWidth()
                                                    .height(106.dp)
                                                    .clip(RoundedCornerShape(10.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .height(106.dp)
                                                    .background(sage.copy(alpha = 0.23f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                PlantupiaBrandGlyph(modifier = Modifier.height(46.dp).width(46.dp))
                                            }
                                        }
                                        Text(
                                            plant.name, color = forestText,
                                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                        )
                                        Text(
                                            eventLabel(plant.lastEventKind) + " · " + plant.lastEventAt.take(10),
                                            color = muted, fontSize = 10.sp, maxLines = 1,
                                        )
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
                    val checkIns = plants
                        .filter { it.active && it.nextFollowUpOn.isNotBlank() }
                        .sortedBy { it.nextFollowUpOn }
                    if (checkIns.isNotEmpty()) {
                        BrandEyebrow("CHECK-INS / مواعيد المتابعة اللي اخترتها")
                        PlantupiaPanel {
                            Text("دي مواعيد مراجعة أنت اخترتها بعد ملاحظة أو رعاية، مش تشخيص ولا ميعاد ري تلقائي.", color = muted, fontSize = 12.sp)
                            checkIns.take(4).forEach { item ->
                                val due = runCatching {
                                    !LocalDate.parse(item.nextFollowUpOn).isAfter(LocalDate.now())
                                }.getOrDefault(false)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(if (due) "وقت المراجعة · ${item.nextFollowUpOn}" else "راجعه يوم ${item.nextFollowUpOn}", color = muted, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { scope.launch { openPlant(item) } }) {
                                        Text("افتح النبات ←", color = deepGreen, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
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
                    val sitePlants = plants.filter { it.spaceId == space.id && it.active }
                    if (sitePlants.size > 8) {
                        OutlinedTextField(
                            value = plantSearch,
                            onValueChange = { plantSearch = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("دوّر باسم النبات أو نوعه أو مكانه") },
                            singleLine = true,
                        )
                    }
                    val matchingPlants = findSpacePlants(plants, space.id, plantSearch)
                    if (matchingPlants.isEmpty() && sitePlants.isNotEmpty()) {
                        Text("مفيش نباتات مطابقة للبحث. جرّب اسم تاني.", color = muted, fontSize = 12.sp)
                    }
                    matchingPlants.take(visiblePlantCount).forEach { plant ->
                        PlantupiaPlantInventoryCard(
                            plant = plant,
                            eventLabel = eventLabel(plant.lastEventKind),
                            onClick = { scope.launch { openPlant(plant) } },
                        )
                    }
                    if (matchingPlants.size > visiblePlantCount) {
                        TextButton(onClick = { visiblePlantCount += 8 }) {
                            Text(
                                "عرض نباتات أكثر · ${visiblePlantCount} من ${matchingPlants.size}",
                                color = deepGreen,
                            )
                        }
                    }
                    }
                }
                if (spaces.isNotEmpty() && plants.none { it.active }) {
                    Text("مساحتك جاهزة! أضف أول نبات من الزر اللي فوق.", color = muted, fontSize = 12.sp)
                }
            }
            if (error != null) Text(error.orEmpty(), color = Color(0xFF9A4A31), fontSize = 12.sp)
            TextButton(onClick = { scope.launch { refresh() } }) {
                Text("تحديث البيانات", color = deepGreen)
            }
        } else {
            TextButton(onClick = { closePlant() }) {
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
                AsyncImage(
                    model = selectedPlant.coverPhoto,
                    contentDescription = "الصورة الفعلية للنبات، اضغط للتكبير",
                    modifier = Modifier.fillMaxWidth()
                        .height(236.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { photoToView = selectedPlant.coverPhoto to selectedPlant.name },
                    contentScale = ContentScale.Crop,
                )
            }
            Text(selectedPlant.name, fontFamily = FontFamily.Serif, fontSize = 35.sp, color = deepGreen)
            val meta = listOf(selectedPlant.species, selectedPlant.placement).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, fontSize = 13.sp, color = muted)

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showAssistant = true },
                shape = RoundedCornerShape(15.dp),
            ) {
                com.nova.app.ui.icons.NovaIcon(
                    asset = com.nova.app.ui.icons.NovaIconAsset.Info,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = deepGreen,
                )
                Text("اسأل مساعد Plantupia عن النبات", color = deepGreen)
            }
            if (openDetailMode == PlantupiaCreateMode.QUESTION) {
                PlantupiaQuestionsPanel(
                    plantId = selectedPlant.id,
                    repository = repository,
                    canPublish = selectedPlant.canRecord,
                    onOpenPost = onOpenLinkedPost,
                    onOutcomeSaved = {
                        when (val response = repository.historyPage(selectedPlant.id)) {
                            is ApiResult.Success -> {
                                events = response.value.events
                                nextHistoryOffset = response.value.nextOffset
                                error = null
                            }
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

            BrandEyebrow("CHECK BACK / راجع اللي حصل")
            if (detailLoading) {
                Text("جارٍ تحميل مواعيد متابعتك…", color = muted, fontSize = 12.sp)
            } else if (pendingFollowUps.isEmpty()) {
                Text("لما تسجّل رعاية أو تغيّر، تقدر تختار بنفسك إمتى ترجع تراجع النتيجة.", color = muted, fontSize = 12.sp)
            } else {
                pendingFollowUps.forEach { check ->
                    PlantupiaPanel {
                        Text(
                            "متابعة ${check.followUpOn} · ${eventLabel(check.kind)}",
                            color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        )
                        if (check.note.isNotBlank()) Text(check.note, color = muted, fontSize = 12.sp)
                        Text("راجع اللي حصل فعلًا للنبات قبل ما تسجّل نتيجة. الميعاد مش أمر بالري.", color = muted, fontSize = 11.sp)
                        if (selectedPlant.canRecord) {
                            OutlinedButton(onClick = {
                                followUpTarget = if (followUpTarget?.id == check.id) null else check
                                followUpResult = ""
                            }) {
                                Text(if (followUpTarget?.id == check.id) "إلغاء" else "سجّل نتيجة المراجعة", color = deepGreen)
                            }
                        }
                    }
                }
            }
            if (followUpTarget != null && selectedPlant.canRecord) {
                PlantupiaPanel {
                    Text("إيه اللي لاحظته لما رجعت للنبات؟", color = deepGreen, fontSize = 15.sp)
                    OutlinedTextField(
                        value = followUpResult,
                        onValueChange = { followUpResult = it.take(1500) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("النتيجة اللي شفتها فعلًا *") },
                        enabled = !saving,
                    )
                    Button(
                        enabled = !saving && followUpResult.isNotBlank(),
                        onClick = {
                            val source = followUpTarget ?: return@Button
                            scope.launch {
                                saving = true
                                when (val response = repository.record(
                                    selectedPlant.id, "outcome", followUpResult,
                                    followUpFor = source.id,
                                )) {
                                    is ApiResult.Failure -> error = response.message
                                    is ApiResult.Success -> openPlant(selectedPlant)
                                }
                                saving = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                    ) { Text(if (saving) "جارٍ تسجيل النتيجة…" else "احفظ النتيجة وأغلق المتابعة", color = white) }
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
                Text("لو عملت رعاية، اختار اللي حصل فعلًا:", color = forestText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                PlantupiaCareAction.entries.chunked(3).forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        group.forEach { action ->
                            PlantupiaCareActionTile(
                                action = action,
                                selected = selectedCareAction == action,
                                enabled = !saving,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedCareAction =
                                        if (selectedCareAction == action) null else action
                                },
                            )
                        }
                    }
                }
                Text("الأفعال دي للتوثيق فقط، مش توصيات بمواعيد الري.", color = muted, fontSize = 11.sp)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(1500) },
                    label = { Text("إيه اللي لاحظته أو عملته؟") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    enabled = !saving,
                )
                OutlinedButton(
                    onClick = { carePhotoPicker.launch("image/*") },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    com.nova.app.ui.icons.NovaIcon(
                        asset = com.nova.app.ui.icons.NovaIconAsset.Photo,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 9.dp),
                        tint = deepGreen,
                    )
                    Text(
                        if (carePhotoUri == null) "أضف صورة حقيقية للملاحظة" else "غيّر الصورة المختارة",
                        color = deepGreen,
                    )
                }
                carePhotoUri?.let { selectedPhoto ->
                    AsyncImage(
                        model = selectedPhoto,
                        contentDescription = "معاينة الصورة الفعلية قبل حفظها في سجل النبات",
                        modifier = Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    TextButton(
                        onClick = { carePhotoUri = null },
                        enabled = !saving,
                    ) {
                        Text("إزالة الصورة المختارة", color = muted)
                    }
                }
                Text("تحب ترجع تراجع نتيجة اللي حصل إمتى؟", color = forestText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(7, 14).forEach { days ->
                        OutlinedButton(
                            enabled = !saving,
                            onClick = { checkAgainInDays = if (checkAgainInDays == days) null else days },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (checkAgainInDays == days) "✓ بعد $days يوم" else "بعد $days يوم", color = deepGreen, fontSize = 12.sp) }
                    }
                }
                if (checkAgainInDays != null) {
                    TextButton(onClick = { checkAgainInDays = null }, enabled = !saving) {
                        Text("بدون موعد متابعة", color = muted, fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !saving && (note.isNotBlank() || carePhotoUri != null),
                        onClick = {
                            scope.launch {
                                saving = true
                                when (val r = carePhotoUri?.let { repository.recordWithPhoto(selectedPlant.id, "observation", note, it, followUpOn = checkAgainInDays?.let { days -> LocalDate.now().plusDays(days.toLong()).toString() }) } ?: repository.record(selectedPlant.id, "observation", note, followUpOn = checkAgainInDays?.let { days -> LocalDate.now().plusDays(days.toLong()).toString() })) {
                                    is ApiResult.Failure -> error = r.message
                                    is ApiResult.Success -> { note = ""; openPlant(selectedPlant) }
                                }
                                saving = false
                            }
                        },
                    ) { Text("ملاحظة", color = deepGreen) }
                    Button(
                        enabled = !saving && (selectedCareAction != null || note.isNotBlank() || carePhotoUri != null),
                        onClick = {
                            val careDetails = buildPlantupiaCareNote(selectedCareAction, note)
                            scope.launch {
                                saving = true
                                when (val r = carePhotoUri?.let { repository.recordWithPhoto(selectedPlant.id, "care", careDetails, it, followUpOn = checkAgainInDays?.let { days -> LocalDate.now().plusDays(days.toLong()).toString() }) } ?: repository.record(selectedPlant.id, "care", careDetails, followUpOn = checkAgainInDays?.let { days -> LocalDate.now().plusDays(days.toLong()).toString() })) {
                                    is ApiResult.Failure -> error = r.message
                                    is ApiResult.Success -> { note = ""; openPlant(selectedPlant) }
                                }
                                saving = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                    ) { Text("رعاية", color = white) }
                    OutlinedButton(
                        enabled = !saving && (note.isNotBlank() || carePhotoUri != null),
                        onClick = {
                            scope.launch {
                                saving = true
                                when (val result = carePhotoUri?.let {
                                    repository.recordWithPhoto(selectedPlant.id, "issue", note, it, followUpOn = checkAgainInDays?.let { days -> LocalDate.now().plusDays(days.toLong()).toString() })
                                } ?: repository.record(selectedPlant.id, "issue", note, followUpOn = checkAgainInDays?.let { days -> LocalDate.now().plusDays(days.toLong()).toString() })) {
                                    is ApiResult.Failure -> error = result.message
                                    is ApiResult.Success -> {
                                        note = ""
                                        openPlant(selectedPlant)
                                    }
                                }
                                saving = false
                            }
                        },
                    ) { Text("تغيّر مقلق", color = deepGreen) }
                }
                Text(
                    "سجل اللي شفته زي ما هو. مش لازم تشخّص سبب التغيّر.",
                    color = muted, fontSize = 11.sp,
                )
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
                Text("${events.size} لحظات معروضة · من ملاحظات حقيقية، مش تقييم صحة تلقائي", color = muted, fontSize = 12.sp)
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
                                        if (index == 0) "أقدم صورة معروضة · ${event.observedAt.take(10)}"
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
                PlantupiaHistoryEventCard(
                    event = event,
                    title = eventLabel(event.kind),
                    evidenceLabel = evidenceLabel(event.evidence),
                )
            }
            if (nextHistoryOffset != null) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !olderEventsLoading,
                    onClick = {
                        val offset = nextHistoryOffset ?: return@OutlinedButton
                        scope.launch {
                            olderEventsLoading = true
                            when (val result = repository.historyPage(selectedPlant.id, offset)) {
                                is ApiResult.Success -> {
                                    events = (events + result.value.events).distinctBy { it.id }
                                    nextHistoryOffset = result.value.nextOffset
                                    error = null
                                }
                                is ApiResult.Failure -> error = result.message
                            }
                            olderEventsLoading = false
                        }
                    },
                ) {
                    Text(
                        if (olderEventsLoading) "جارٍ تحميل لحظات أقدم…" else "شوف لحظات أقدم ←",
                        color = deepGreen,
                    )
                }
            }
            HorizontalDivider(color = sage)
            if (openDetailMode != PlantupiaCreateMode.QUESTION) PlantupiaQuestionsPanel(
                plantId = selectedPlant.id,
                repository = repository,
                canPublish = selectedPlant.canRecord,
                onOpenPost = onOpenLinkedPost,
                onOutcomeSaved = {
                    when (val response = repository.historyPage(selectedPlant.id)) {
                            is ApiResult.Success -> {
                                events = response.value.events
                                nextHistoryOffset = response.value.nextOffset
                                error = null
                            }
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
    photoToView?.let { (url, plantName) ->
        PlantupiaPhotoViewer(
            photoUrl = url,
            plantName = plantName,
            onClose = { photoToView = null },
        )
    }
    if (showAssistant) {
        PlantupiaAssistantSheet(
            plant = selectedPlant,
            repository = repository,
            onDismiss = { showAssistant = false },
        )
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
