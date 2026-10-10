package com.nova.app.feature.plants

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var submitting by remember(plantId) { mutableStateOf(false) }
    var loading by remember(plantId) { mutableStateOf(true) }
    var error by remember(plantId) { mutableStateOf<String?>(null) }
    var linkedPosts by remember(plantId) { mutableStateOf<List<PlantupiaQuestion>>(emptyList()) }
    var respondingTo by remember(plantId) { mutableStateOf<String?>(null) }
    var outcome by remember(plantId) { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
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
            onValueChange = { topic = it.take(80) },
            label = { Text("عنوان عام للسؤال — بدون اسم الكافيه أو عنوانه") },
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = question,
            onValueChange = { question = it.take(500) },
            label = { Text("إيه اللي حصل؟ وإيه اللي جربته؟") },
            minLines = 2,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { picker.launch("image/*") },
        ) {
            Text(if (imageUri == null) "أضف صورة السؤال" else "تم اختيار الصورة — اضغط للتغيير", color = green)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text(
                "أوافق على نشر الصورة والعنوان والسؤال للناس. تفاصيل مساحتي وسجل نباتي الخاص لن تُنشر.",
                modifier = Modifier.weight(1f).padding(top = 9.dp),
                color = mutedGreen, fontSize = 12.sp, lineHeight = 18.sp,
            )
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting && consent && imageUri != null && topic.trim().isNotBlank() && question.trim().isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = green),
            onClick = {
                val selected = imageUri ?: return@Button
                scope.launch {
                    submitting = true
                    when (val r = repository.askWithPhoto(plantId, topic, question, selected)) {
                        is ApiResult.Failure -> error = r.message
                        is ApiResult.Success -> {
                            question = ""; topic = ""; imageUri = null; consent = false
                            refreshQuestions()
                            if (r.value > 0) onOpenPost(r.value)
                        }
                    }
                    submitting = false
                }
            },
        ) { Text(if (submitting) "جارٍ نشر السؤال…" else "انشر في الدائرة", color = card) }
        } else {
            Text("يمكنك قراءة الأسئلة والردود. النشر مرتبط بصلاحية الرعاية لهذه المساحة.", color = mutedGreen, fontSize = 12.sp)
        }
        if (error != null) Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
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
    var pending by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        pending = true
        when (val result = repo.circle()) {
            is ApiResult.Success -> { questions = result.value; error = null }
            is ApiResult.Failure -> error = result.message
        }
        pending = false
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(parchment, RoundedCornerShape(22.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("CIRCLE / PLANTUPIA", color = green, fontSize = 10.sp, letterSpacing = 1.7.sp)
        Text("A little knowledge grows.", fontFamily = FontFamily.Serif, fontSize = 24.sp, color = green)
        Text("أسئلة وتجارب من الناس ونباتاتهم. الإجابات بتفضل مرتبطة بالمتابعة الحقيقية.", color = mutedGreen, fontSize = 13.sp, lineHeight = 20.sp)
        if (pending) CircularProgressIndicator(color = green)
        if (error != null) {
            Text(error.orEmpty(), color = Color(0xFF9F482B), fontSize = 12.sp)
            TextButton(onClick = { refresh++ }) { Text("حاول تاني", color = green) }
        }
        if (!pending && error == null && questions.isEmpty()) {
            Text("الدائرة لسه في أولها. أول سؤال هيظهر هنا.", color = mutedGreen, fontSize = 13.sp)
        }
        questions.take(8).forEach { q -> QuestionCard(q, onClick = { onOpenPost(q.postId) }) }
        if (questions.isNotEmpty()) TextButton(onClick = { refresh++ }) { Text("تحديث الأسئلة", color = green) }
    }
}

@Composable
private fun QuestionCard(entry: PlantupiaQuestion, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = card,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (entry.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = entry.imageUrl,
                    contentDescription = "صورة السؤال",
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(entry.topic, color = green, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(entry.caption, color = mutedGreen, fontSize = 13.sp, maxLines = 3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.authorName, color = mutedGreen, fontSize = 11.sp)
                Text("${entry.comments} ردود", color = mutedGreen, fontSize = 11.sp)
                if (entry.resolved) Text("تمت المتابعة ✓", color = green, fontSize = 11.sp)
            }
            Text("افتح السؤال والردود ←", color = green, fontSize = 12.sp)
        }
    }
}
