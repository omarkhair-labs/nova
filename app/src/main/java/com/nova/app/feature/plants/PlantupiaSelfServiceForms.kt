package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import kotlinx.coroutines.launch

private val ink = Color(0xFF123B2E)
private val textMuted = Color(0xFF4A5A50)
private val paper = Color(0xFFFFFDF7)

/** A signed-in person can start a living space without admin provisioning. */
@Composable
fun PlantupiaSelfServiceForms(
    spaces: List<PlantupiaSpace>,
    repository: PlantupiaRepository,
    onCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var spaceOpen by remember { mutableStateOf(false) }
    var plantOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var spaceName by remember { mutableStateOf("") }
    var spaceLocation by remember { mutableStateOf("") }
    var spaceDescription by remember { mutableStateOf("") }
    var plantName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf("") }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    var spaceMenuOpen by remember { mutableStateOf(false) }
    var plantPhoto by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        plantPhoto = it
    }

    LaunchedEffect(spaces) {
        if (selectedSpaceId == null && spaces.isNotEmpty()) selectedSpaceId = spaces.first().id
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (spaces.isEmpty() && !spaceOpen && !plantOpen) {
            Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("ابدأ من مكانك", color = ink, fontSize = 22.sp)
                    Text(
                        "بيتك، بلكونتك، مكتبك، أو أي مكان فيه حياة. سمّي المساحة الأول، وبعدها أضف نباتاتك واحدة واحدة.",
                        color = textMuted, fontSize = 13.sp,
                    )
                    Button(
                        onClick = { error = null; spaceOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ink),
                    ) { Text("+ أنشئ مساحتي") }
                }
            }
        } else if (!spaceOpen && !plantOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { error = null; spaceOpen = true }, modifier = Modifier.weight(1f)) {
                    Text("+ مساحة جديدة", color = ink, fontSize = 12.sp)
                }
                Button(
                    onClick = { error = null; plantOpen = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ink),
                ) { Text("+ أضف نبات", fontSize = 12.sp) }
            }
        }

        if (spaceOpen) {
            Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مساحة جديدة", color = ink, fontSize = 22.sp)
                    Text("المساحة خاصة بيك، ومش هتظهر على بروفايلك العام.", color = textMuted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = spaceName, onValueChange = { spaceName = it.take(140) },
                        label = { Text("اسم المكان *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = spaceLocation, onValueChange = { spaceLocation = it.take(200) },
                        label = { Text("مكانها (اختياري)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = spaceDescription, onValueChange = { spaceDescription = it.take(1000) },
                        label = { Text("عن المكان (اختياري)") },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = !saving && spaceName.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    saving = true
                                    error = null
                                    when (val result = repository.createSpace(spaceName, spaceDescription, spaceLocation)) {
                                        is ApiResult.Failure -> error = result.message
                                        is ApiResult.Success -> {
                                            selectedSpaceId = result.value
                                            spaceOpen = false
                                            plantOpen = true
                                            spaceName = ""
                                            spaceLocation = ""
                                            spaceDescription = ""
                                            onCreated()
                                        }
                                    }
                                    saving = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ink),
                        ) { Text(if (saving) "جارٍ الحفظ…" else "أنشئ وأضف نبات") }
                        TextButton(onClick = { spaceOpen = false; error = null }, enabled = !saving) {
                            Text("إلغاء", color = ink)
                        }
                    }
                }
            }
        }

        if (plantOpen) {
            Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نبات جديد", color = ink, fontSize = 22.sp)
                    Text("كل نبات ليه اسم ومكان وتاريخ بيتكوّن مع الوقت.", color = textMuted, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { spaceMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    ) {
                        Text(
                            spaces.firstOrNull { it.id == selectedSpaceId }?.name
                                ?: "اختار المساحة",
                            color = ink,
                        )
                    }
                    DropdownMenu(expanded = spaceMenuOpen, onDismissRequest = { spaceMenuOpen = false }) {
                        spaces.forEach { space ->
                            DropdownMenuItem(text = { Text(space.name) }, onClick = {
                                selectedSpaceId = space.id
                                spaceMenuOpen = false
                            })
                        }
                    }
                    OutlinedTextField(
                        value = plantName, onValueChange = { plantName = it.take(120) },
                        label = { Text("اسم النبات *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = species, onValueChange = { species = it.take(180) },
                        label = { Text("نوع النبات (لو تعرفه)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = placement, onValueChange = { placement = it.take(180) },
                        label = { Text("مكانه جوه المساحة") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") }, enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (plantPhoto == null) "+ صورة حقيقية (اختيارية)" else "تغيير الصورة", color = ink) }
                    plantPhoto?.let { uri ->
                        AsyncImage(
                            model = uri, contentDescription = "صورة النبات المختارة",
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        TextButton(onClick = { plantPhoto = null }, enabled = !saving) {
                            Text("إزالة الصورة", color = textMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = !saving && plantName.isNotBlank() && selectedSpaceId != null,
                            onClick = {
                                val targetSpace = selectedSpaceId ?: return@Button
                                scope.launch {
                                    saving = true
                                    error = null
                                    when (val result = repository.createPlant(
                                        targetSpace, plantName, species, placement, plantPhoto,
                                    )) {
                                        is ApiResult.Failure -> error = result.message
                                        is ApiResult.Success -> {
                                            plantName = ""
                                            species = ""
                                            placement = ""
                                            plantPhoto = null
                                            plantOpen = false
                                            onCreated()
                                        }
                                    }
                                    saving = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ink),
                        ) { Text(if (saving) "جارٍ الحفظ…" else "أضف النبات") }
                        TextButton(onClick = { plantOpen = false; error = null }, enabled = !saving) {
                            Text("إلغاء", color = ink)
                        }
                    }
                }
            }
        }

        error?.let { Text(it, color = Color(0xFF9A4A31), fontSize = 12.sp) }
    }
}
