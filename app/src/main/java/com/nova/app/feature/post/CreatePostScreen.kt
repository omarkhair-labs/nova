package com.nova.app.feature.post

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaVideoPlayer
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun CreatePostScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onShare: (Uri, String) -> Unit,
) {
    val context = LocalContext.current
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    val selectedMime = remember(selectedMedia) {
        selectedMedia?.let { context.contentResolver.getType(it).orEmpty().lowercase() }.orEmpty()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            selectedMedia = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlantupiaColors.cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { if (!isLoading) onBack() }, enabled = !isLoading) {
                Text("? Back", color = PlantupiaColors.forest, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Text("SHARED MOMENT / PUBLIC", style = PlantupiaText.sectionLabel)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Share a moment", style = PlantupiaText.pageHeading)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Choose one photo or video that you intentionally want other people to see.",
            style = PlantupiaText.body,
            modifier = Modifier.fillMaxWidth(0.94f),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PlantupiaColors.softSage,
            shape = PlantupiaShapes.inset,
        ) {
            Text(
                text = "PRIVATE RECORDS STAY PRIVATE / Publishing this moment does not expose your plant history, spaces or care records.",
                modifier = Modifier.padding(14.dp),
                color = PlantupiaColors.forestText,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            onClick = { if (!isLoading) picker.launch(arrayOf("image/*", "video/*")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = PlantupiaColors.paper,
            border = BorderStroke(1.dp, PlantupiaColors.border),
        ) {
            if (selectedMedia != null) {
                if (selectedMime.startsWith("video/")) {
                    NovaVideoPlayer(
                        source = selectedMedia.toString(),
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                        autoplay = true,
                        repeat = true,
                        muted = true,
                        useController = true,
                        description = "Selected shared-moment video preview",
                    )
                } else {
                    NovaMediaImage(
                        source = selectedMedia.toString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(28.dp)),
                        contentDescription = "Selected shared-moment photo",
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = NovaAccentSoft,
                    ) {
                        Text(
                            text = "+",
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            color = NovaAccent,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Choose the moment",
                        color = NovaInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A photo or video from your world, shared only because you chose it.",
                        color = NovaMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (selectedMedia != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = { if (!isLoading) picker.launch(arrayOf("image/*", "video/*")) },
                shape = RoundedCornerShape(16.dp),
                color = NovaAccentSoft,
            ) {
                Text(
                    text = "Change media",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = NovaAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { if (it.length <= 500) caption = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            label = { Text("Caption (optional)") },
            placeholder = { Text("Say something about this moment…", color = NovaMuted) },
            enabled = !isLoading,
            maxLines = 6,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NovaAccent,
                unfocusedBorderColor = NovaBorder,
                focusedLabelColor = NovaAccent,
                cursorColor = NovaAccent,
                focusedContainerColor = NovaSurface,
                unfocusedContainerColor = NovaSurface,
            ),
        )

        Text(
            text = "${caption.length}/500",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            color = NovaMuted,
            fontSize = 11.sp,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(14.dp),
                    color = NovaMuted,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        NovaPrimaryButton(
            text = if (isLoading) "Publishing…" else "Publish moment",
            onClick = {
                val media = selectedMedia
                if (media != null && !isLoading) onShare(media, caption)
            },
            enabled = selectedMedia != null && !isLoading,
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Plantupia can finish publishing in the background. You can continue using the app while this moment is prepared.",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}
