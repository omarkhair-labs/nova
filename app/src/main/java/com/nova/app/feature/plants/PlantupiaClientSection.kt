package com.nova.app.feature.plants

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import androidx.core.content.ContextCompat
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
import com.nova.app.feature.plants.data.findArchivedPlants
import com.nova.app.feature.plants.data.resolveFocusedSpace
import com.nova.app.feature.plants.data.isCurrentPlantDetailRequest
import com.nova.app.feature.plants.data.spacePlantCountSummary
import com.nova.app.feature.plants.data.spaceHeroMediaEyebrow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

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
    accountId: Long,
    onCommunityCreateClick: () -> Unit,
    onOpenLinkedPost: (Long, () -> Boolean) -> Unit,
    onOpenPlant: () -> Unit = {},
) {
    key(accountId) {
        PlantupiaAccountClientSection(accountId, onCommunityCreateClick, onOpenLinkedPost, onOpenPlant)
    }
}

@Composable
private fun PlantupiaAccountClientSection(
    accountId: Long,
    onCommunityCreateClick: () -> Unit,
    onOpenLinkedPost: (Long, () -> Boolean) -> Unit,
    onOpenPlant: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val lifetime = remember(repository) { PlantupiaCallbackLifetime(repository::isCurrentSession) }
    DisposableEffect(lifetime) { onDispose { lifetime.dispose() } }
    var spaces by remember { mutableStateOf<List<PlantupiaSpace>>(emptyList()) }
    var plants by remember { mutableStateOf<List<PlantupiaPlant>>(emptyList()) }
    var currentPlantId by remember { mutableStateOf<String?>(null) }
    // Invalidate slower in-flight history calls when a user opens another plant or goes Back.
    var detailRequestGeneration by remember { mutableIntStateOf(0) }
    var openDetailMode by remember { mutableStateOf<PlantupiaCreateMode?>(null) }
    var showNameEditor by remember { mutableStateOf(false) }
    var showSharedDetailsEditor by remember { mutableStateOf(false) }
    var sharedPlantLabel by remember { mutableStateOf("") }
    var sharedSpecies by remember { mutableStateOf("") }
    var sharedPlacement by remember { mutableStateOf("") }
    var showCareEditor by remember { mutableStateOf(false) }
    var creationAccessNotice by remember { mutableStateOf<String?>(null) }
    var showPlantManagement by remember(currentPlantId) { mutableStateOf(false) }
    var showPlantSharing by remember(currentPlantId) { mutableStateOf(false) }
    var showAssistant by remember { mutableStateOf(false) }
    var photoToView by remember { mutableStateOf<Pair<String, String>?>(null) }
    var focusedSpaceId by rememberSaveable { mutableStateOf<String?>(null) }
    var plantSearch by remember { mutableStateOf("") }
    var archiveSearch by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var visibleArchivedCount by remember { mutableStateOf(8) }
    var pendingArchiveAction by remember { mutableStateOf<Boolean?>(null) }
    var visiblePlantCount by remember { mutableStateOf(8) }
    var events by remember { mutableStateOf<List<PlantupiaCareEvent>>(emptyList()) }
    var historyFilter by remember(currentPlantId) { mutableStateOf(PlantupiaHistoryFilter.ALL) }
    var pendingState by remember { mutableStateOf<PlantupiaPendingCheckInsState>(PlantupiaPendingCheckInsState.Loading) }
    val pendingFollowUps = (pendingState as? PlantupiaPendingCheckInsState.Loaded)?.events.orEmpty()
    val nextPendingOffset = (pendingState as? PlantupiaPendingCheckInsState.Loaded)?.nextOffset
    var pendingPageError by remember { mutableStateOf<String?>(null) }
    var pendingPageLoading by remember { mutableStateOf(false) }
    var followUpTarget by remember { mutableStateOf<PlantupiaCareEvent?>(null) }
    var followUpResult by remember { mutableStateOf("") }
    var movingCheckIn by remember { mutableStateOf<PlantupiaCareEvent?>(null) }
    var movedCheckInDate by remember { mutableStateOf<LocalDate?>(null) }
    var checkAgainInDays by remember { mutableStateOf<Int?>(null) }
    var customFollowUpDate by remember { mutableStateOf<LocalDate?>(null) }
    var reminderOptIn by remember { mutableStateOf(false) }
    var reminderRevision by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var requestedExistingReminder by remember { mutableStateOf<PlantupiaCareEvent?>(null) }
    var reminderPermissionGeneration by remember { mutableStateOf<Int?>(null) }
    val selectedPlant = plants.firstOrNull { it.id == currentPlantId }
    val reminderPermissionPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val permissionGeneration = reminderPermissionGeneration
        reminderPermissionGeneration = null
        if (!lifetime.isCurrent() || permissionGeneration != detailRequestGeneration) return@rememberLauncherForActivityResult
        val existing = requestedExistingReminder
        requestedExistingReminder = null
        if (existing == null) {
            reminderOptIn = granted
        } else {
            val scheduled = granted && PlantupiaCareReminderScheduler.schedule(
                context, existing.id, existing.followUpOn, selectedPlant?.id.orEmpty(),
            )
            reminderRevision += 1
            error = if (scheduled) null else "The check-in reminder couldn't be enabled. Check notification permission and the review date."
        }
    }
    var nextHistoryOffset by remember { mutableStateOf<Int?>(null) }
    var olderEventsLoading by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var detailLoading by remember { mutableStateOf(false) }
    var historyRetryLoading by remember { mutableStateOf(false) }
    var historyLoadFailed by remember(currentPlantId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var plantName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCareAction by remember { mutableStateOf<PlantupiaCareAction?>(null) }
    var pendingPrivateSaves by remember { mutableStateOf<List<com.nova.app.feature.plants.data.PlantupiaSubmission>>(emptyList()) }
    var carePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var carePhotoRequestGeneration by remember { mutableStateOf<Int?>(null) }
    val carePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (lifetime.isCurrent() && carePhotoRequestGeneration == detailRequestGeneration) carePhotoUri = uri
        carePhotoRequestGeneration = null
    }

    fun isCurrentRequest(plantId: String, generation: Int): Boolean =
        lifetime.isCurrent() && isCurrentPlantDetailRequest(
            plantId, generation, currentPlantId, detailRequestGeneration,
        )

    suspend fun refresh(acceptResult: () -> Boolean = { true }) {
        if (!lifetime.isCurrent() || !acceptResult()) return
        loading = true
        error = null
        val spaceResult = repository.spaces()
        if (!lifetime.isCurrent() || !acceptResult()) return
        val plantResult = repository.plants()
        if (!lifetime.isCurrent() || !acceptResult()) return
        if (spaceResult is ApiResult.Failure && spaceResult.statusCode == 404 ||
            plantResult is ApiResult.Failure && plantResult.statusCode == 404
        ) {
            spaces = emptyList()
            plants = emptyList()
            error = "The Plantupia service isn't available on this server. Your plant records will appear when the Plantupia backend is ready."
        } else {
            when (spaceResult) {
                is ApiResult.Failure -> { error = spaceResult.message }
                is ApiResult.Success -> spaces = spaceResult.value
            }
            when (plantResult) {
                is ApiResult.Failure -> { error = plantResult.message }
                is ApiResult.Success -> plants = plantResult.value
            }
        }
        loading = false
    }

    suspend fun loadPending(plantId: String, generation: Int) {
        if (!isCurrentRequest(plantId, generation)) return
        pendingPageError = null
        loadPlantupiaPendingCheckIns(
            isCurrent = { isCurrentRequest(plantId, generation) },
            request = { repository.historyPage(plantId, pendingOnly = true) },
            onState = { pendingState = it },
        )
    }

    suspend fun openPlant(plant: PlantupiaPlant) {
        if (!lifetime.isCurrent()) return
        if (PlantupiaCreateIntent.pending.needsPlantSelection() &&
            plant !in actionablePlantChoices(plants)
        ) {
            creationAccessNotice = "This plant is read-only. Choose a plant you can care for, or create your own."
            return
        }
        creationAccessNotice = null
        detailRequestGeneration += 1
        val requestGeneration = detailRequestGeneration
        openDetailMode = PlantupiaCreateIntent.pending.takeIf { plant.active }
        showNameEditor = false
        showSharedDetailsEditor = false
        sharedPlantLabel = plant.label
        sharedSpecies = plant.species
        sharedPlacement = plant.placement
        showCareEditor = openDetailMode == PlantupiaCreateMode.OBSERVATION
        PlantupiaCreateIntent.clear()
        currentPlantId = plant.id
        saving = false
        pendingArchiveAction = null
        // Never let another plant's private history flash while the new detail loads.
        events = emptyList()
        photoToView = null
        onOpenPlant()
        plantName = plant.name
        note = ""
        selectedCareAction = null
        carePhotoUri = null
        carePhotoRequestGeneration = null
        checkAgainInDays = null
        customFollowUpDate = null
        reminderOptIn = false
        requestedExistingReminder = null
        reminderPermissionGeneration = null
        followUpTarget = null
        followUpResult = ""
        movingCheckIn = null
        movedCheckInDate = null
        pendingState = PlantupiaPendingCheckInsState.Loading
        pendingPageError = null
        pendingPageLoading = false
        olderEventsLoading = false
        historyRetryLoading = false
        historyLoadFailed = false
        detailLoading = true
        error = null
        nextHistoryOffset = null
        val historyResponse = repository.historyPage(plant.id)
        if (!isCurrentRequest(plant.id, requestGeneration)) return
        when (val response = historyResponse) {
            is ApiResult.Success -> {
                events = response.value.events
                nextHistoryOffset = response.value.nextOffset
                historyLoadFailed = false
            }
            is ApiResult.Failure -> {
                events = emptyList()
                historyLoadFailed = true
                error = response.message
            }
        }
        detailLoading = false
        loadPending(plant.id, requestGeneration)
    }

    suspend fun reloadPlantRecords(plantId: String) {
        // Invalidate older history/pending/pagination, preserving note/photo/action/date.
        detailRequestGeneration += 1
        val generation = detailRequestGeneration
        val current = { isCurrentRequest(plantId, generation) }
        detailLoading = true
        pendingState = PlantupiaPendingCheckInsState.Loading
        pendingPageError = null
        pendingPageLoading = false
        olderEventsLoading = false
        historyRetryLoading = false
        reloadPlantupiaMutationRecords(
            isCurrent = current,
            refreshInventory = { refresh(current) },
            loadHistory = { repository.historyPage(plantId) },
            applyHistory = { history ->
                when (history) {
                    is ApiResult.Success -> {
                        events = history.value.events
                        nextHistoryOffset = history.value.nextOffset
                        historyLoadFailed = false
                    }
                    is ApiResult.Failure -> { historyLoadFailed = true; error = history.message }
                }
                detailLoading = false
            },
            loadPending = { loadPending(plantId, generation) },
            onFinished = { saving = false },
        )
    }

    fun saveMoment(plant: PlantupiaPlant, kind: String, details: String) {
        val generation = detailRequestGeneration
        val photo = carePhotoUri
        val followUpDate = selectedFollowUpDate(customFollowUpDate, checkAgainInDays, LocalDate.now())
        val optedInToReminder = reminderOptIn
        val current = { isCurrentRequest(plant.id, generation) }
        scope.launch {
            var reminderScheduled = true
            runPlantupiaMutation(
                isAccountCurrent = lifetime::isCurrent,
                isDetailCurrent = current,
                request = {
                    photo?.let { repository.recordWithPhoto(plant.id, kind, details, it, followUpOn = followUpDate, reminder = optedInToReminder) }
                        ?: repository.record(plant.id, kind, details, followUpOn = followUpDate, reminder = optedInToReminder)
                },
                onStarted = { saving = true },
                onCommitted = { eventId ->
                    if (optedInToReminder && followUpDate != null) {
                        reminderScheduled = PlantupiaCareReminderScheduler.scheduleOnce(context, eventId, followUpDate, plant.id)
                    }
                },
                onSuccess = { eventId ->
                    repository.confirmSubmission(plant.id, "event", eventId)
                    if (!current()) return@runPlantupiaMutation
                    val reloadGeneration = detailRequestGeneration + 1
                    openPlant(plant)
                    if (isCurrentRequest(plant.id, reloadGeneration) && !reminderScheduled) {
                        error = "Your entry was saved, but the reminder couldn't be scheduled. Check notification permissions."
                    }
                },
                onFailure = { error = it.message },
                onFinished = { saving = false },
            )
        }
    }

    fun closePlant() {
        detailRequestGeneration += 1
        showAssistant = false
        currentPlantId = null
        saving = false
        pendingArchiveAction = null
        requestedExistingReminder = null
        reminderPermissionGeneration = null
        carePhotoRequestGeneration = null
        openDetailMode = null
        photoToView = null
        events = emptyList()
        pendingState = PlantupiaPendingCheckInsState.Loading
        pendingPageError = null
        nextHistoryOffset = null
        pendingPageLoading = false
        olderEventsLoading = false
        historyRetryLoading = false
        historyLoadFailed = false
        detailLoading = false
        error = null
        scope.launch { refresh() }
    }

    BackHandler(enabled = selectedPlant != null) {
        // The first Android Back returns to the customer's living space,
        // instead of unexpectedly leaving the existing Nova tab.
        closePlant()
    }

    LaunchedEffect(currentPlantId, saving, detailRequestGeneration) {
        val plantId = currentPlantId
        pendingPrivateSaves = emptyList()
        if (plantId != null && !saving) {
            val generation = detailRequestGeneration
            when (val pending = repository.pendingSubmissions(plantId)) {
                is ApiResult.Success -> if (isCurrentRequest(plantId, generation)) pendingPrivateSaves = pending.value.filter { it.operation == "event" }
                is ApiResult.Failure -> if (isCurrentRequest(plantId, generation)) error = pending.message
            }
        }
    }
    val handoverTarget = PlantupiaHandoverNavigation.target
    LaunchedEffect(handoverTarget) {
        refresh()
    }
    LaunchedEffect(PlantupiaCreateIntent.pending, plants, loading) {
        if (!loading && error == null && PlantupiaCreateIntent.pending.needsPlantSelection()) {
            val available = actionablePlantChoices(plants)
            if (available.size == 1) openPlant(available.first())
        }
    }
    LaunchedEffect(spaces, loading, error, handoverTarget) {
        // Never reset a saved user choice during the initial empty/loading
        // frame or an unavailable API. Validate only against a successful,
        // authenticated server response.
        if (loading || error != null) return@LaunchedEffect
        if (handoverTarget?.first == accountId && spaces.any { it.id == handoverTarget.second }) {
            focusedSpaceId = handoverTarget.second
            plantSearch = ""
            PlantupiaHandoverNavigation.consume()
            return@LaunchedEffect
        }
        val allowed = resolveFocusedSpace(spaces, focusedSpaceId)
        if (focusedSpaceId != allowed?.id) {
            focusedSpaceId = allowed?.id
            plantSearch = ""
        }
    }
    LaunchedEffect(focusedSpaceId, plantSearch) {
        // Reuse Nova's fast, progressive inventory; do not render 100+ units.
        visiblePlantCount = 8
        visibleArchivedCount = 8
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cream, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (selectedPlant == null) {
            TextButton(onClick = { PlantupiaHandoverNavigation.open() }) {
                Text("Received plants from Plantupia? Connect a delivery", color = deepGreen)
            }
            if (PlantupiaCreateIntent.pending.needsPlantSelection()) {
                PlantupiaPanel {
                    Text(
                        if (PlantupiaCreateIntent.pending == PlantupiaCreateMode.QUESTION)
                            "Choose a plant to ask the community about"
                        else "Choose a plant to record a moment",
                        color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    )
                    Text(
                        "Private care records stay private. Public sharing requires your consent.",
                        color = muted, fontSize = 12.sp,
                    )
                    if (actionablePlantChoices(plants).isEmpty()) {
                        Text(
                            "None of your active plants allow care or questions yet. You can create your own private space and plant.",
                            color = muted, fontSize = 12.sp,
                        )
                        TextButton(onClick = {
                            creationAccessNotice = null
                            PlantupiaCreateIntent.request(PlantupiaCreateMode.ADD_PLANT)
                        }) {
                            Text("+ Add your own plant", color = deepGreen)
                        }
                    }
                    if (creationAccessNotice != null) {
                        Text(creationAccessNotice.orEmpty(), color = Color(0xFF9A4A31), fontSize = 12.sp)
                    }
                    // Show an immediate, bounded picker instead of making the
                    // user scroll past the space hero and old inventory to act.
                    PlantupiaIntentPlantPicker(
                        plants = plants,
                        onChoosePlant = { plant -> scope.launch { openPlant(plant) } },
                    )
                    TextButton(onClick = {
                        creationAccessNotice = null
                        PlantupiaCreateIntent.clear()
                    }) {
                        Text("Cancel", color = deepGreen)
                    }
                }
            }
            // Quiet Care: a small, real next step before hero and AI.
            if (!loading && error == null) {
                PlantupiaTodayPanel(
                    plants = plants,
                    onOpenPlant = { plant -> scope.launch { openPlant(plant) } },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (plants.any { it.active && it.canRecord }) {
                        Button(
                            onClick = { PlantupiaCreateIntent.request(PlantupiaCreateMode.OBSERVATION) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                        ) {
                            Text("Record care", color = white)
                        }
                    }
                    OutlinedButton(
                        onClick = { PlantupiaCreateIntent.request(PlantupiaCreateMode.ADD_PLANT) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text("+ Add plant", color = deepGreen)
                    }
                }
            }
            // Pass 07: show the authentic place before AI and creation controls.
            // An empty or failed API must never display stock customer imagery.
            if (!loading && error == null && spaces.isNotEmpty()) {
                PlantupiaSpaceSwitcher(
                    spaces = spaces,
                    plants = plants,
                    focusedSpaceId = focusedSpaceId,
                    onSelectSpace = { id ->
                        focusedSpaceId = id
                        plantSearch = ""
                    },
                )
                val featured = resolveFocusedSpace(spaces, focusedSpaceId) ?: spaces.first()
                val focusedPlants = plants.filter { it.spaceId == featured.id && it.active }
                val featuredCount = focusedPlants.size
                // A plant cover is evidence of that plant, NOT an architectural site photo.
                val heroPlant = focusedPlants.firstOrNull { it.coverPhoto.isNotBlank() }
                val heroPhoto = heroPlant?.coverPhoto.orEmpty()
                var heroPhotoLoaded by remember(featured.id, heroPhoto) { mutableStateOf(false) }
                // Only a successfully rendered, owner-uploaded plant photo may
                // switch the architectural card to white-on-photo treatment.
                val photoHero = heroPhoto.isNotBlank() && heroPhotoLoaded
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
                            .height(if (heroPhoto.isNotBlank()) 196.dp else 154.dp),
                    ) {
                        if (heroPhoto.isNotBlank()) {
                            PlantupiaEvidencePhoto(
                                photoUrl = heroPhoto,
                                photoDescription = "Uploaded photo of ${heroPlant?.name.orEmpty()} in ${featured.name}",
                                modifier = Modifier.matchParentSize(),
                                cornerRadius = 22.dp,
                                onPhotoLoadChanged = { loaded -> heroPhotoLoaded = loaded },
                            )
                            if (photoHero) {
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
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(19.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                spaceHeroMediaEyebrow(photoHero),
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
                                spacePlantCountSummary(featuredCount),
                                color = heroMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                // Editing site context belongs with its actual chosen space,
                // not beneath unrelated AI/Create controls and the social feed.
                PlantupiaSpaceDetailsEditor(
                    space = featured,
                    repository = repository,
                    onSaved = { scope.launch { refresh() } },
                )
            }
            PlantupiaSelfServiceForms(
                spaces = spaces,
                repository = repository,
                onCreated = { scope.launch { refresh() } },
                onRefreshSpaces = { scope.launch { refresh() } },
                onPlantCreated = { plantId, spaceId ->
                    // Completing onboarding opens the real new plant, rather
                    // than dropping a first-time owner back into a generic list.
                    if (!lifetime.isCurrent()) return@PlantupiaSelfServiceForms
                    val generation = detailRequestGeneration
                    val current = { lifetime.isCurrent() && generation == detailRequestGeneration && currentPlantId == null }
                    focusedSpaceId = spaceId
                    plantSearch = ""
                    scope.launch {
                        refresh(current)
                        if (current()) plants.firstOrNull { it.id == plantId }?.let { openPlant(it) }
                    }
                },
                preferredSpaceId = focusedSpaceId,
                enabled = !loading && error == null,
                creationRequest = PlantupiaCreateIntent.pending,
                onRequestConsumed = { PlantupiaCreateIntent.clear() },
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
                Text("Ask the Plantupia assistant", color = deepGreen)
            }
            if (loading) {
                Text("Loading your spaces…", color = muted, fontSize = 13.sp)
            } else {
                if (spaces.isNotEmpty()) {
                    // Recent events remain scoped to the currently focused site.
                    val featured = resolveFocusedSpace(spaces, focusedSpaceId) ?: spaces.first()
                    val focusedPlants = plants.filter { it.spaceId == featured.id && it.active }
                    val recent = focusedPlants
                        .filter { it.lastEventAt.isNotBlank() }
                        .sortedByDescending { it.lastEventAt }
                    if (recent.isNotEmpty()) {
                        val latest = recent.first()
                        BrandEyebrow("LATEST OBSERVATION")
                        PlantupiaPanel {
                            Text(latest.name, color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "${latest.lastEventAt.take(10)} · ${eventLabel(latest.lastEventKind)}",
                                color = muted, fontSize = 12.sp,
                            )
                            TextButton(onClick = { scope.launch { openPlant(latest) } }) {
                                Text("View history →", color = deepGreen)
                            }
                        }
                        BrandEyebrow("RECENT MOMENTS")
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
                                        PlantupiaEvidencePhoto(
                                            photoUrl = plant.coverPhoto,
                                            photoDescription = "Uploaded photograph of " + plant.name,
                                            modifier = Modifier.fillMaxWidth().height(106.dp),
                                            cornerRadius = 10.dp,
                                        )
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
                            "Your first plant photo or observation will appear here. No sample records.",
                            color = muted, fontSize = 12.sp,
                        )
                    }
                    BrandEyebrow("YOUR PLANTS")
                }
                val focused = resolveFocusedSpace(spaces, focusedSpaceId)
                if (focused != null) {
                    val sitePlants = plants.filter { it.spaceId == focused.id && it.active }
                    if (sitePlants.isEmpty()) {
                        PlantupiaPanel {
                            val hasArchived = plants.any { it.spaceId == focused.id && !it.active }
                            if (!hasArchived) {
                                // An archived plant is a real existing record, not
                                // the "first plant" state illustrated by this art.
                                PlantupiaEmptyPlantArt(modifier = Modifier.fillMaxWidth().height(92.dp))
                            }
                            Text(
                                if (hasArchived) "No active plants in this space" else "Your space is waiting for its first plant",
                                color = forestText, fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (hasArchived) "Archived records are available below. Restore a plant or add a new one." else "Add a plant with a photo and placement. Its history and check-ins will appear here.",
                                color = muted, fontSize = 13.sp, lineHeight = 19.sp,
                            )
                            if (focused.canAddPlant) {
                                TextButton(onClick = { PlantupiaCreateIntent.request(PlantupiaCreateMode.ADD_PLANT) }) {
                                    Text("Add your first plant →", color = deepGreen)
                                }
                            }
                        }
                    } else {
                        if (sitePlants.size > 8) {
                            OutlinedTextField(
                                value = plantSearch,
                                onValueChange = { plantSearch = it.take(80) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search plants in " + focused.name) },
                                singleLine = true,
                            )
                        }
                        val matchingPlants = findSpacePlants(plants, focused.id, plantSearch)
                        if (matchingPlants.isEmpty()) {
                            Text("No plants in this space match your search.", color = muted, fontSize = 12.sp)
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
                                Text("Load more plants · ${visiblePlantCount} of ${matchingPlants.size}", color = deepGreen)
                            }
                        }
                    }
                }
            }
            val archiveSpace = resolveFocusedSpace(spaces, focusedSpaceId)
            val archivedUnits = plants.filter { !it.active && it.spaceId == archiveSpace?.id }
            if (archivedUnits.isNotEmpty() && archiveSpace != null) {
                TextButton(onClick = {
                    showArchived = !showArchived
                    archiveSearch = ""
                    visibleArchivedCount = 8
                }) {
                    Text(
                        if (showArchived) "Hide archived plants ↑"
                        else "Archived plants · ${archivedUnits.size} ↓",
                        color = deepGreen,
                    )
                }
                if (showArchived) {
                    Text(
                        "Archived plants keep their private photos and history. They do not appear in active care.",
                        color = muted, fontSize = 12.sp,
                    )
                    if (archivedUnits.size > 8) {
                        OutlinedTextField(
                            value = archiveSearch,
                            onValueChange = { archiveSearch = it.take(80); visibleArchivedCount = 8 },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search archived plants") },
                            singleLine = true,
                        )
                    }
                    val archivedMatches = findArchivedPlants(plants, archiveSpace.id, archiveSearch)
                    if (archivedMatches.isEmpty()) {
                        Text("No archived records match your search.", color = muted, fontSize = 12.sp)
                    }
                    archivedMatches.take(visibleArchivedCount).forEach { plant ->
                        PlantupiaPlantInventoryCard(
                            plant = plant,
                            eventLabel = "Archived record",
                            onClick = { scope.launch { openPlant(plant) } },
                        )
                    }
                    if (archivedMatches.size > visibleArchivedCount) {
                        TextButton(onClick = { visibleArchivedCount += 8 }) {
                            Text("Load more archived plants", color = deepGreen)
                        }
                    }
                }
            }
            if (error != null) Text(error.orEmpty(), color = Color(0xFF9A4A31), fontSize = 12.sp)
            TextButton(onClick = { scope.launch { refresh() } }) {
                Text("Refresh records", color = deepGreen)
            }
        } else {
            TextButton(onClick = { closePlant() }) {
                Text("← Back to space", color = deepGreen)
            }
            BrandEyebrow(
                if (openDetailMode == PlantupiaCreateMode.QUESTION) "ASK THE CIRCLE"
                else "YOUR PLANT · PRIVATE RECORD"
            )
            if (openDetailMode == PlantupiaCreateMode.QUESTION) {
                Text(
                    "Choose what to share. Your care records and place stay private.",
                    color = muted, fontSize = 12.sp,
                )
            }
            // Identify the *actual selected plant* before showing its photo or controls.
            Text(selectedPlant.name, fontFamily = FontFamily.Serif, fontSize = 35.sp, color = deepGreen)
            PlantupiaCareGuidePanel(selectedPlant.id, repository)
            if (!selectedPlant.active) {
                Text(
                    "ARCHIVED · Read-only living record. Photos, notes and history are retained.",
                    color = muted, fontSize = 12.sp,
                )
            }
            val meta = listOf(selectedPlant.species, selectedPlant.placement).filter(String::isNotBlank).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, fontSize = 13.sp, color = muted)
            if (selectedPlant.coverPhoto.isNotBlank()) {
                PlantupiaEvidencePhoto(
                    photoUrl = selectedPlant.coverPhoto,
                    photoDescription = "Uploaded photograph of " + selectedPlant.name,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    cornerRadius = 16.dp,
                    onOpenPhoto = { photoToView = selectedPlant.coverPhoto to selectedPlant.name },
                )
            } else {
                // No cover photo is not a missing plant or invented photographic evidence.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.softSage,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlantupiaMissingPhotoMark(modifier = Modifier.size(52.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("NO COVER PHOTO YET", style = PlantupiaText.sectionLabel)
                            Text(
                                "Photos from recorded observations, if any, appear in the private history below.",
                                style = PlantupiaText.supporting,
                            )
                        }
                    }
                }
            }

            pendingPrivateSaves.filter { it.plant == selectedPlant.id }.forEach { pending ->
                PlantupiaPanel {
                    Text("An earlier entry is awaiting confirmation. Retry its original text and photo; it won't create a second record.", color = forestText, fontSize = 13.sp)
                    Text(pending.fields["note"].orEmpty(), color = muted, fontSize = 12.sp)
                    Button(enabled = !saving && selectedPlant.canRecord, onClick = {
                        val generation = detailRequestGeneration
                        scope.launch {
                            runPlantupiaMutation(
                                isAccountCurrent = lifetime::isCurrent,
                                isDetailCurrent = { isCurrentRequest(selectedPlant.id, generation) },
                                request = { repository.retrySubmission(pending) },
                                onStarted = { saving = true },
                                onCommitted = { eventId ->
                                    pending.fields["follow_up_for"]?.let { PlantupiaCareReminderScheduler.cancel(context, it) }
                                    val date = pending.fields["follow_up_on"]
                                    if (pending.reminder && date != null) PlantupiaCareReminderScheduler.scheduleOnce(context, eventId, date, selectedPlant.id)
                                },
                                onSuccess = { eventId ->
                                    repository.confirmSubmission(selectedPlant.id, pending.slot, eventId)
                                    if (isCurrentRequest(selectedPlant.id, generation)) reloadPlantRecords(selectedPlant.id)
                                },
                                onFailure = { error = it.message },
                                onFinished = { saving = false },
                            )
                        }
                    }) { Text("Retry original entry") }
                }
            }

            // Care is the primary reason to open a plant. Keep the
            // record-action composer near the identity, not after
            // rename + pending results + lengthy timeline.
            if (openDetailMode != PlantupiaCreateMode.QUESTION) {
                BrandEyebrow("CARE & OBSERVATIONS")
                if (selectedPlant.canRecord) {
                    Button(
                        onClick = { showCareEditor = !showCareEditor },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                    ) {
                        Text(if (showCareEditor) "Close care form ↑" else "+ Record care or observation", color = white)
                    }
                if (showCareEditor) PlantupiaPanel {
                    Text("What did you do?", color = forestText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    // Respect the true editor width and fontScale, not only device pixels.
                    // Each readable action still retains its authored native pictogram.
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val columns = plantupiaCareActionColumns(
                            availableWidthDp = maxWidth.value,
                            fontScale = LocalDensity.current.fontScale,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            PlantupiaCareAction.entries.chunked(columns).forEach { group ->
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
                        }
                    }
                    Text("These record actual actions, not watering recommendations.", color = muted, fontSize = 11.sp)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(1500) },
                        label = { Text("What did you observe or do?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        enabled = !saving,
                    )
                    OutlinedButton(
                        onClick = {
                            carePhotoRequestGeneration = detailRequestGeneration
                            carePhotoPicker.launch("image/*")
                        },
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
                            if (carePhotoUri == null) "Add a photo of your observation" else "Change selected photo",
                            color = deepGreen,
                        )
                    }
                    carePhotoUri?.let { selectedPhoto ->
                        AsyncImage(
                            model = selectedPhoto,
                            contentDescription = "Preview the photo before saving it to the plant's private history",
                            modifier = Modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        TextButton(
                            onClick = { carePhotoUri = null },
                            enabled = !saving,
                        ) {
                            Text("Remove selected photo", color = muted)
                        }
                    }
                    Text("When would you like to check this plant again?", color = forestText, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(7, 14).forEach { days ->
                            OutlinedButton(
                                enabled = !saving,
                                onClick = {
                                    checkAgainInDays = if (checkAgainInDays == days && customFollowUpDate == null) null else days
                                    customFollowUpDate = null
                                    if (checkAgainInDays == null) reminderOptIn = false
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(if (checkAgainInDays == days) "✓ In $days days" else "In $days days", color = deepGreen, fontSize = 12.sp) }
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                        onClick = {
                            val generation = detailRequestGeneration
                            val today = LocalDate.now()
                            val current = customFollowUpDate
                                ?: checkAgainInDays?.let { today.plusDays(it.toLong()) }
                                ?: today.plusDays(7)
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val picked = LocalDate.of(year, month + 1, day)
                                    if (isCurrentRequest(selectedPlant.id, generation) && !picked.isBefore(LocalDate.now())) {
                                        customFollowUpDate = picked
                                        checkAgainInDays = null
                                    }
                                },
                                current.year, current.monthValue - 1, current.dayOfMonth,
                            ).apply {
                                datePicker.minDate = today.atStartOfDay(ZoneId.systemDefault())
                                    .toInstant().toEpochMilli()
                            }.show()
                        },
                    ) { Text("Choose a date", color = deepGreen) }
                    val actualFollowUpDate = selectedFollowUpDate(
                        customFollowUpDate, checkAgainInDays, LocalDate.now(),
                    )
                    if (actualFollowUpDate != null) {
                        Text("Check-in date: $actualFollowUpDate", color = deepGreen, fontSize = 12.sp)
                        TextButton(onClick = {
                            checkAgainInDays = null
                            customFollowUpDate = null
                            reminderOptIn = false
                        }, enabled = !saving) {
                            Text("No check-in date", color = muted, fontSize = 12.sp)
                        }
                    }
                    val validFollowUpDate = isValidCheckInDate(customFollowUpDate, LocalDate.now())
                    if (!validFollowUpDate) {
                        Text("That date has passed. Choose another check-in date.", color = Color(0xFF9A4A31), fontSize = 12.sp)
                    }
                    if (actualFollowUpDate != null && validFollowUpDate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = reminderOptIn,
                                enabled = !saving,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        reminderOptIn = false
                                    } else if (
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.POST_NOTIFICATIONS,
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        reminderPermissionGeneration = detailRequestGeneration
                                        reminderPermissionPicker.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        reminderOptIn = true
                                    }
                                },
                            )
                            Text(
                                "Remind me on the check-in morning (subject to device settings)",
                                color = muted, fontSize = 12.sp, lineHeight = 19.sp,
                            )
                        }
                        Text(
                            "Optional reminder to inspect your plant, not an instruction to water. Battery settings may delay it.",
                            color = muted, fontSize = 11.sp,
                        )
                    }
                    // Each save action has its own full-width touch target.
                    // Never force three unequal labels into a single weighted row:
                    // large fonts made "Concerning change" render letter-by-letter.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = !saving && validFollowUpDate && (note.isNotBlank() || carePhotoUri != null),
                            onClick = { saveMoment(selectedPlant, "observation", note) },
                        ) { Text("Save observation", color = deepGreen) }
                        Button(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = !saving && validFollowUpDate && (selectedCareAction != null || note.isNotBlank() || carePhotoUri != null),
                            onClick = { saveMoment(selectedPlant, "care", buildPlantupiaCareNote(selectedCareAction, note)) },
                            colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                        ) { Text("Save care", color = white) }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = !saving && validFollowUpDate && (note.isNotBlank() || carePhotoUri != null),
                            onClick = { saveMoment(selectedPlant, "issue", note) },
                        ) { Text("Record a concern", color = deepGreen) }
                    }
                    Text(
                        "Record what you actually noticed. You don't need to diagnose the cause.",
                        color = muted, fontSize = 11.sp,
                    )
                }
                } else {
                    Text("You have read-only access to this plant's history. Ask a space manager for recording permission.", color = muted, fontSize = 12.sp)
                }
                if (error != null) Text(error.orEmpty(), color = Color(0xFF9A4A31), fontSize = 12.sp)
            }

            if (openDetailMode != PlantupiaCreateMode.QUESTION) {
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
                    Text("Plantupia assistant (optional)", color = deepGreen)
                }
            }
            if (openDetailMode == PlantupiaCreateMode.QUESTION) {
                val questionGeneration = detailRequestGeneration
                PlantupiaQuestionsPanel(
                    isParentCurrent = { isCurrentRequest(selectedPlant.id, questionGeneration) },
                    requestGeneration = questionGeneration,
                    plantId = selectedPlant.id,
                    repository = repository,
                    compactHeader = true,
                    canPublish = selectedPlant.canRecord,
                    onOpenPost = { postId ->
                        onOpenLinkedPost(postId) { isCurrentRequest(selectedPlant.id, questionGeneration) }
                    },
                    onOutcomeSaved = {
                        val plantId = selectedPlant.id
                        val generation = questionGeneration
                        if (!isCurrentRequest(plantId, generation)) return@PlantupiaQuestionsPanel
                        val response = repository.historyPage(plantId)
                        if (!isCurrentRequest(plantId, generation)) return@PlantupiaQuestionsPanel
                        when (response) {
                            is ApiResult.Success -> {
                                events = response.value.events
                                nextHistoryOffset = response.value.nextOffset
                                historyLoadFailed = false
                                error = null
                            }
                            is ApiResult.Failure -> {
                                historyLoadFailed = true
                                error = response.message
                            }
                        }
                    },
                )
            }

            // Asking the Circle is a focused public workflow. Private check-ins,
            // history and record administration belong to the normal plant detail.
            if (openDetailMode != PlantupiaCreateMode.QUESTION) {
            BrandEyebrow("CHECK BACK")
            if (pendingState is PlantupiaPendingCheckInsState.Loading) {
                Text("Loading check-ins…", color = muted, fontSize = 12.sp)
            } else if (pendingState is PlantupiaPendingCheckInsState.Unavailable) {
                PlantupiaPanel {
                    Text("Check-ins couldn't be loaded", color = forestText, fontWeight = FontWeight.SemiBold)
                    Text((pendingState as PlantupiaPendingCheckInsState.Unavailable).message, color = muted, fontSize = 12.sp)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = {
                            val plantId = selectedPlant.id
                            val generation = detailRequestGeneration
                            scope.launch { loadPending(plantId, generation) }
                        },
                    ) { Text("Retry loading check-ins", color = deepGreen) }
                }
            } else if (pendingState is PlantupiaPendingCheckInsState.Empty) {
                Text("When you record care or a change, you can choose when to check its outcome.", color = muted, fontSize = 12.sp)
            } else {
                pendingFollowUps.forEach { check ->
                    PlantupiaPanel {
                        Text(
                            "Check-in ${check.followUpOn} · ${eventLabel(check.kind)}",
                            color = deepGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        )
                        if (check.note.isNotBlank()) Text(check.note, color = muted, fontSize = 12.sp)
                        Text("Observe the plant before saving an outcome. Check-in dates are not watering orders.", color = muted, fontSize = 11.sp)
                        if (selectedPlant.canRecord) {
                            OutlinedButton(onClick = {
                                followUpTarget = if (followUpTarget?.id == check.id) null else check
                                followUpResult = ""
                                movingCheckIn = null
                                movedCheckInDate = null
                            }, enabled = !saving) {
                                Text(if (followUpTarget?.id == check.id) "Cancel" else "Record check-in outcome", color = deepGreen)
                            }
                            TextButton(
                                enabled = !saving,
                                onClick = {
                                    movingCheckIn = if (movingCheckIn?.id == check.id) null else check
                                    movedCheckInDate = null
                                    followUpTarget = null
                                },
                            ) {
                                Text(
                                    if (movingCheckIn?.id == check.id) "Cancel date change" else "Choose another check-in day",
                                    color = deepGreen,
                                )
                            }
                            if (movingCheckIn?.id == check.id) {
                                Text(
                                    "Change this check-in date without recording a watering or plant-health result.",
                                    color = muted, fontSize = 11.sp,
                                )
                                OutlinedButton(
                                    enabled = !saving,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        val generation = detailRequestGeneration
                                        val tomorrow = LocalDate.now().plusDays(1)
                                        val selected = movedCheckInDate ?: runCatching {
                                            LocalDate.parse(check.followUpOn)
                                        }.getOrNull()?.takeIf { it.isAfter(LocalDate.now()) } ?: tomorrow
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                if (isCurrentRequest(selectedPlant.id, generation)) {
                                                    movedCheckInDate = LocalDate.of(year, month + 1, day)
                                                }
                                            },
                                            selected.year, selected.monthValue - 1, selected.dayOfMonth,
                                        ).apply {
                                            datePicker.minDate = tomorrow.atStartOfDay(ZoneId.systemDefault())
                                                .toInstant().toEpochMilli()
                                        }.show()
                                    },
                                ) {
                                    Text(
                                        movedCheckInDate?.let { "New date: $it" } ?: "Choose a future date",
                                        color = deepGreen,
                                    )
                                }
                                Button(
                                    enabled = !saving && movedCheckInDate?.isAfter(LocalDate.now()) == true &&
                                        movedCheckInDate.toString() != check.followUpOn,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                                    onClick = {
                                        val target = movingCheckIn ?: return@Button
                                        val newDate = movedCheckInDate?.toString() ?: return@Button
                                        val generation = detailRequestGeneration
                                        val current = { isCurrentRequest(selectedPlant.id, generation) }
                                        val hadReminder = PlantupiaCareReminderScheduler.isScheduled(context, target.id)
                                        scope.launch {
                                            var reminderScheduled = true
                                            runPlantupiaMutation(
                                                isAccountCurrent = lifetime::isCurrent,
                                                isDetailCurrent = current,
                                                request = { repository.moveCheckIn(selectedPlant.id, target.id, newDate) },
                                                onStarted = { saving = true },
                                                onCommitted = {
                                                    PlantupiaCareReminderScheduler.cancel(context, target.id)
                                                    if (hadReminder) reminderScheduled = PlantupiaCareReminderScheduler.schedule(context, target.id, newDate, selectedPlant.id)
                                                },
                                                onSuccess = {
                                                    val reloadGeneration = detailRequestGeneration + 1
                                                    openPlant(selectedPlant)
                                                    if (isCurrentRequest(selectedPlant.id, reloadGeneration) && !reminderScheduled) {
                                                        error = "Check-in date was saved, but the local reminder could not be moved."
                                                    }
                                                },
                                                onFailure = { error = it.message },
                                                onFinished = { saving = false },
                                            )
                                        }
                                    },
                                ) { Text(if (saving) "Saving…" else "Save new check-in date", color = white) }
                            }
                        }
                        val reminderIsActive = remember(check.id, reminderRevision) {
                            PlantupiaCareReminderScheduler.isScheduled(context, check.id)
                        }
                        if (reminderIsActive) {
                            TextButton(onClick = {
                                PlantupiaCareReminderScheduler.cancel(context, check.id)
                                reminderRevision += 1
                            }) {
                                Text("Cancel reminder (keep check-in)", color = muted)
                            }
                        } else if (checkInDelayMillis(check.followUpOn, ZonedDateTime.now()) != null) {
                            TextButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS,
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    requestedExistingReminder = check
                                    reminderPermissionGeneration = detailRequestGeneration
                                    reminderPermissionPicker.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val scheduled = PlantupiaCareReminderScheduler.schedule(
                                        context, check.id, check.followUpOn, selectedPlant.id,
                                    )
                                    reminderRevision += 1
                                    error = if (scheduled) null
                                        else "Couldn't enable a reminder for this date."
                                }
                            }) {
                                Text("Remind me on check-in day", color = deepGreen)
                            }
                        }
                    }
                }
                pendingPageError?.let { message ->
                    Text("Older check-ins couldn't be loaded: $message", color = PlantupiaColors.errorText, fontSize = 12.sp)
                }
                if (nextPendingOffset != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !pendingPageLoading,
                        onClick = {
                            val offset = nextPendingOffset ?: return@OutlinedButton
                            val plantId = selectedPlant.id
                            val generation = detailRequestGeneration
                            scope.launch {
                                pendingPageLoading = true
                                pendingPageError = null
                                val response = repository.historyPage(
                                    plantId, offset = offset, pendingOnly = true,
                                )
                                if (!isCurrentRequest(plantId, generation)) return@launch
                                when (response) {
                                    is ApiResult.Success -> {
                                        pendingState = pendingCheckInsFromPage(response.value.copy(
                                            events = (pendingFollowUps + response.value.events).distinctBy { it.id },
                                        ))
                                        pendingPageError = null
                                    }
                                    is ApiResult.Failure -> pendingPageError = response.message
                                }
                                pendingPageLoading = false
                            }
                        },
                    ) {
                        Text(
                            if (pendingPageLoading) "Loading older check-ins…"
                            else if (pendingPageError != null) "Retry older check-ins" else "Load older check-ins",
                            color = deepGreen,
                        )
                    }
                }
            }
            if (followUpTarget != null && selectedPlant.canRecord) {
                PlantupiaPanel {
                    Text("What changed when you checked the plant again?", color = deepGreen, fontSize = 15.sp)
                    OutlinedTextField(
                        value = followUpResult,
                        onValueChange = { followUpResult = it.take(1500) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("What you actually observed *") },
                        enabled = !saving,
                    )
                    Button(
                        enabled = !saving && followUpResult.isNotBlank(),
                        onClick = {
                            val source = followUpTarget ?: return@Button
                            val generation = detailRequestGeneration
                            val submittedOutcome = followUpResult
                            scope.launch {
                                runPlantupiaMutation(
                                    isAccountCurrent = lifetime::isCurrent,
                                    isDetailCurrent = { isCurrentRequest(selectedPlant.id, generation) },
                                    request = { repository.record(selectedPlant.id, "outcome", submittedOutcome, followUpFor = source.id) },
                                    onStarted = { saving = true },
                                    onCommitted = { PlantupiaCareReminderScheduler.cancel(context, source.id) },
                                    onSuccess = { eventId ->
                                        repository.confirmSubmission(selectedPlant.id, "outcome-${source.id}", eventId)
                                        if (isCurrentRequest(selectedPlant.id, generation)) openPlant(selectedPlant)
                                    },
                                    onFailure = { error = it.message },
                                    onFinished = { saving = false },
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                    ) { Text(if (saving) "Saving outcome…" else "Save outcome and close check-in", color = white) }
                }
            }
            BrandEyebrow("LIVING HISTORY")
            when (plantupiaHistoryDisplayState(
                loading = detailLoading || historyRetryLoading,
                failed = historyLoadFailed,
                recordCount = events.size,
            )) {
            PlantupiaHistoryDisplayState.LOADING ->
                Text("Loading history…", color = muted, fontSize = 13.sp)
            PlantupiaHistoryDisplayState.UNAVAILABLE -> {
                PlantupiaPanel {
                    Text(
                        "History couldn't be loaded",
                        color = forestText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "We couldn't verify the saved records. Please retry when your connection is available.",
                        color = muted,
                        fontSize = 12.sp,
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = !historyRetryLoading,
                        onClick = {
                            val plantId = selectedPlant.id
                            val generation = detailRequestGeneration
                            scope.launch {
                                historyRetryLoading = true
                                val response = repository.historyPage(plantId)
                                if (!isCurrentRequest(plantId, generation)) return@launch
                                when (response) {
                                    is ApiResult.Success -> {
                                        events = response.value.events
                                        nextHistoryOffset = response.value.nextOffset
                                        historyLoadFailed = false
                                        error = null
                                    }
                                    is ApiResult.Failure -> {
                                        historyLoadFailed = true
                                        error = response.message
                                    }
                                }
                                historyRetryLoading = false
                            }
                        },
                    ) { Text("Retry loading history", color = deepGreen) }
                }
            }
            PlantupiaHistoryDisplayState.EMPTY -> {
                // An original blank field-book motif is an empty-state cue,
                // never a fabricated note/photo/event. Real records appear only
                // after a successful API save and history refresh.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlantupiaEmptyFieldNotesArt(
                        modifier = Modifier.width(104.dp).height(86.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            "No field notes yet",
                            style = PlantupiaText.body,
                        )
                        Text(
                            "Your first saved care or observation will start this plant's history.",
                            style = PlantupiaText.supporting,
                        )
                    }
                }
            }
            PlantupiaHistoryDisplayState.RECORDED -> {
                Text("${events.size} recorded moments · actual observations, not automatic health scores", color = muted, fontSize = 12.sp)
                PlantupiaPhotoIndex(
                    events = events,
                    hasOlderPages = nextHistoryOffset != null,
                    onOpenPhoto = { photo ->
                        photoToView = photo.photoUrl to selectedPlant.name
                    },
                )
            }
            }
            val historyReady = !detailLoading && !historyRetryLoading && !historyLoadFailed
            if (historyReady && events.isNotEmpty()) {
                Text(
                    "EXPLORE HISTORY",
                    color = muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlantupiaHistoryFilter.entries.size) { index ->
                        val filter = PlantupiaHistoryFilter.entries[index]
                        OutlinedButton(
                            onClick = { historyFilter = filter },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, if (historyFilter == filter) deepGreen else sage,
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (historyFilter == filter) deepGreen else white,
                                contentColor = if (historyFilter == filter) white else deepGreen,
                            ),
                        ) {
                            Text(filter.label, fontSize = 12.sp)
                        }
                    }
                }
            }
            val visibleHistory = if (historyReady) {
                filteredPlantupiaHistory(events, historyFilter)
            } else {
                emptyList()
            }
            if (historyReady && events.isNotEmpty() && visibleHistory.isEmpty()) {
                Text(
                    "No ${historyFilter.label.lowercase()} in the loaded history. " +
                        "Load older moments to explore earlier records.",
                    color = muted, fontSize = 12.sp, lineHeight = 19.sp,
                )
            }
            visibleHistory.forEach { moment ->
                PlantupiaHistoryEventCard(
                    event = moment.event,
                    title = eventLabel(moment.event.kind),
                    evidenceLabel = evidenceLabel(moment.event.evidence),
                    linkedOutcomes = moment.outcomes,
                    onPhotoClick = {
                        if (moment.event.photoUrl.isNotBlank()) {
                            photoToView = moment.event.photoUrl to selectedPlant.name
                        }
                    },
                )
            }
            if (historyReady && nextHistoryOffset != null) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !olderEventsLoading,
                    onClick = {
                        val offset = nextHistoryOffset ?: return@OutlinedButton
                        val plantId = selectedPlant.id
                        val generation = detailRequestGeneration
                        scope.launch {
                            olderEventsLoading = true
                            val result = repository.historyPage(plantId, offset)
                            if (!isCurrentRequest(plantId, generation)) return@launch
                            when (result) {
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
                        if (olderEventsLoading) "Loading older moments…" else "Load older moments →",
                        color = deepGreen,
                    )
                }
            }
            // Secondary record administration follows care, chosen check-ins and real history.
            HorizontalDivider(color = sage)
            OutlinedButton(
                onClick = { showPlantManagement = !showPlantManagement },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (showPlantManagement) "Hide plant details & settings ↑"
                    else "Plant details & settings ↓",
                    color = deepGreen,
                )
            }
            if (showPlantManagement) {
                PlantupiaPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("YOUR PLANT NAME", color = muted, fontSize = 11.sp)
                            Text(selectedPlant.name, color = deepGreen, fontWeight = FontWeight.Medium, fontSize = 19.sp)
                        }
                        TextButton(onClick = { showNameEditor = !showNameEditor }) {
                            Text(if (showNameEditor) "Cancel" else "Rename", color = deepGreen)
                        }
                    }
                    if (showNameEditor) {
                    OutlinedTextField(
                        value = plantName,
                        onValueChange = { plantName = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Plant name") },
                        singleLine = true,
                        enabled = !saving,
                    )
                    Button(
                        enabled = !saving && plantName.trim().isNotBlank(),
                        onClick = {
                            val generation = detailRequestGeneration
                            val submittedName = plantName.trim()
                            scope.launch {
                                runPlantupiaMutation(
                                    isAccountCurrent = lifetime::isCurrent,
                                    isDetailCurrent = { isCurrentRequest(selectedPlant.id, generation) },
                                    request = { repository.nickname(selectedPlant.id, submittedName) },
                                    onStarted = { saving = true },
                                    onSuccess = {
                                        plants = plants.map { if (it.id == selectedPlant.id) it.copy(name = submittedName) else it }
                                        error = null
                                        showNameEditor = false
                                    },
                                    onFailure = { error = it.message },
                                    onFinished = { saving = false },
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                    ) { Text("Save name", color = white) }
                    }
                }

                if (spaces.any { it.id == selectedPlant.spaceId && it.canAddPlant }) {
                    PlantupiaPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("SHARED PLANT CONTEXT", color = muted, fontSize = 11.sp)
                                Text(
                                    listOf(
                                        selectedPlant.species.ifBlank { "Species not specified" },
                                        selectedPlant.placement.ifBlank { "Placement not specified" },
                                    ).joinToString(" · "),
                                    color = forestText, fontSize = 13.sp,
                                )
                            }
                            TextButton(
                                onClick = {
                                    showSharedDetailsEditor = !showSharedDetailsEditor
                                    if (showSharedDetailsEditor) {
                                        sharedPlantLabel = selectedPlant.label
                                        sharedSpecies = selectedPlant.species
                                        sharedPlacement = selectedPlant.placement
                                    }
                                },
                            ) {
                                Text(if (showSharedDetailsEditor) "Cancel" else "Edit", color = deepGreen)
                            }
                        }
                        if (showSharedDetailsEditor) {
                            Text(
                                "Changes here are visible to authorized space members and recorded in the plant's private history. Your personal nickname remains separate.",
                                color = muted, fontSize = 12.sp, lineHeight = 18.sp,
                            )
                            OutlinedTextField(
                                value = sharedPlantLabel,
                                onValueChange = { sharedPlantLabel = it.take(120) },
                                label = { Text("Shared plant name") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving,
                            )
                            OutlinedTextField(
                                value = sharedSpecies,
                                onValueChange = { sharedSpecies = it.take(180) },
                                label = { Text("Species (if known)") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving,
                            )
                            OutlinedTextField(
                                value = sharedPlacement,
                                onValueChange = { sharedPlacement = it.take(180) },
                                label = { Text("Placement in this space") },
                                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving,
                            )
                            Button(
                                enabled = !saving && sharedPlantLabel.isNotBlank() &&
                                    (sharedPlantLabel.trim() != selectedPlant.label ||
                                     sharedSpecies.trim() != selectedPlant.species ||
                                     sharedPlacement.trim() != selectedPlant.placement),
                                onClick = {
                                    val generation = detailRequestGeneration
                                    val submittedLabel = sharedPlantLabel
                                    val submittedSpecies = sharedSpecies
                                    val submittedPlacement = sharedPlacement
                                    val current = { isCurrentRequest(selectedPlant.id, generation) }
                                    scope.launch {
                                        runPlantupiaMutation(
                                            isAccountCurrent = lifetime::isCurrent,
                                            isDetailCurrent = current,
                                            request = { repository.updatePlantDetails(selectedPlant.id, submittedLabel, submittedSpecies, submittedPlacement) },
                                            onStarted = { saving = true },
                                            onSuccess = {
                                                showSharedDetailsEditor = false
                                                reloadPlantRecords(selectedPlant.id)
                                            },
                                            onFailure = { error = it.message },
                                            onFinished = { saving = false },
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = deepGreen),
                            ) { Text(if (saving) "Saving…" else "Save shared details", color = white) }
                        }
                    }
                }

                if (spaces.any { it.id == selectedPlant.spaceId && it.canAddPlant }) {
                    PlantupiaPanel {
                        Text("RECORD STATUS", color = muted, fontSize = 11.sp)
                        Text(
                            if (selectedPlant.active) "Keep a plant's history if it leaves your active space."
                            else "Restore this plant to resume new care and optional check-ins.",
                            color = forestText, fontSize = 12.sp,
                        )
                        OutlinedButton(
                            onClick = { pendingArchiveAction = selectedPlant.active },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (selectedPlant.active) "Archive plant" else "Restore plant",
                                color = deepGreen,
                            )
                        }
                    }
                }

            }
            // Keep optional public composition separate from everyday care.
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = { showPlantSharing = !showPlantSharing },
            ) {
                Text(
                    if (showPlantSharing) "Hide community options ↑" else "Community & sharing (optional) ↓",
                    color = deepGreen,
                )
            }
            if (showPlantSharing) {
                val questionGeneration = detailRequestGeneration
                PlantupiaQuestionsPanel(
                    isParentCurrent = { isCurrentRequest(selectedPlant.id, questionGeneration) },
                    requestGeneration = questionGeneration,
                    plantId = selectedPlant.id,
                    repository = repository,
                    compactHeader = true,
                    canPublish = selectedPlant.canRecord,
                    onOpenPost = { postId ->
                        onOpenLinkedPost(postId) { isCurrentRequest(selectedPlant.id, questionGeneration) }
                    },
                    onOutcomeSaved = {
                        val plantId = selectedPlant.id
                        val generation = questionGeneration
                        if (!isCurrentRequest(plantId, generation)) return@PlantupiaQuestionsPanel
                        val response = repository.historyPage(plantId)
                        if (!isCurrentRequest(plantId, generation)) return@PlantupiaQuestionsPanel
                        when (response) {
                            is ApiResult.Success -> {
                                events = response.value.events
                                nextHistoryOffset = response.value.nextOffset
                                historyLoadFailed = false
                                error = null
                            }
                            is ApiResult.Failure -> {
                                historyLoadFailed = true
                                error = response.message
                            }
                        }
                    },
                )
                TextButton(onClick = onCommunityCreateClick) {
                    Text("Create a separate public post →", color = deepGreen)
                }
                Text(
                    "Only the question, text and photo you approve are shared publicly. Private history stays in this plant.",
                    color = muted,
                    fontSize = 11.sp,
                )
            }
            } // End of the normal private plant detail (not the Ask Circle flow).
        }
    }
    photoToView?.let { (url, plantName) ->
        PlantupiaPhotoViewer(
            photoUrl = url,
            plantName = plantName,
            onClose = { photoToView = null },
        )
    }
    pendingArchiveAction?.let { archived ->
        selectedPlant?.let { plant ->
            AlertDialog(
                onDismissRequest = { if (!saving) pendingArchiveAction = null },
                title = {
                    Text(if (archived) "Archive ${plant.name}?" else "Restore ${plant.name}?")
                },
                text = {
                    Text(
                        if (archived) "This removes the plant from active care and stops its local reminders on this device. Private history, photos and original check-in dates remain. Nothing is deleted or marked watered."
                        else "The plant returns to active care. Historical check-in dates remain as recorded; review them before scheduling reminders.",
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            pendingArchiveAction = null
                            val generation = detailRequestGeneration
                            val current = { isCurrentRequest(plant.id, generation) }
                            scope.launch {
                                runPlantupiaMutation(
                                    isAccountCurrent = lifetime::isCurrent,
                                    isDetailCurrent = current,
                                    request = { repository.setPlantArchived(plant.id, archived) },
                                    onStarted = { saving = true },
                                    onCommitted = {
                                        if (archived) PlantupiaCareReminderScheduler.cancelForPlant(context, plant.id)
                                    },
                                    onSuccess = {
                                        if (archived) { showCareEditor = false; followUpTarget = null }
                                        reloadPlantRecords(plant.id)
                                    },
                                    onFailure = { error = it.message },
                                    onFinished = { saving = false },
                                )
                            }
                        },
                    ) { Text(if (archived) "Archive" else "Restore") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingArchiveAction = null }) { Text("Cancel") }
                },
            )
        }
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
    "care" -> "Care recorded"
    "issue" -> "Observation needs attention"
    "outcome" -> "Documented outcome"
    else -> "Observation recorded"
}

private fun evidenceLabel(kind: String): String = when (kind) {
    "measured" -> "Measurement"
    "reported" -> "Reported by someone else"
    "inferred" -> "Unverified inference"
    else -> "Direct observation"
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
