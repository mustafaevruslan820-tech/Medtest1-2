package com.example.medtest1.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medtest1.network.CareEvent
import com.example.medtest1.network.DoctorPrescription
import com.example.medtest1.network.TreatmentReportSummary

enum class CareMilestoneKind {
    Assigned,
    Prescription,
    TreatmentStarted,
    FirstDose,
    ReportSent,
    DoctorConclusion
}

data class CareMilestone(
    val kind: CareMilestoneKind,
    val label: String,
    val done: Boolean
)

fun computeDoctorViewMilestones(
    prescriptions: List<DoctorPrescription>,
    reports: List<TreatmentReportSummary>,
    careEvents: List<CareEvent>
): List<CareMilestone> {
    val hasRx = prescriptions.isNotEmpty() ||
        careEvents.any { it.eventType == "prescription_sent" }
    val treatmentStarted = prescriptions.any { it.patientStatus == "accepted" } ||
        careEvents.any { it.eventType == "prescription_accepted" }
    val firstDose = careEvents.any { it.eventType == "first_dose_taken" }
    val reportSent = reports.isNotEmpty()
    val doctorResponded = reports.any { it.doctorSignedAt != null }

    return listOf(
        CareMilestone(CareMilestoneKind.Assigned, "1", true),
        CareMilestone(CareMilestoneKind.Prescription, "2", hasRx),
        CareMilestone(CareMilestoneKind.TreatmentStarted, "3", treatmentStarted),
        CareMilestone(CareMilestoneKind.FirstDose, "4", firstDose),
        CareMilestone(CareMilestoneKind.ReportSent, "5", reportSent),
        CareMilestone(CareMilestoneKind.DoctorConclusion, "6", doctorResponded)
    )
}

fun computePatientViewMilestones(
    hasAssignment: Boolean,
    prescriptions: List<DoctorPrescription>,
    reports: List<TreatmentReportSummary>,
    careEvents: List<CareEvent>
): List<CareMilestone> {
    val hasRx = prescriptions.isNotEmpty() ||
        careEvents.any { it.eventType == "prescription_sent" }
    val treatmentStarted = prescriptions.any { it.patientStatus == "accepted" } ||
        careEvents.any { it.eventType == "prescription_accepted" }
    val firstDose = careEvents.any { it.eventType == "first_dose_taken" }

    return listOf(
        CareMilestone(CareMilestoneKind.Assigned, "1", hasAssignment),
        CareMilestone(CareMilestoneKind.Prescription, "2", hasRx),
        CareMilestone(CareMilestoneKind.TreatmentStarted, "3", treatmentStarted),
        CareMilestone(CareMilestoneKind.FirstDose, "4", firstDose),
        CareMilestone(CareMilestoneKind.ReportSent, "5", reports.isNotEmpty()),
        CareMilestone(
            CareMilestoneKind.DoctorConclusion,
            "6",
            reports.any { it.doctorSignedAt != null }
        )
    )
}

fun pendingPrescription(prescriptions: List<DoctorPrescription>): DoctorPrescription? =
    prescriptions.lastOrNull {
        val status = it.patientStatus.trim().lowercase()
        status.isBlank() || status == "pending" || status == "null"
    }

fun hasFirstDoseRecorded(careEvents: List<CareEvent>): Boolean =
    careEvents.any { it.eventType == "first_dose_taken" }

@Composable
fun CareMilestoneRow(
    milestones: List<CareMilestone>,
    highlightNewFromIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        milestones.forEachIndexed { index, milestone ->
            val isNew = milestone.done && index == highlightNewFromIndex
            val bg = when {
                isNew -> scheme.tertiary
                milestone.done -> scheme.primary
                else -> scheme.surfaceVariant.copy(alpha = 0.55f)
            }
            val fg = when {
                isNew -> scheme.onTertiary
                milestone.done -> scheme.onPrimary
                else -> scheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(if (isNew) 30.dp else 26.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .then(
                        if (isNew) {
                            Modifier.border(2.dp, Color(0xFFFFD54F), CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = milestone.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
            }
        }
    }
}

fun latestDoneMilestoneIndex(milestones: List<CareMilestone>): Int =
    milestones.indexOfLast { it.done }

enum class ImportantDoctorMessageKind {
    Prescription,
    Plan
}

fun classifyImportantDoctorMessage(text: String, viewerIsPatient: Boolean): ImportantDoctorMessageKind? {
    if (!viewerIsPatient) return null
    val trimmed = text.trimStart()
    return when {
        trimmed.startsWith("Рецепт:", ignoreCase = true) -> ImportantDoctorMessageKind.Prescription
        trimmed.startsWith("Новый план лечения:", ignoreCase = true) -> ImportantDoctorMessageKind.Plan
        trimmed.contains("План лечения:", ignoreCase = true) -> ImportantDoctorMessageKind.Plan
        else -> null
    }
}
