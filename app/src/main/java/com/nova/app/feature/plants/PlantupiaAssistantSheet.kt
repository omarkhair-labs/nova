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
 * Live provider conversation. Consent to include a particular plant's context is
 * per-question; a disabled toggle clears prior context-bearing bubbles.
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
        containerColor = PlantupiaColors.paper,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlantupiaConversationHeader(
                category = "AI · PLANT CARE",
                title = "Plantupia AI",
                subtitle = if (plant == null) "Ask about plant care" else "Here for " + plant.name,
                onClose = onDismiss,
            )
            PlantupiaDemoAccessGate(repository, human = false) { accessLost ->
                if (plant != null) Surface(
                    color = PlantupiaColors.softSage,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Share plant context", style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                            Text("Optional for each question · name, placement and recent notes. No photos or address.", style = PlantupiaText.supporting)
                        }
                        Switch(
                            checked = includeContext,
                            enabled = !pending,
                            onCheckedChange = {
                                includeContext = it
                                bubbles = emptyList()
                                error = null
                            },
                        )
                    }
                }
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier.fillMaxWidth().height(305.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    if (bubbles.isEmpty()) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("What's happening with your plant?", style = PlantupiaText.editorialHeading)
                            Text("Describe a change, or choose a question.", style = PlantupiaText.supporting)
                            listOf(
                                "What should I check before watering?",
                                "Why are the leaf tips turning brown?",
                            ).forEach { idea ->
                                Surface(
                                    onClick = { input = idea },
                                    color = PlantupiaColors.softSage,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(idea, modifier = Modifier.fillMaxWidth().padding(13.dp), style = PlantupiaText.body)
                                }
                            }
                        }
                    }
                    itemsIndexed(bubbles) { _, message ->
                        PlantupiaConversationMessage(
                            body = message.text,
                            mine = message.fromUser,
                            sender = if (message.fromUser) "You" else "Plantupia AI",
                            detail = if (message.fromUser) "" else if (message.scope == "plant_context") "Plant context included" else "General guidance",
                        )
                    }
                    if (pending) item {
                        Text("Plantupia is thinking…", style = PlantupiaText.supporting)
                    }
                }
                error?.let { problem ->
                    Surface(color = Color(0xFFFFEDE9), shape = RoundedCornerShape(12.dp)) {
                        Text(problem, modifier = Modifier.fillMaxWidth().padding(12.dp), color = PlantupiaColors.errorText, style = PlantupiaText.supporting)
                    }
                }
                PlantupiaConversationComposer(
                    value = input,
                    onChange = { input = it.take(1000) },
                    enabled = !pending,
                    placeholder = if (plant == null) "Ask Plantupia…" else "Ask about " + plant.name + "…",
                    onSend = {
                        val question = input.trim()
                        if (question.isNotEmpty()) {
                            input = ""
                            error = null
                            val consent = includeContext
                            val dialogue = (if (consent) bubbles.takeLast(8) else emptyList()).map {
                                PlantupiaAssistantTurn(
                                    role = if (it.fromUser) "user" else "assistant",
                                    content = it.text.take(1200),
                                )
                            }
                            bubbles = bubbles + CareChatBubble(question, fromUser = true)
                            includeContext = false
                            pending = true
                            scope.launch {
                                when (val result = repository.askAssistant(
                                    message = question, plantId = plant?.id,
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
                                        input = question
                                        bubbles = bubbles.dropLast(1)
                                        if (result.statusCode == 402) accessLost()
                                        error = if (result.statusCode == 503)
                                            "Plantupia AI is unavailable right now. Your question wasn't added to your care history."
                                        else result.message
                                    }
                                }
                                pending = false
                            }
                        }
                    },
                )
                Text(
                    "AI guidance, not a confirmed diagnosis. No care record is created automatically.",
                    style = PlantupiaText.supporting,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
