package com.example.medtest1.doctor

import com.example.medtest1.data.TreatmentPlan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun buildPlansFromPrescription(
    username: String,
    prescriptionText: String,
    treatmentPlanText: String,
    courseId: Long = System.currentTimeMillis()
): List<TreatmentPlan> {
    parseStructuredPlanJson(treatmentPlanText)?.let { structured ->
        return buildPlansFromStructuredPlan(username, structured, courseId)
    }

    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val startCal = Calendar.getInstance()
    val lines = treatmentPlanText.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        val fallbackName = prescriptionText.lines()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(80)
            ?: "Препарат по рецепту"
        return defaultCourse(username, fallbackName, "по рецепту", "08:00", 7, courseId, startCal, dateFmt)
    }

    val plans = mutableListOf<TreatmentPlan>()
    lines.forEach { line ->
        val parts = line.split("|", "—", "–").map { it.trim() }
        val name = parts.getOrNull(0)?.ifBlank { null } ?: line
        val dosage = parts.getOrNull(1)?.ifBlank { null } ?: "по рецепту"
        val time = normalizeReminderTime(parts.getOrNull(2))
        val days = parts.getOrNull(3)?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(1, 30) ?: 7
        val periodStart = dateFmt.format(startCal.time)
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, days - 1) }
        val periodEnd = dateFmt.format(endCal.time)

        repeat(days) { offset ->
            val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val day = dateFmt.format(dayCal.time)
            plans += TreatmentPlan(
                username = username,
                medicineName = name,
                dosage = dosage,
                timesPerDay = 1,
                reminderTime = time,
                startDate = day,
                endDate = day,
                notes = "Период: $periodStart - $periodEnd. Автоплан по рецепту врача.",
                courseId = courseId
            )
        }
    }
    return plans
}

private fun defaultCourse(
    username: String,
    medicineName: String,
    dosage: String,
    time: String,
    days: Int,
    courseId: Long,
    startCal: Calendar,
    dateFmt: SimpleDateFormat
): List<TreatmentPlan> {
    val periodStart = dateFmt.format(startCal.time)
    val endCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, days - 1) }
    val periodEnd = dateFmt.format(endCal.time)
    return (0 until days).map { offset ->
        val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        val day = dateFmt.format(dayCal.time)
        TreatmentPlan(
            username = username,
            medicineName = medicineName,
            dosage = dosage,
            timesPerDay = 1,
            reminderTime = time,
            startDate = day,
            endDate = day,
            notes = "Период: $periodStart - $periodEnd. Автоплан по рецепту врача.",
            courseId = courseId
        )
    }
}

private fun normalizeReminderTime(raw: String?): String {
    val value = raw?.trim().orEmpty()
    val match = Regex("(\\d{1,2})[:.](\\d{2})").find(value)
    if (match != null) {
        val h = match.groupValues[1].padStart(2, '0')
        val m = match.groupValues[2]
        return "$h:$m"
    }
    return "08:00"
}
