package com.nova.app.feature.plants

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.ui.components.PlantupiaAvatar
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaQuestion
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import android.net.Uri

private val green = Color(0xFF123B2E)
private val mutedGreen = Color(0xFF4A5A50)
private val parchment = Color(0xFFF3EEDE)
private val card = Color(0xFFFFFDF7)

/** A new Circle refresh invalidates earlier in-flight pagination and retries. */
internal fun isCurrentCirclePageRequest(requestRevision: Int, visibleRevision: Int): Boolean =
    requestRevision == visibleRevision

@Composable
fun PlantupiaQuestionsPanel(
    plantId: String,
    repository: PlantupiaRepository,
    canPublish: Boolean,
    onOpenPost: (Long) -> Unit,
    onOutcomeSaved: suspend () -> Unit,
    compactHeader: Boolean = false,
    isParentCurrent: () -> Boolean = { true },
    requestGeneration: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val lifetime = remember(plantId, repository, requestGeneration) {
        PlantupiaCallbackLifetime { repository.isCurrentSession() && isParentCurrent() }
    }
    DisposableEffect(lifetime) { onDispose { lifetime.dispose() } }
    var savedQuestion by remember(plantId, repository) { mutableStateOf<com.nova.app.feature.plants.data.PlantupiaSubmission?>(null) }
    var question by remember(plantId) { mutableStateOf("") }
    var topic by remember(plantId) { mutableStateOf("") }
    var imageUri by remember(plantId) { mutableStateOf<Uri?>(null) }
    var consent by remember(plantId) { mutableStateOf(false) }
    var reviewOpen by remember(plantId) { mutableStateOf(false) }
    var submitting by remember(plantId, requestGeneration) { mutableStateOf(false) }
    var loading by remember(plantId, requestGeneration) { mutableStateOf(true) }
    var error by remember(plantId, requestGeneration) { mutableStateOf<String?>(null) }
    var linkedPosts by remember(plantId, requestGeneration) { mutableStateOf<List<PlantupiaQuestion>>(emptyList()) }
    var respondingTo by remember(plantId) { mutableStateOf<String?>(null) }
    var outcome by remember(plantId) { mutableStateOf("") }
    val publicDraft = PlantupiaPublicShareDraft(topic, question, imageUri != null)
    var pickerGeneration by remember(plantId) { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (lifetime.isCurrent() && pickerGeneration == requestGeneration && uri != null) {
            imageUri = uri
            // Selecting a different file must require renewed public consent.
            consent = false
            reviewOpen = false
        }
        pickerGeneration = null
    }

    suspend fun refreshQuestions() {
        if (!lifetime.isCurrent()) return
        loading = true
        val response = repository.plantQuestions(plantId)
        if (!lifetime.isCurrent()) return
        when (response) {
            is ApiResult.Failure -> error = response.message
            is ApiResult.Success -> { linkedPosts = response.value; error = null }
        }
        loading = false
    }

    LaunchedEffect(plantId, requestGeneration) { refreshQuestions() }
    LaunchedEffect(plantId, submitting, requestGeneration) {
        if (!submitting) when (val pending = repository.pendingSubmissions(plantId)) {
            is ApiResult.Success -> if (lifetime.isCurrent()) savedQuestion = pending.value.firstOrNull { it.operation == "question" }
            is ApiResult.Failure -> if (lifetime.isCurrent()) error = pending.message
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!compactHeader) {
            Text("PLANTUPIA CIRCLE", color = green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Text("Ask the Circle about your plant", color = green, fontFamily = FontFamily.Serif, fontSize = 22.sp)
            Text(
                "Share a specific question. Review the exact text and image before it becomes public.",
                color = mutedGreen, fontSize = 12.sp, lineHeight = 18.sp,
            )
        }
        if (canPublish) {
        savedQuestion?.let { saved ->
            Text("An earlier question is awaiting confirmation. Review that exact submission before retrying; it won't publish another question.", color = mutedGreen, fontSize = 12.sp)
            OutlinedButton(enabled = !submitting, onClick = {
                topic = saved.fields["public_topic"].orEmpty()
                question = saved.fields["caption"].orEmpty()
                imageUri = saved.photo?.let(Uri::fromFile)
                consent = false
                error = null
                reviewOpen = true
            }) { Text("Review saved question") }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = topic,
            onValueChange = { topic = it.take(80); consent = false; reviewOpen = false },
            label = { Text("Public question title — leave out private locations") },
            singleLine = true,
            enabled = !submitting,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = question,
            onValueChange = { question = it.take(500); consent = false; reviewOpen = false },
            label = { Text("What happened, and what have you tried?") },
            minLines = 2,
            enabled = !submitting,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting,
            onClick = { pickerGeneration = requestGeneration; picker.launch("image/*") },
        ) {
            Text(if (imageUri == null) "Choose a photo to publish (currently required)" else "Change the public photo", color = green)
        }
        if (imageUri != null) {
            Surface(color = card, shape = RoundedCornerShape(15.dp)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("PUBLIC PREVIEW", color = green, fontSize = 11.sp)
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Preview of the exact photo that will be published",
                        modifier = Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Text(topic.ifBlank { "Question title" }, color = green, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(question.ifBlank { "Your public question" }, color = mutedGreen, fontSize = 12.sp)
                }
            }
        }
        Text(
            "The photo, title and question will be public. Check that neither your words nor the image reveals where you live or work.",
            color = mutedGreen, fontSize = 12.sp, lineHeight = 19.sp,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting && publicDraft.canReview,
            colors = ButtonDefaults.buttonColors(containerColor = green),
            onClick = { consent = false; error = null; reviewOpen = true },
        ) { Text("Review before publishing →", color = card) }
        } else {
            Text("You can browse questions and replies. Publishing requires care permission for this space.", color = mutedGreen, fontSize = 12.sp)
        }
        if (canPublish && reviewOpen) {
            AlertDialog(
                onDismissRequest = {
                    if (!submitting) { reviewOpen = false; consent = false }
                },
                title = {
                    Text("Before sharing to Circle", color = green, fontFamily = FontFamily.Serif, fontSize = 23.sp)
                },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "This is public content, not an entry in your private care history.",
                            color = mutedGreen, fontSize = 12.sp,
                        )
                        imageUri?.let { selected ->
                            AsyncImage(
                                model = selected,
                                contentDescription = "Selected public photo. Check its contents carefully.",
                                modifier = Modifier.fillMaxWidth().height(196.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Text(publicDraft.cleanTopic, color = green, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(publicDraft.cleanQuestion, color = mutedGreen, fontSize = 14.sp, lineHeight = 21.sp)
                        Text(
                            "Your location may be visible in the image or text. Space details and private care history are never attached automatically; public photo EXIF is removed by the server.",
                            color = mutedGreen, fontSize = 12.sp, lineHeight = 19.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(
                                checked = consent,
                                onCheckedChange = { consent = it },
                                enabled = !submitting,
                            )
                            Text(
                                "I reviewed the photo, title and question, and agree to publish this content publicly.",
                                color = green, fontSize = 12.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f).padding(top = 10.dp),
                            )
                        }
                        if (error != null) {
                            Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !submitting && consent && publicDraft.canReview && imageUri != null,
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                        onClick = {
                            val selected = imageUri ?: return@Button
                            val submittedTopic = publicDraft.cleanTopic
                            val submittedQuestion = publicDraft.cleanQuestion
                            scope.launch {
                                runPlantupiaMutation(
                                    isAccountCurrent = lifetime::isCurrent,
                                    isDetailCurrent = lifetime::isCurrent,
                                    request = { repository.askWithPhoto(plantId, submittedTopic, submittedQuestion, selected) },
                                    onStarted = { submitting = true; error = null },
                                    onSuccess = { postId ->
                                        repository.confirmSubmission(plantId, "question", postId.toString())
                                        if (!lifetime.isCurrent()) return@runPlantupiaMutation
                                        savedQuestion = null
                                        question = ""
                                        topic = ""
                                        imageUri = null
                                        consent = false
                                        reviewOpen = false
                                        refreshQuestions()
                                        if (lifetime.isCurrent() && postId > 0) onOpenPost(postId)
                                    },
                                    onFailure = { error = it.message },
                                    onFinished = { submitting = false },
                                )
                            }
                        },
                    ) { Text(if (submitting) "Publishing…" else "Agree and publish", color = card) }
                },
                dismissButton = {
                    TextButton(
                        enabled = !submitting,
                        onClick = { reviewOpen = false; consent = false },
                    ) { Text("Go back and edit", color = green) }
                },
                containerColor = card,
            )
        }
        if (error != null && !reviewOpen) {
            Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
        }
        HorizontalDivider(color = Color(0xFF9FB79A))
        Text("Questions about this plant", color = green, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        if (loading) CircularProgressIndicator(color = green)
        if (!loading && linkedPosts.isEmpty()) Text("Questions you share about this plant will appear here.", color = mutedGreen, fontSize = 12.sp)
        linkedPosts.forEach { entry ->
            QuestionCard(entry = entry, onClick = { onOpenPost(entry.postId) })
            if (canPublish && entry.isMine && !entry.resolved) {
                if (respondingTo == entry.id) {
                    OutlinedTextField(
                        value = outcome,
                        onValueChange = { outcome = it.take(1500) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What actually happened after the advice?") },
                    )
                    Button(
                        enabled = outcome.isNotBlank() && !submitting,
                        onClick = {
                            val submittedOutcome = outcome
                            val discussionId = entry.id
                            scope.launch {
                                runPlantupiaMutation(
                                    isAccountCurrent = lifetime::isCurrent,
                                    isDetailCurrent = lifetime::isCurrent,
                                    request = { repository.resolveQuestion(plantId, discussionId, submittedOutcome) },
                                    onStarted = { submitting = true },
                                    onSuccess = {
                                        respondingTo = null
                                        outcome = ""
                                        refreshQuestions()
                                        if (lifetime.isCurrent()) onOutcomeSaved()
                                    },
                                    onFailure = { error = it.message },
                                    onFinished = { submitting = false },
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                    ) { Text("Save outcome to plant history", color = card) }
                } else {
                    TextButton(onClick = { respondingTo = entry.id; outcome = "" }) {
                        Text(if (entry.comments == 0) "Record what changed later" else "Record the outcome after getting help", color = green)
                    }
                }
            }
        }
    }
}

@Composable
fun PlantupiaCirclePanel(onOpenPost: (Long) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(context) { PlantupiaRepository(context.applicationContext) }
    var questions by remember { mutableStateOf<List<PlantupiaQuestion>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    var olderPageError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        val requestRevision = refresh
        pending = true
        // Never leave a previous session's public-feed snapshot on a failed reload.
        // A refresh also invalidates the cursor used by any older loading page.
        questions = emptyList()
        nextCursor = null
        loadingMore = false
        olderPageError = null
        error = null
        val result = repo.circle()
        if (!isCurrentCirclePageRequest(requestRevision, refresh)) return@LaunchedEffect
        when (result) {
            is ApiResult.Success -> {
                questions = result.value.questions
                nextCursor = result.value.nextCursor
                error = null
            }
            is ApiResult.Failure -> error = result.message
        }
        pending = false
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("PLANT QUESTIONS", color = green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            if (!pending && error == null && questions.isNotEmpty()) {
                TextButton(onClick = { refresh++ }) {
                    Text("Refresh", color = green, fontSize = 12.sp)
                }
            }
        }
        if (pending) {
            Surface(color = parchment, shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    CircularProgressIndicator(color = green, modifier = Modifier.size(28.dp))
                    Text("Loading Circle questions…", color = mutedGreen, fontSize = 13.sp)
                }
            }
        }
        if (error != null) {
            Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
            TextButton(onClick = { refresh++ }) { Text("Try again", color = green) }
        }
        if (!pending && error == null && questions.isEmpty()) {
            Surface(color = parchment, shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(17.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PlantupiaBrandGlyph(modifier = Modifier.size(48.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No questions yet", color = green, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Start with a plant, choose a photo and question, then preview what will be public. Real questions will appear as they're published.",
                            color = mutedGreen, fontSize = 12.sp, lineHeight = 19.sp,
                        )
                    }
                }
            }
        }
        questions.forEach { q -> QuestionCard(q, onClick = { onOpenPost(q.postId) }) }
        if (olderPageError != null) {
            Text(
                "Couldn't load older questions: " + olderPageError.orEmpty(),
                color = Color(0xFF9F482B), fontSize = 12.sp,
            )
        }
        if (nextCursor != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingMore,
                onClick = {
                    val cursor = nextCursor ?: return@OutlinedButton
                    val requestRevision = refresh
                    scope.launch {
                        loadingMore = true
                        val result = repo.circle(cursor)
                        // A late response belongs to an older Circle list. Never
                        // append it to the freshly loaded questions or revive its cursor.
                        if (!isCurrentCirclePageRequest(requestRevision, refresh)) return@launch
                        when (result) {
                            is ApiResult.Failure -> olderPageError = result.message
                            is ApiResult.Success -> {
                                questions = (questions + result.value.questions).distinctBy { it.id }
                                nextCursor = result.value.nextCursor
                                olderPageError = null
                            }
                        }
                        loadingMore = false
                    }
                },
            ) {
                Text(
                    if (loadingMore) "Loading more…" else if (olderPageError != null) "Retry older questions" else "Load older questions",
                    color = green,
                )
            }
        }
    }
}

/**
 * Compact question-first content: the public image is optional supporting evidence,
 * not a full-width feed hero. A documented outcome is shown only when the API says
 * it has been deliberately recorded by the plant's author.
 */
internal fun circleReplyLabel(count: Int): String = when (count.coerceAtLeast(0)) {
    1 -> "1 reply"
    else -> "${count.coerceAtLeast(0)} replies"
}

/** A removed/unknown account must not retain an identity-specific avatar. */
internal fun canDisplayQuestionAuthorPortrait(entry: PlantupiaQuestion): Boolean =
    entry.authorId > 0L && !entry.authorDeleted

@Composable
private fun QuestionCard(entry: PlantupiaQuestion, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = card,
        border = BorderStroke(1.dp, Color(0xFFD8E1D5)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (canDisplayQuestionAuthorPortrait(entry)) {
                        PlantupiaAvatar(
                            userId = entry.authorId,
                            displayName = entry.authorName,
                            photoUrl = entry.authorAvatarUrl,
                            chosenVariant = entry.authorAvatarVariant,
                            size = 40.dp,
                        )
                    } else {
                        // Deleted/unknown authors must not inherit a stable
                        // identity silhouette from a former account ID.
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = PlantupiaColors.softSage,
                        ) {
                            Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("—", color = PlantupiaColors.secondaryText)
                            }
                        }
                    }
                    Text(
                        entry.authorName.ifBlank { "Circle member" },
                        color = green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                if (entry.resolved) {
                    Text(
                        "Resolved",
                        color = green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        entry.topic,
                        color = green,
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    if (entry.caption.isNotBlank()) {
                        Text(
                            entry.caption,
                            color = mutedGreen,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
                if (entry.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = entry.imageUrl,
                        contentDescription = "Photo shared for question: ${entry.topic}",
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFD8E1D5))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(circleReplyLabel(entry.comments), color = green, fontSize = 12.sp)
                Text("Read discussion →", color = green, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
