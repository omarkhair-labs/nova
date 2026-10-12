package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaAssistantTurn
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import kotlinx.coroutines.launch

private val forest = Color(0xFF123B2E)
private val mist = Color(0xFFEEF1E8)
private val paper = Color(0xFFFFFDF7)
private val muted = Color(0xFF4A5A50)

private data class CareChatBubble(
    val text: String,
    val fromUser: Boolean,
    val scope: String = "",
)

/**
 * Authentic AI provider flow, NOT a scripted demo. The server is read-only and
 * disabled until a secured API account is configured. Each message explicitly
 * opts in or out of sharing the user's authorized plant data.
 *
 * Human-to-human Inbox remains Nova's existing messaging system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantupiaAssistantSheet(
    plant: PlantupiaPlant?,
    repository: PlantupiaRepository,
    onDismiss: () -> Unit,
) {
    var input by remember(plant?.id) { mutableStateOf("") }
    var includeContext by remember(plant?.id) { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var bubbles by remember(plant?.id) { mutableStateOf(emptyList<CareChatBubble>()) }
    val scope = rememberCoroutineScope()
    val chatListState = rememberLazyListState()
    LaunchedEffect(bubbles.size) {
        if (bubbles.isNotEmpty()) chatListState.animateScrollToItem(bubbles.lastIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = paper,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaIcon(
                    asset = NovaIconAsset.Info,
                    contentDescription = null,
                    tint = forest,
                )
                Text(if (plant == null) "اسأل مساعد Plantupia" else "اسأل عن " + plant.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = forest)
            }
            Text(
                "مساعد نباتات بالذكاء الاصطناعي. بيقترح أسئلة وفحوصات، مش تشخيص مؤكد أو خدمة دعم من الشركة.",
                fontSize = 12.sp, color = muted, lineHeight = 20.sp,
            )
            if (plant != null) Surface(
                color = mist,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Color(0xFFD4DFD1)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("استخدم تاريخ نباتي في الإجابة", color = forest, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "اختياري لكل سؤال: الاسم والنوع والمكان وآخر ملاحظاتك فقط. صور النبات وموقع منزلك مش بيتبعتوا.",
                            color = muted, fontSize = 11.sp, lineHeight = 17.sp,
                        )
                    }
                    Switch(
                        checked = includeContext,
                        enabled = !pending,
                        onCheckedChange = {
                            includeContext = it
                            // Do not replay a previous AI answer containing plant details
                            // when this person changes their context-sharing preference.
                            bubbles = emptyList()
                            error = null
                        },
                    )
                }
            }
            LazyColumn(
                state = chatListState,
                modifier = Modifier.fillMaxWidth().height(215.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                reverseLayout = false,
            ) {
                if (bubbles.isEmpty()) {
                    item {
                        Text(
                            "إيه التغيّر اللي لاحظته؟ احكي مثلًا عن لون الورق، الإضاءة، آخر ريّة، وهل التربة ناشفة أو رطبة.",
                            color = muted, fontSize = 14.sp, lineHeight = 23.sp,
                        )
                    }
                }
                itemsIndexed(bubbles) { _, item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (item.fromUser) mist else Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = if (item.fromUser) null else BorderStroke(1.dp, Color(0xFFD4DFD1)),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = if (item.fromUser) "سؤالك" else
                                    "إجابة AI · " + (if (item.scope == "plant_context") "مع بيانات النبات" else "معلومات عامة"),
                                color = forest, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            )
                            Text(item.text, color = forest, fontSize = 13.sp, lineHeight = 21.sp)
                        }
                    }
                }
            }
            error?.let {
                Surface(color = Color(0xFFFFEDE9), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(it, color = Color(0xFF8F3023), fontSize = 12.sp)
                    }
                }
            }
            if (pending) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = forest)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(1000) },
                label = { Text(if (plant == null) "اسأل عن أي نبات…" else "اسأل عن النبات…") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !pending,
                minLines = 2,
                maxLines = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onDismiss, enabled = !pending) {
                    Text("إغلاق", color = muted)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = !pending && input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = forest),
                    onClick = {
                        val question = input.trim()
                        if (question.isEmpty()) return@Button
                        input = ""
                        error = null
                        val consent = includeContext
                        val dialogue = bubbles.takeLast(8).map {
                            PlantupiaAssistantTurn(
                                role = if (it.fromUser) "user" else "assistant",
                                content = it.text.take(1200),
                            )
                        }
                        bubbles = bubbles + CareChatBubble(question, fromUser = true)
                        pending = true
                        scope.launch {
                            when (val result = repository.askAssistant(
                                message = question,
                                plantId = plant?.id,
                                includeContext = consent && plant != null,
                                conversation = dialogue,
                            )) {
                                is ApiResult.Success -> {
                                    bubbles = bubbles + CareChatBubble(
                                        result.value.answer, fromUser = false,
                                        scope = result.value.scope,
                                    )
                                }
                                is ApiResult.Failure -> {
                                    error = if (result.statusCode == 503)
                                        "المساعد لسه مش مُفعّل على السيرفر أو غير متاح حاليًا. سؤالك ما اتحفظش في سجل النبات."
                                    else result.message
                                }
                            }
                            pending = false
                        }
                    },
                ) {
                    Text(if (pending) "جارٍ السؤال…" else "إرسال السؤال", color = paper)
                }
            }
            Text(
                "الردود دي إرشادية ومش بتُسجّل رعاية تلقائيًا. المساعد مش بيقرأ Sensors لأن Plantupia مفيهاش حساسات نباتات.",
                color = muted, fontSize = 10.sp, lineHeight = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
