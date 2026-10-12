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
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaQuestion
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import android.net.Uri

private val green = Color(0xFF123B2E)
private val mutedGreen = Color(0xFF4A5A50)
private val parchment = Color(0xFFF3EEDE)
private val card = Color(0xFFFFFDF7)

@Composable
fun PlantupiaQuestionsPanel(
    plantId: String,
    repository: PlantupiaRepository,
    canPublish: Boolean,
    onOpenPost: (Long) -> Unit,
    onOutcomeSaved: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var question by remember(plantId) { mutableStateOf("") }
    var topic by remember(plantId) { mutableStateOf("") }
    var imageUri by remember(plantId) { mutableStateOf<Uri?>(null) }
    var consent by remember(plantId) { mutableStateOf(false) }
    var reviewOpen by remember(plantId) { mutableStateOf(false) }
    var submitting by remember(plantId) { mutableStateOf(false) }
    var loading by remember(plantId) { mutableStateOf(true) }
    var error by remember(plantId) { mutableStateOf<String?>(null) }
    var linkedPosts by remember(plantId) { mutableStateOf<List<PlantupiaQuestion>>(emptyList()) }
    var respondingTo by remember(plantId) { mutableStateOf<String?>(null) }
    var outcome by remember(plantId) { mutableStateOf("") }
    val publicDraft = PlantupiaPublicShareDraft(topic, question, imageUri != null)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            // Selecting a different file must require renewed public consent.
            consent = false
            reviewOpen = false
        }
    }

    suspend fun refreshQuestions() {
        loading = true
        when (val response = repository.plantQuestions(plantId)) {
            is ApiResult.Failure -> error = response.message
            is ApiResult.Success -> { linkedPosts = response.value; error = null }
        }
        loading = false
    }

    LaunchedEffect(plantId) { refreshQuestions() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PLANTUPIA CIRCLE", color = green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Text("اسأل الناس عن نباتك", color = green, fontFamily = FontFamily.Serif, fontSize = 22.sp)
        Text(
            "صوّر اللي لاحظته. الخبرة هنا مرتبطة بنبات حقيقي، وبعد الرد ارجع سجّل إيه اللي حصل.",
            color = mutedGreen, fontSize = 13.sp, lineHeight = 20.sp,
        )
        if (canPublish) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = topic,
            onValueChange = { topic = it.take(80); consent = false; reviewOpen = false },
            label = { Text("عنوان عام للسؤال — بدون اسم الكافيه أو عنوانه") },
            singleLine = true,
            enabled = !submitting,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = question,
            onValueChange = { question = it.take(500); consent = false; reviewOpen = false },
            label = { Text("إيه اللي حصل؟ وإيه اللي جربته؟") },
            minLines = 2,
            enabled = !submitting,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting,
            onClick = { picker.launch("image/*") },
        ) {
            Text(if (imageUri == null) "أضف صورة للنشر العام (مطلوبة حاليًا)" else "غيّر الصورة اللي هتظهر للناس", color = green)
        }
        if (imageUri != null) {
            Surface(color = card, shape = RoundedCornerShape(15.dp)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("معاينة النشر العام / PUBLIC PREVIEW", color = green, fontSize = 11.sp)
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "معاينة الصورة التي ستنشر للعامة",
                        modifier = Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Text(topic.ifBlank { "عنوان السؤال" }, color = green, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(question.ifBlank { "السؤال اللي هينشر" }, color = mutedGreen, fontSize = 12.sp)
                }
            }
        }
        Text(
            "صورتك وعنوان السؤال والنص المكتوب هيظهروا للناس. راجع بعناية أي تفاصيل تكشف عنوانك أو مكانك داخل الصورة نفسها.",
            color = mutedGreen, fontSize = 12.sp, lineHeight = 19.sp,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting && publicDraft.canReview,
            colors = ButtonDefaults.buttonColors(containerColor = green),
            onClick = { consent = false; error = null; reviewOpen = true },
        ) { Text("راجع المحتوى قبل النشر ←", color = card) }
        } else {
            Text("يمكنك قراءة الأسئلة والردود. النشر مرتبط بصلاحية الرعاية لهذه المساحة.", color = mutedGreen, fontSize = 12.sp)
        }
        if (canPublish && reviewOpen) {
            AlertDialog(
                onDismissRequest = {
                    if (!submitting) { reviewOpen = false; consent = false }
                },
                title = {
                    Text("قبل النشر في الدائرة", color = green, fontFamily = FontFamily.Serif, fontSize = 23.sp)
                },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "ده محتوى عام، مش إضافة لسجل الرعاية الخاص.",
                            color = mutedGreen, fontSize = 12.sp,
                        )
                        imageUri?.let { selected ->
                            AsyncImage(
                                model = selected,
                                contentDescription = "الصورة المختارة للنشر العام، راجع محتواها بعناية",
                                modifier = Modifier.fillMaxWidth().height(196.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Text(publicDraft.cleanTopic, color = green, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(publicDraft.cleanQuestion, color = mutedGreen, fontSize = 14.sp, lineHeight = 21.sp)
                        Text(
                            "قد يظهر مكانك من تفاصيل الصورة أو الكلام. بيانات المساحة وسجلها لا تُضاف تلقائيًا، والصورة العامة تُزال منها بيانات EXIF على السيرفر.",
                            color = mutedGreen, fontSize = 12.sp, lineHeight = 19.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(
                                checked = consent,
                                onCheckedChange = { consent = it },
                                enabled = !submitting,
                            )
                            Text(
                                "راجعت الصورة والعنوان والسؤال وأوافق على نشر المحتوى الظاهر للناس.",
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
                            scope.launch {
                                submitting = true
                                error = null
                                when (val result = repository.askWithPhoto(
                                    plantId, publicDraft.cleanTopic, publicDraft.cleanQuestion, selected,
                                )) {
                                    is ApiResult.Failure -> error = result.message
                                    is ApiResult.Success -> {
                                        question = ""
                                        topic = ""
                                        imageUri = null
                                        consent = false
                                        reviewOpen = false
                                        refreshQuestions()
                                        if (result.value > 0) onOpenPost(result.value)
                                    }
                                }
                                submitting = false
                            }
                        },
                    ) { Text(if (submitting) "جارٍ النشر…" else "أوافق وأنشر", color = card) }
                },
                dismissButton = {
                    TextButton(
                        enabled = !submitting,
                        onClick = { reviewOpen = false; consent = false },
                    ) { Text("ارجع وعدّل", color = green) }
                },
                containerColor = card,
            )
        }
        if (error != null && !reviewOpen) {
            Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
        }
        HorizontalDivider(color = Color(0xFF9FB79A))
        Text("أسئلة مرتبطة بالنبات", color = green, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        if (loading) CircularProgressIndicator(color = green)
        if (!loading && linkedPosts.isEmpty()) Text("لما تسأل عن النبات، الأسئلة هتفضل محفوظة هنا.", color = mutedGreen, fontSize = 12.sp)
        linkedPosts.forEach { entry ->
            QuestionCard(entry = entry, onClick = { onOpenPost(entry.postId) })
            if (canPublish && entry.isMine && !entry.resolved) {
                if (respondingTo == entry.id) {
                    OutlinedTextField(
                        value = outcome,
                        onValueChange = { outcome = it.take(1500) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("إيه اللي حصل بعد النصيحة؟") },
                    )
                    Button(
                        enabled = outcome.isNotBlank() && !submitting,
                        onClick = {
                            submitting = true
                            scope.launch {
                                when (val result = repository.resolveQuestion(plantId, entry.id, outcome)) {
                                    is ApiResult.Failure -> error = result.message
                                    is ApiResult.Success -> {
                                        respondingTo = null
                                        outcome = ""
                                        refreshQuestions()
                                        onOutcomeSaved()
                                    }
                                }
                                submitting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = green),
                    ) { Text("احفظ النتيجة في تاريخ النبات", color = card) }
                } else {
                    TextButton(onClick = { respondingTo = entry.id; outcome = "" }) {
                        Text("وثّق النتيجة بعد المساعدة", color = green)
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
        pending = true
        olderPageError = null
        when (val result = repo.circle()) {
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
        Text("PLANTUPIA / SHARED KNOWLEDGE", color = green, fontSize = 10.sp, letterSpacing = 1.7.sp)
        Text("الخبرة اللي بتفضل.", fontFamily = FontFamily.Serif, fontSize = 27.sp, color = green)
        Text(
            "سؤال وصورة من نبات حقيقي، وردود من الناس، وبعدها متابعة للنتيجة. من غير كشف المساحة الخاصة.",
            color = mutedGreen, fontSize = 13.sp, lineHeight = 20.sp,
        )
        if (pending) {
            Surface(color = parchment, shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    CircularProgressIndicator(color = green, modifier = Modifier.size(28.dp))
                    Text("بنحمّل أسئلة الدائرة…", color = mutedGreen, fontSize = 13.sp)
                }
            }
        }
        if (error != null) {
            Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
            TextButton(onClick = { refresh++ }) { Text("حاول تاني", color = green) }
        }
        if (!pending && error == null && questions.isEmpty()) {
            Surface(color = parchment, shape = RoundedCornerShape(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(17.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PlantupiaBrandGlyph(modifier = Modifier.size(48.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("أول سؤال لسه ما اتنشرش", color = green, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "ابدأ من نباتك، اختار صورة وسؤال، وراجع اللي هتشاركه. أسئلة الناس بتظهر هنا لما تتنشر فعلًا.",
                            color = mutedGreen, fontSize = 12.sp, lineHeight = 19.sp,
                        )
                    }
                }
            }
        }
        questions.forEach { q -> QuestionCard(q, onClick = { onOpenPost(q.postId) }) }
        if (olderPageError != null) {
            Text(
                "تعذّر تحميل الأسئلة الأقدم: " + olderPageError.orEmpty(),
                color = Color(0xFF9F482B), fontSize = 12.sp,
            )
        }
        if (nextCursor != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !loadingMore,
                onClick = {
                    val cursor = nextCursor ?: return@OutlinedButton
                    scope.launch {
                        loadingMore = true
                        when (val result = repo.circle(cursor)) {
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
                    if (loadingMore) "جارٍ تحميل المزيد…" else if (olderPageError != null) "حاول تحميل الأسئلة الأقدم تاني" else "اعرض أسئلة أقدم",
                    color = green,
                )
            }
        }
        if (questions.isNotEmpty()) TextButton(onClick = { refresh++ }) { Text("تحديث الأسئلة", color = green) }
    }
}

@Composable
private fun QuestionCard(entry: PlantupiaQuestion, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = card,
        border = BorderStroke(1.dp, Color(0xFFD8E1D5)),
        shape = RoundedCornerShape(19.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "FROM REAL PLANTS / من تجربة حقيقية",
                    color = green,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (entry.resolved) {
                    Text(
                        "النتيجة اتوثّقت ✓",
                        color = green,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                entry.topic,
                color = green,
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
            if (entry.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = "صورة السؤال: ${entry.topic}",
                    modifier = Modifier.fillMaxWidth().height(208.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            if (entry.caption.isNotBlank()) {
                Text(
                    entry.caption,
                    color = mutedGreen,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                )
            }
            HorizontalDivider(color = Color(0xFFD8E1D5))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    entry.authorName.ifBlank { "شخص من الدائرة" },
                    color = mutedGreen,
                    fontSize = 12.sp,
                )
                Text("${entry.comments} ردود · افتح النقاش ←", color = green, fontSize = 12.sp)
            }
        }
    }
}
