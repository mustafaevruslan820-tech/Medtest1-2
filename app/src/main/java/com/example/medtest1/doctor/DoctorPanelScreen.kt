package com.example.medtest1.doctor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.DoctorAssignment
import com.example.medtest1.network.DoctorPrescription
import com.example.medtest1.network.TreatmentReportSummary
import com.example.medtest1.ui.MedSectionLabel
import com.example.medtest1.ui.MedSubScreenLayout
import com.example.medtest1.ui.MedSurfaceCard
import com.example.medtest1.ui.theme.LocalMedAppColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class PatientShiftGroup(
    val title: String,
    val assignments: List<DoctorAssignment>
)

private fun groupPatientsByShift(assignments: List<DoctorAssignment>): List<PatientShiftGroup> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val yesterdayStart = todayStart - 86_400_000L

    val today = mutableListOf<DoctorAssignment>()
    val yesterday = mutableListOf<DoctorAssignment>()
    val earlier = mutableListOf<DoctorAssignment>()
    assignments.forEach { assignment ->
        when {
            assignment.assignedAt >= todayStart -> today.add(assignment)
            assignment.assignedAt >= yesterdayStart -> yesterday.add(assignment)
            else -> earlier.add(assignment)
        }
    }
    return buildList {
        if (today.isNotEmpty()) add(PatientShiftGroup("Сегодняшняя смена", today))
        if (yesterday.isNotEmpty()) add(PatientShiftGroup("Вчерашняя смена", yesterday))
        if (earlier.isNotEmpty()) add(PatientShiftGroup("Ранее", earlier))
    }
}

