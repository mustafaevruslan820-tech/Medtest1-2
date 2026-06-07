package com.example.medtest1.doctor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.medtest1.data.TreatmentPlan
import com.example.medtest1.data.UserProfile
import com.example.medtest1.data.WellbeingEntry
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.DoctorAssignment
import com.example.medtest1.network.DoctorProfile
import com.example.medtest1.ui.theme.LocalMedAppColors
import kotlinx.coroutines.launch

@Composable
fun DoctorTreatmentSection(
    tokenProvider: () -> String,
    userProfile: UserProfile?,
    treatmentPlans: List<TreatmentPlan>,
    wellbeingEntries: Map<String, WellbeingEntry>,
    onOpenChat: (assignmentId: Long, doctorName: String) -> Unit,
    onAssignmentChanged: (DoctorAssignment?) -> Unit
) {
    val app = LocalMedAppColors.current
    val scope = rememberCoroutineScope()
    var doctors by remember { mutableStateOf<List<DoctorProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var assignment by remember { mutableStateOf<DoctorAssignment?>(null) }
    var profilePreview by remember { mutableStateOf<DoctorProfile?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        doctors = BackendApi.listOnDutyDoctors(tokenProvider())
        assignment = BackendApi.getPatientAssignment(tokenProvider())
        onAssignmentChanged(assignment)
        loading = false
    }

    LaunchedEffect(assignment?.id, treatmentPlans, wellbeingEntries) {
        val a = assignment ?: return@LaunchedEffect
        BackendApi.syncPatientTreatment(
            token = tokenProvider(),
            assignmentId = a.id,
            treatmentSyncJson = treatmentDataToJson(treatmentPlans, wellbeingEntries)
        )
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
                Text(
                    "Ваш врач: ${assignment!!.doctorUsername}",
                    color = app.onHeroMuted
                )
                Button(onClick = {
                    onOpenChat(assignment!!.id, assignment!!.doctorUsername)
                }) {
                    Text("Чат с врачом")
                }
            } else if (doctors.isEmpty()) {
                Text(
                    "Сейчас нет врачей на смене. Попробуйте позже.",
                    color = app.onHeroMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "На смене сейчас:",
                    color = app.onHeroMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(doctors, key = { it.userId }) { doc ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 6 }
                        ) {
                            DoctorChip(doctor = doc, onClick = {
                                profilePreview = doc
                                showProfileDialog = true
                            })
                        }
                    }
                }
            }
            status?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = app.onHeroMuted)
            }
        }
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
                        status = "Вы выбрали врача ${doc.fullName.ifBlank { doc.username }}."
                    } else {
                        status = "Не удалось выбрать врача."
                    }
                }
            },
            selectInProgress = selecting
        )
    }
}

@Composable
private fun DoctorChip(doctor: DoctorProfile, onClick: () -> Unit) {
    val app = LocalMedAppColors.current
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DoctorAvatar(doctor = doctor, size = 64.dp)
            Text(
                doctor.fullName.ifBlank { doctor.username },
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface
            )
            Text(
                doctor.specialty.ifBlank { "Врач" },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary
            )
        }
    }
}
