package com.nova.app.feature.plants

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

// Row-major inner white cells from OpenCV's standard DICT_4X4_50.
private val markerBits = intArrayOf(
    0xb532, 0xf9a, 0x332d, 0x9946, 0x549e, 0x79cd, 0x9e2e, 0xc4f2, 0xfeda, 0xcf56,
    0xf991, 0x11a7, 0xeb7, 0x2a0f, 0x24b1, 0x263e, 0x4665, 0x6600, 0x6c5e, 0x76af,
    0x868b, 0xb02b, 0xccd5, 0xdd82, 0xfe47, 0x9471, 0xace4, 0xa554, 0x2123, 0x346f,
    0x4415, 0x57b2, 0x9ecf, 0xf0cb, 0x8ae, 0x929, 0x1875, 0x4ff, 0xdf6, 0x1c5a,
    0x1718, 0x2a28, 0x328c, 0x38b2, 0x24e8, 0x2eeb, 0x2d3f, 0x4b64, 0x502e, 0x5013,
)

private fun writeMarkerCard(output: OutputStream, pair: Int) {
    val doc = PdfDocument()
    try {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val paint = Paint().apply { color = Color.BLACK; textSize = 18f }
        canvas.drawColor(Color.WHITE)
        canvas.drawText("Plantupia   /   Plant measurement card", 40f, 60f, paint)
        paint.textSize = 12f
        canvas.drawText("Print at 100%. Check that each black square measures 20 mm across.", 40f, 97f, paint)
        canvas.drawText("Cut out each square with a white margin. Keep this pair for the same leaf.", 40f, 120f, paint)
        val edge = 72f * 2f / 2.54f
        for (offset in 0..1) {
            val id = pair * 2 + offset
            val x = 72f + offset * 220f
            val y = 188f
            paint.color = Color.BLACK
            canvas.drawRect(x, y, x + edge, y + edge, paint)
            paint.color = Color.WHITE
            val cell = edge / 6f
            for (bit in 0 until 16) {
                if ((markerBits[id] and (1 shl (15-bit))) == 0) continue
                val left = x + cell * (1 + bit % 4)
                val top = y + cell * (1 + bit / 4)
                canvas.drawRect(left, top, left + cell, top + cell, paint)
            }
            paint.color = Color.BLACK
            canvas.drawText("Marker $id", x, y + edge + 24f, paint)
        }
        doc.finishPage(page)
        doc.writeTo(output)
    } finally {
        doc.close()
    }
}

@Composable
internal fun TagScaleControls(active: Boolean, edge: String, enabled: Boolean,
                              toggle: (Boolean) -> Unit, changeEdge: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pair by remember { mutableIntStateOf(0) }
    var savingPair by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            message = try {
                withContext(Dispatchers.IO) {
                    (context.contentResolver.openOutputStream(uri) ?: throw java.io.IOException()).use { writeMarkerCard(it, savingPair) }
                }
                "Reference card saved."
            } catch (_: java.io.IOException) { "The card could not be saved." }
              catch (_: SecurityException) { "Choose a writable folder." }
        }
    }
    FilterChip(selected = active, enabled = enabled, onClick = { toggle(!active) },
        label = { Text("Use a tagged measurement plane") })
    if (active) {
        Text("Keep one marker on either side of the same flat plant part, in its plane. Use a different pair for each part on this plant.",
            style = PlantupiaText.supporting)
        OutlinedTextField(value = edge, onValueChange = changeEdge, enabled = enabled, singleLine = true,
            label = { Text("Measured black square edge (cm)") }, modifier = Modifier.fillMaxWidth())
        Row {
            Box {
                TextButton(enabled = enabled, onClick = { menu = true }) { Text("Pair ${pair+1}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    repeat(25) { i -> DropdownMenuItem(text = { Text("Pair ${i+1} · IDs ${i*2} and ${i*2+1}") },
                        onClick = { pair = i; menu = false }) }
                }
            }
            TextButton(enabled = enabled, onClick = { savingPair = pair; export.launch("plantupia-marker-pair-${pair+1}.pdf") }) {
                Text("Save printable card")
            }
        }
        if (message.isNotBlank()) Text(message, style = PlantupiaText.supporting)
    }
}