@Composable
fun DoctorPanelScreen(
    modifier: Modifier = Modifier,
    tokenProvider: () -> String,
    onEditProfile: () -> Unit,
    onOpenChat: (assignmentId: Long, patientName: String) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalMedAppColors.current
    val scope = rememberCoroutineScope()
    var onDuty by remember { mutableStateOf(false) }
    var shiftLoading by remember { mutableStateOf(true) }
    var assignments by remember { mutableStateOf<List<DoctorAssignment>>(emptyList()) }
    var selected by remember { mutableStateOf<DoctorAssignment?>(null) }
    var prescriptions by remember { mutableStateOf<List<DoctorPrescription>>(emptyList()) }
    var reports by remember { mutableStateOf<List<TreatmentReportSummary>>(emptyList()) }
    var rxText by remember { mutableStateOf("") }
    var planText by remember { mutableStateOf("") }
    var conclusion by remember { mutableStateOf("") }
    var continuePlan by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            val (_, duty) = BackendApi.getMyShift(tokenProvider())
            onDuty = duty
            shiftLoading = false
            assignments = BackendApi.getDoctorAssignments(tokenProvider())
        }
    }

    fun loadDetail(assignment: DoctorAssignment) {
        scope.launch {
            val (a, rx, rep) = BackendApi.getAssignmentDetail(tokenProvider(), assignment.id)
            selected = a ?: assignment
            prescriptions = rx
            reports = rep
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val shiftColor by animateColorAsState(
        targetValue = if (onDuty) app.accentRing else app.onHeroMuted,
        animationSpec = tween(400),
        label = "shift-color"
    )

    if (selected != null) {
        val assignment = selected!!
        MedSubScreenLayout(
            title = assignment.patientUsername,
            subtitle = "Пациент",
            onBack = { selected = null },
            modifier = modifier
        ) {
            MedSurfaceCard {
                Text("Профиль пациента", fontWeight = FontWeight.Bold)
                assignment.patientProfileJson?.let { json ->
                    runCatching {
                        val o = JSONObject(json)
                        Text("ФИО: ${o.optString("fullName")}")
                        Text("Дата рождения: ${o.optString("birthDate")}")
                        Text("Группа крови: ${o.optString("bloodType")}")
                        Text("Аллергии: ${o.optString("allergies")}")
                        Text("Хронические: ${o.optString("chronicDiseases")}")
                    }
                } ?: Text("Данные профиля не переданы.")
            }
            MedSurfaceCard {
                Text("План лечения и дневник", fontWeight = FontWeight.Bold)
                assignment.treatmentSyncJson?.let { json ->
                    runCatching {
                        val root = JSONObject(json)
                        val plans = root.optJSONArray("plans")
                        if (plans != null) {
                            for (i in 0 until plans.length()) {
                                val p = plans.getJSONObject(i)
                                Text("• ${p.optString("medicineName")} — ${p.optString("dosage")} в ${p.optString("reminderTime")}")
                            }
                        }
                        val wellbeing = root.optJSONObject("wellbeing")
                        if (wellbeing != null && wellbeing.length() > 0) {
                            Text("Дневник самочувствия:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                            wellbeing.keys().forEach { day ->
                                val e = wellbeing.getJSONObject(day)
                                Text("$day: ${e.optString("mood")} — ${e.optString("comment")}")
                            }
                        }
                    }
                } ?: Text("Пациент ещё не синхронизировал план.")
            }
            Button(
                onClick = { onOpenChat(assignment.id, assignment.patientUsername) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Чат с пациентом") }

            MedSectionLabel("Выдать рецепт и план")
            MedSurfaceCard {
                OutlinedTextField(
                    value = rxText,
                    onValueChange = { rxText = it },
                    label = { Text("Рецепт") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = planText,
                    onValueChange = { planText = it },
                    label = { Text("План лечения") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Button(
                    onClick = {
                        scope.launch {
                            val ok = BackendApi.sendPrescription(
                                tokenProvider(), assignment.id, rxText.trim(), planText.trim()
                            )
                            status = if (ok) "Рецепт отправлен пациенту." else "Не удалось отправить."
                            if (ok) loadDetail(assignment)
                        }
                    },
                    enabled = rxText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Отправить рецепт") }
            }

            if (prescriptions.isNotEmpty()) {
                MedSectionLabel("Ранее выданные рецепты")
                prescriptions.forEach { rx ->
                    MedSurfaceCard {
                        Text(rx.prescriptionText)
                        if (rx.treatmentPlanText.isNotBlank()) {
                            Text("План: ${rx.treatmentPlanText}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            val pendingReport = reports.firstOrNull { it.status == "pending" }
            if (pendingReport != null) {
                MedSectionLabel("Отчёт на заключение")
                MedSurfaceCard {
                    Text("Отчёт от ${formatTs(pendingReport.createdAt)}", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = conclusion,
                        onValueChange = { conclusion = it },
                        label = { Text("Заключение") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = continuePlan,
                        onValueChange = { continuePlan = it },
                        label = { Text("Новый план (если продолжить)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val ok = BackendApi.concludeReport(
                                        token = tokenProvider(),
                                        assignmentId = assignment.id,
                                        reportId = pendingReport.id,
                                        action = "complete",
                                        conclusion = conclusion.ifBlank { "Лечение завершено." },
                                        newTreatmentPlanText = null
                                    )
                                    status = if (ok) "Лечение отмечено завершённым." else "Ошибка."
                                    loadDetail(assignment)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Завершить") }
                        Button(
                            onClick = {
                                scope.launch {
                                    val ok = BackendApi.concludeReport(
                                        token = tokenProvider(),
                                        assignmentId = assignment.id,
                                        reportId = pendingReport.id,
                                        action = "continue",
                                        conclusion = conclusion.ifBlank { "Продолжить лечение." },
                                        newTreatmentPlanText = continuePlan
                                    )
                                    status = if (ok) "Назначено продолжение лечения." else "Ошибка."
                                    loadDetail(assignment)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Продолжить") }
                    }
                }
            }
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        return
    }

    MedSubScreenLayout(
        title = "Панель врача",
        subtitle = if (onDuty) "Смена открыта — пациенты видят вас" else "Начните смену для приёма пациентов",
        onBack = onBack,
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 8 }
        ) {
            MedSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Смена", fontWeight = FontWeight.Bold, color = shiftColor)
                        Text(
                            if (onDuty) "Вы на смене" else "Смена закрыта",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (shiftLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    shiftLoading = true
                                    if (onDuty) {
                                        BackendApi.endShift(tokenProvider())
                                    } else {
                                        BackendApi.startShift(tokenProvider())
                                    }
                                    refresh()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (onDuty) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.primary,
                                contentColor = if (onDuty) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(if (onDuty) "Закончить смену" else "Начать смену")
                        }
                    }
                }
                Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                    Text("Редактировать анкету")
                }
            }
        }

        MedSectionLabel("Мои пациенты")
        if (assignments.isEmpty()) {
            Text("Пока нет назначенных пациентов.", modifier = Modifier.padding(8.dp))
        } else {
            groupPatientsByShift(assignments).forEach { group ->
                MedSectionLabel(group.title)
                group.assignments.forEach { a ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 }
                    ) {
                        MedSurfaceCard(
                            modifier = Modifier.clickable { loadDetail(a) }
                        ) {
                            Text(a.patientUsername, fontWeight = FontWeight.SemiBold)
                            Text("Назначен: ${formatTs(a.assignedAt)}", style = MaterialTheme.typography.bodySmall)
                            Text("Нажмите для просмотра профиля и лечения", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTs(ts: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ts))
