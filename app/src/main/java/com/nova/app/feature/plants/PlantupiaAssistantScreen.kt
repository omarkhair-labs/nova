package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.launch

/** Real authenticated server assistant, text-only. Never pretends a demo reply is AI. */
@Composable
fun PlantupiaAssistantScreen(plantId: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val turns = remember(plantId) { mutableStateListOf<Pair<Boolean, String>>() }
    var text by remember(plantId) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var plantName by remember(plantId) { mutableStateOf<String?>(null) }
    var includeContext by remember(plantId) { mutableStateOf(plantId != null) }
    LaunchedEffect(plantId) {
        if (plantId != null) {
            when (val result = repository.plants()) {
                is ApiResult.Success -> {
                    plantName = result.value.firstOrNull { it.id == plantId }?.name
                    if (plantName == null) includeContext = false
                }
                is ApiResult.Failure -> includeContext = false
            }
        }
    }
    Scaffold(containerColor = NovaBackground) { inset ->
        Column(modifier = Modifier.fillMaxSize().padding(inset).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                TextButton(onClick = onBack) { Text("← رجوع", color = NovaAccent) }
                Spacer(Modifier.weight(1f))
                Text("Plantupia AI", style = NovaType.sectionTitle, color = NovaAccent)
            }
            Text("اسأل عن نباتك", style = NovaType.screenTitle, color = NovaAccent)
            Text("إرشاد عام، مش تشخيص مؤكد ولا بديل عن خبير. مفيش حساسات أو قراءات تلقائية.", color = NovaMuted, style = NovaType.bodyCompact)
            if (plantId != null && plantName != null) {
                Surface(color = NovaSurface, shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(checked = includeContext, onCheckedChange = { includeContext = it })
                        Column(Modifier.weight(1f)) {
                            Text("استخدم سجل ${plantName.orEmpty()}؟", color = NovaAccent)
                            Text("الاسم والنوع ومكان النبات وملاحظاتك الأخيرة، من غير صور أو بيانات حساب.", color = NovaMuted, style = NovaType.meta)
                        }
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(turns) { index, entry ->
                    Surface(
                        color = if (entry.first) NovaAccent.copy(alpha = .11f) else NovaSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(if (entry.first) "سؤالك" else "مساعد Plantupia", color = NovaAccent, style = NovaType.label)
                            Text(entry.second, color = NovaAccent, style = NovaType.bodyCompact)
                        }
                    }
                }
                if (busy) item { CircularProgressIndicator(color = NovaAccent) }
            }
            if (error != null) Text(error.orEmpty(), color = NovaAccent, style = NovaType.bodyCompact)
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(1200) },
                label = { Text("إيه اللي لاحظته؟") }, modifier = Modifier.fillMaxWidth(),
                enabled = !busy, maxLines = 4,
            )
            Button(
                enabled = !busy && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NovaAccent),
                onClick = {
                    val question = text.trim()
                    busy = true
                    error = null
                    scope.launch {
                        when (val reply = repository.askAssistant(question, plantId, includeContext)) {
                            is ApiResult.Success -> {
                                turns.add(true to question)
                                turns.add(false to reply.value)
                                text = ""
                            }
                            is ApiResult.Failure -> error = reply.message
                        }
                        busy = false
                    }
                },
            ) { Text("اسأل المساعد") }
            Text("محادثة مؤقتة؛ مفيش رعاية بتتسجل تلقائيًا، ولا سؤال بيتنشر للناس.", color = NovaMuted, style = NovaType.meta)
        }
    }
}
