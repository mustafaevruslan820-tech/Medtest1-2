package com.example.medtest1.doctor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtest1.data.TreatmentPlan
import com.example.medtest1.data.UserProfile
import com.example.medtest1.data.WellbeingEntry
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.CareEvent
import com.example.medtest1.network.DoctorAssignment
import com.example.medtest1.network.DoctorPrescription
import com.example.medtest1.network.DoctorProfile
import com.example.medtest1.ui.theme.LocalMedAppColors
import kotlinx.coroutines.launch

@Composable
fun DoctorTreatmentSection(
    username: String,
    tokenProvider: () -> String,
    userProfile: UserProfile?,
    treatmentPlans: List<TreatmentPlan>,
    wellbeingEntries: Map<String, WellbeingEntry>,
    patientMilestoneSeenIndex: Int,
    onPatientMilestoneSeen: (Int) -> Unit,
    onOpenChat: (assignmentId: Long, doctorName: String) -> Unit,
    onAssignmentChanged: (DoctorAssignment?) -> Unit,
    onAutoPlansCreated: (List<TreatmentPlan>) -> Unit
) {
    val app = LocalMedAppColors.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var doctors by remember { mutableStateOf<List<DoctorProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var assignment by remember { mutableStateOf<DoctorAssignment?>(null) }
    var prescriptions by remember { mutableStateOf<List<DoctorPrescription>>(emptyList()) }
    var careEvents by remember { mutableStateOf<List<CareEvent>>(emptyList()) }
    var patientMilestones by remember { mutableStateOf<List<CareMilestone>>(emptyList()) }
    var seenMilestoneIndex by remember(patientMilestoneSeenIndex) {
        mutableIntStateOf(patientMilestoneSeenIndex)
    }
    var profilePreview by remember { mutableStateOf<DoctorProfile?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showRejectDoctorDialog by remember { mutableStateOf(false) }
    var showDeclineRxDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var declineRxReason by remember { mutableStateOf("") }
    var rejecting by remember { mutableStateOf(false) }
    var respondingRx by remember { mutableStateOf(false) }
    var markingDose by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var assignedDoctorProfile by remember { mutableStateOf<DoctorProfile?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val pendingRx = remember(prescriptions) { pendingPrescription(prescriptions) }
    val firstDoseDone = remember(careEvents) { hasFirstDoseRecorded(careEvents) }
    val treatmentAccepted = remember(prescriptions, careEvents) {
        prescriptions.any { it.patientStatus == "accepted" } ||
            careEvents.any { it.eventType == "prescription_accepted" }
    }

    suspend fun refreshAssignmentDetail() {
        val a = assignment ?: return
        val detail = BackendApi.getAssignmentDetail(tokenProvider(), a.id)
        prescriptions = detail.prescriptions
        careEvents = detail.careEvents
        patientMilestones = computePatientViewMilestones(true, prescriptions, detail.reports, careEvents)
        val latestDone = latestDoneMilestoneIndex(patientMilestones)
        if (latestDone > seenMilestoneIndex) {
            onPatientMilestoneSeen(latestDone)
            seenMilestoneIndex = latestDone
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        doctors = BackendApi.listOnDutyDoctors(tokenProvider())
        assignment = BackendApi.getPatientAssignment(tokenProvider())
        onAssignmentChanged(assignment)
        loading = false
    }

    LaunchedEffect(assignment?.id) {
        assignment?.let { refreshAssignmentDetail() }
    }

    LaunchedEffect(assignment?.doctorUserId) {
        val doctorId = assignment?.doctorUserId ?: run {
            assignedDoctorProfile = null
            return@LaunchedEffect
        }
        assignedDoctorProfile = BackendApi.getDoctorProfile(tokenProvider(), doctorId)
    }

    LaunchedEffect(assignment?.id, treatmentPlans, wellbeingEntries) {
        val a = assignment ?: return@LaunchedEffect
        BackendApi.syncPatientTreatment(
            token = tokenProvider(),
            assignmentId = a.id,
            treatmentSyncJson = treatmentDataToJson(treatmentPlans, wellbeingEntries)
        )
        refreshAssignmentDetail()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = app.cardOnHero)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Выбор врача",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = app.onHero
            )
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else if (assignment != null) {
                val assignedSpecialty = formatDoctorSpecialtyLabel(
                    assignment!!.doctorSpecialty.ifBlank {
                        assignedDoctorProfile?.specialty.orEmpty()
                    }
                )
                Text(
                    buildString {
                        append("Ваш врач: ${assignment!!.doctorUsername}")
                        if (assignedSpecialty.isNotBlank()) {
                            append(" · ")
                            append(assignedSpecialty)
                        }
                    },
                    color = app.onHeroMuted,
                    fontWeight = FontWeight.Medium
                )
                if (patientMilestones.isNotEmpty()) {
                    Text(
                        "Прогресс лечения",
                        style = MaterialTheme.typography.labelMedium,
                        color = app.onHeroMuted
                    )
                    CareMilestoneRow(
                        milestones = patientMilestones,
                        highlightNewFromIndex = seenMilestoneIndex
                    )
                }

                pendingRx?.let { rx ->
                    val days = countPlanDays(rx.treatmentPlanText)
                    val meds = countPlanMedicines(rx.treatmentPlanText)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = scheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "План лечения от врача",
                                fontWeight = FontWeight.Bold,
                                color = app.onHero
                            )
                            Text(
                                formatStructuredPlanForPatientCard(rx.treatmentPlanText),
                                color = app.onHeroMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (days > 0) {
                                Text(
                                    "$days дней · $meds приёмов",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "После принятия расписание создастся автоматически.",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.onHeroMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (respondingRx) return@Button
                                        respondingRx = true
                                        scope.launch {
                                            val result = BackendApi.respondToPrescription(
                                                token = tokenProvider(),
                                                assignmentId = assignment!!.id,
                                                prescriptionId = rx.id,
                                                action = "accept"
                                            )
                                            if (result.ok) {
                                                val plans = buildPlansFromPrescription(
                                                    username = username,
                                                    prescriptionText = rx.prescriptionText,
                                                    treatmentPlanText = rx.treatmentPlanText,
                                                    courseId = rx.id
                                                )
                                                onAutoPlansCreated(plans)
                                                refreshAssignmentDetail()
                                                status = if (result.httpCode == 404) {
                                                    "План принят локально. Обновите backend для синхронизации с врачом."
                                                } else {
                                                    "План принят. Лечение добавлено в расписание."
                                                }
                                            } else {
                                                status = apiActionErrorMessage(
                                                    default = "Не удалось принять план.",
                                                    result = result,
                                                    knownErrors = mapOf(
                                                        "already_responded" to "На этот план уже дан ответ."
                                                    )
                                                )
                                            }
                                            respondingRx = false
                                        }
                                    },
                                    enabled = !respondingRx,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (respondingRx) "…" else "Принять план")
                                }
                                OutlinedButton(
                                    onClick = { showDeclineRxDialog = true },
                                    enabled = !respondingRx,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error)
                                ) {
                                    Text("Отклонить")
                                }
                            }
                        }
                    }
                }

                if (treatmentAccepted && !firstDoseDone) {
                    Button(
                        onClick = {
                            if (markingDose) return@Button
                            markingDose = true
                            scope.launch {
                                val ok = BackendApi.recordCareEvent(
                                    tokenProvider(),
                                    assignment!!.id,
                                    "first_dose_taken"
                                )
                                if (ok) {
                                    refreshAssignmentDetail()
                                    status = "Отмечено: первая таблетка принята."
                                } else {
                                    status = "Не удалось отметить приём."
                                }
                                markingDose = false
                            }
                        },
                        enabled = !markingDose,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.tertiary,
                            contentColor = scheme.onTertiary
                        )
                    ) {
                        Text(if (markingDose) "Сохранение…" else "Принял первую таблетку")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        onOpenChat(assignment!!.id, assignment!!.doctorUsername)
                    }) {
                        Text("Чат с врачом")
                    }
                    OutlinedButton(
                        onClick = { showRejectDoctorDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error)
                    ) {
                        Text("Отказаться от врача")
                    }
                }
            } else if (doctors.isEmpty()) {
                Text(
                    "Сейчас нет врачей на смене. Попробуйте позже.",
                    color = app.onHeroMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "Врачи на смене — специальность указана на карточке. Нажмите, чтобы выбрать:",
                    color = app.onHeroMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(doctors, key = { _, doc -> doc.userId }) { index, doc ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(280 + index * 70)) +
                                slideInVertically(tween(360 + index * 70)) { it / 5 }
                        ) {
                            DoctorSelectionCard(
                                doctor = doc,
                                index = index,
                                onClick = {
                                    profilePreview = doc
                                    showProfileDialog = true
                                }
                            )
                        }
                    }
                }
            }
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = app.onHeroMuted)
            }
        }
    }

    if (showDeclineRxDialog && pendingRx != null) {
        AlertDialog(
            onDismissRequest = { if (!respondingRx) showDeclineRxDialog = false },
            title = { Text("Отклонить план лечения") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Укажите причину — врач увидит её в чате.")
                    OutlinedTextField(
                        value = declineRxReason,
                        onValueChange = { declineRxReason = it },
                        label = { Text("Причина") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = declineRxReason.trim()
                        if (reason.length < 3 || respondingRx) return@Button
                        respondingRx = true
                        scope.launch {
                            val result = BackendApi.respondToPrescription(
                                token = tokenProvider(),
                                assignmentId = assignment!!.id,
                                prescriptionId = pendingRx.id,
                                action = "decline",
                                reason = reason
                            )
                            respondingRx = false
                            showDeclineRxDialog = false
                            declineRxReason = ""
                            if (result.ok) {
                                refreshAssignmentDetail()
                                status = "План отклонён. Врач получит уведомление."
                            } else {
                                status = apiActionErrorMessage(
                                    default = "Не удалось отклонить план.",
                                    result = result,
                                    knownErrors = mapOf(
                                        "reason_required" to "Укажите причину (минимум 3 символа).",
                                        "already_responded" to "На этот план уже дан ответ."
                                    )
                                )
                            }
                        }
                    },
                    enabled = declineRxReason.trim().length >= 3 && !respondingRx
                ) { Text("Отправить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeclineRxDialog = false }, enabled = !respondingRx) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showRejectDoctorDialog) {
        AlertDialog(
            onDismissRequest = { if (!rejecting) showRejectDoctorDialog = false },
            title = { Text("Отказ от врача") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Укажите причину отказа — врач увидит её в разделе «Отказы».")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Причина") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectReason.trim()
                        if (reason.length < 3 || rejecting) return@Button
                        rejecting = true
                        scope.launch {
                            val result = BackendApi.rejectDoctorAssignment(tokenProvider(), reason)
                            rejecting = false
                            showRejectDoctorDialog = false
                            rejectReason = ""
                            if (result.ok) {
                                assignment = null
                                assignedDoctorProfile = null
                                prescriptions = emptyList()
                                careEvents = emptyList()
                                patientMilestones = emptyList()
                                onAssignmentChanged(null)
                                status = "Вы отказались от врача. Можно выбрать другого."
                            } else {
                                status = apiActionErrorMessage(
                                    default = "Не удалось отправить отказ.",
                                    result = result,
                                    knownErrors = mapOf(
                                        "reason_required" to "Укажите причину (минимум 3 символа).",
                                        "no_assignment" to "Активное назначение не найдено."
                                    )
                                )
                            }
                        }
                    },
                    enabled = rejectReason.trim().length >= 3 && !rejecting
                ) { Text("Отправить") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDoctorDialog = false }, enabled = !rejecting) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showProfileDialog && profilePreview != null) {
        val doc = profilePreview!!
        DoctorProfileDialog(
            doctor = doc,
            onDismiss = { showProfileDialog = false },
            onSelect = {
                selecting = true
                scope.launch {
                    val result = BackendApi.assignDoctor(
                        token = tokenProvider(),
                        doctorUserId = doc.userId,
                        patientProfileJson = userProfileToJson(userProfile)
                    )
                    selecting = false
                    showProfileDialog = false
                    if (result != null) {
                        assignment = BackendApi.getPatientAssignment(tokenProvider())
                        onAssignmentChanged(assignment)
                        val spec = formatDoctorSpecialtyLabel(doc.specialty)
                        status = "Вы выбрали врача ${doc.fullName.ifBlank { doc.username }}" +
                            if (spec.isNotBlank()) " ($spec)" else "."
                    } else {
                        status = "Не удалось выбрать врача."
                    }
                }
            },
            selectInProgress = selecting
        )
    }
}
