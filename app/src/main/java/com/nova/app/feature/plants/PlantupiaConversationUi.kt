package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

/**
 * Shared conversation surfaces. Preserve the difference between a model answer
 * and an authenticated reply written by a Plantupia employee.
 */
@Composable
internal fun PlantupiaConversationHeader(
    category: String,
    title: String,
    subtitle: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = PlantupiaColors.softSage, shape = RoundedCornerShape(15.dp)) {
            NovaIcon(
                asset = if (category.startsWith("AI")) NovaIconAsset.Info else NovaIconAsset.Messages,
                contentDescription = null,
                tint = PlantupiaColors.forest,
                modifier = Modifier.padding(14.dp).size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(category, style = PlantupiaText.sectionLabel)
            Text(title, style = PlantupiaText.editorialHeading)
            Text(subtitle, style = PlantupiaText.supporting, maxLines = 2)
        }
        TextButton(onClick = onClose) {
            Text("Close", color = PlantupiaColors.forest)
        }
    }
}

@Composable
internal fun PlantupiaConversationMessage(
    body: String,
    mine: Boolean,
    sender: String,
    detail: String = "",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = if (mine) PlantupiaColors.forest else PlantupiaColors.softSage,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (mine) 18.dp else 5.dp,
                bottomEnd = if (mine) 5.dp else 18.dp,
            ),
            border = if (mine) null else BorderStroke(1.dp, PlantupiaColors.border),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    sender,
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (mine) PlantupiaColors.cream else PlantupiaColors.secondaryText,
                )
                Text(
                    body,
                    style = PlantupiaText.body.copy(
                        color = if (mine) PlantupiaColors.paper else PlantupiaColors.forestText,
                    ),
                )
                if (detail.isNotBlank()) Text(
                    detail, style = PlantupiaText.supporting.copy(
                        color = if (mine) PlantupiaColors.cream else PlantupiaColors.secondaryText,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun PlantupiaConversationComposer(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    placeholder: String = "Write a message…",
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder, style = PlantupiaText.supporting) },
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            minLines = 1, maxLines = 4,
        )
        Button(
            modifier = Modifier.heightIn(min = 56.dp),
            enabled = enabled && value.isNotBlank(),
            onClick = onSend,
            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
            shape = RoundedCornerShape(18.dp),
        ) {
            NovaIcon(
                asset = NovaIconAsset.Send,
                contentDescription = null,
                tint = PlantupiaColors.paper,
                modifier = Modifier.size(17.dp),
            )
            Text("Send", modifier = Modifier.padding(start = 5.dp))
        }
    }
}
