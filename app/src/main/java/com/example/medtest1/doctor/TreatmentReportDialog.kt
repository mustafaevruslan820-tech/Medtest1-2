package com.example.medtest1.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medtest1.data.TreatmentPlan
import com.example.medtest1.data.WellbeingEntry
import com.example.medtest1.network.BackendApi
import kotlinx.coroutines.launch

@Composable
fun TreatmentReportOptionsDialog(
    visible: Boolean,
    tokenProvider: () -> String,
    assignmentId: Long?,
    username: String,
    displayName: String?,
    birthDate: String?,
    plans: List<TreatmentPlan>,
    wellbeing: Map<String, WellbeingEntry>,
    onDismiss: () -> Unit,
    onSavePdf: () -> Unit,
    onDone: (message: String) -> Unit
) {
    if (!visible) return
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отчёт по лечению") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Курс выполнен. Сохраните отчёт на устройство или отправьте врачу для заключения.")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSavePdf()
                    onDismiss()
                }
            ) { Text("Сохранить PDF") }
        },
        dismissButton = {
            Button(
                onClick = {
                    val aid = assignmentId
                    if (aid == null || aid <= 0L) {
                        onDone("Сначала выберите врача во вкладке «Лечение».")
                        onDismiss()
                        return@Button
                    }
                    sending = true
                    scope.launch {
                        val json = reportDataToJson(username, displayName, birthDate, plans, wellbeing)
                        val ok = BackendApi.sendTreatmentReport(
                            token = tokenProvider(),
                            assignmentId = aid,
                            reportDataJson = json,
                            pdfBase64 = null
                        )
                        sending = false
                        onDismiss()
                        onDone(
                            if (ok) "Отчёт отправлен врачу на заключение."
                            else "Не удалось отправить отчёт."
                        )
                    }
                },
                enabled = !sending
            ) {
                if (sending) CircularProgressIndicator(modifier = Modifier.fillMaxWidth(0.2f))
                else Text("Отправить врачу")
            }
        }
    )
}
