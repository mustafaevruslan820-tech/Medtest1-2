package com.example.medtest1.support

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SupportQuickReplyBar(
    chips: List<String>,
    enabled: Boolean,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chips.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { label ->
            AssistChip(
                onClick = { if (enabled) onChipClick(label) },
                enabled = enabled,
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = SupportIncomingBubble,
                    labelColor = SupportChatText,
                    disabledContainerColor = SupportIncomingBubble.copy(alpha = 0.5f),
                    disabledLabelColor = SupportChatTime
                )
            )
        }
    }
}
