package com.example.medtest1.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SupportChatActionButtons(
    showQuickChipsButton: Boolean,
    onShowQuickChips: () -> Unit,
    onCallUmnik: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showQuickChipsButton) {
            OutlinedButton(
                onClick = onShowQuickChips,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SupportChatText)
            ) {
                Text("Быстрые кнопки", style = MaterialTheme.typography.labelLarge)
            }
        }
        OutlinedButton(
            onClick = onCallUmnik,
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SupportAccent)
        ) {
            Text("Позвать Умника", style = MaterialTheme.typography.labelLarge)
        }
    }
}
