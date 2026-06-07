package com.example.medtest1.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.medtest1.network.DoctorAssignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DoctorShiftPatientLine(
    val username: String,
    val assignedAt: Long,
    val prescriptionsCount: Int,
    val reportsCount: Int
)

fun exportDoctorShiftToPdf(
    context: Context,
    uri: Uri,
    doctorName: String,
    shiftStartedAt: Long,
    shiftEndedAt: Long,
    patients: List<DoctorShiftPatientLine>
): Boolean {
    return try {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            isFakeBoldText = true
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
        }
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 48f

        fun newPageIfNeeded(lines: Int = 1) {
            if (y + lines * 18f > 800f) {
                document.finishPage(page)
                pageNumber += 1
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 48f
            }
        }

        canvas.drawText("Отчёт о смене", 40f, y, titlePaint)
        y += 28f
        canvas.drawText("Врач: $doctorName", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("Начало: ${fmt.format(Date(shiftStartedAt))}", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("Окончание: ${fmt.format(Date(shiftEndedAt))}", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("Пациентов за смену: ${patients.size}", 40f, y, bodyPaint)
        y += 26f
        canvas.drawText("Пациенты", 40f, y, sectionPaint)
        y += 22f

        if (patients.isEmpty()) {
            canvas.drawText("За смену не было назначенных пациентов.", 40f, y, bodyPaint)
        } else {
            patients.forEachIndexed { index, patient ->
                newPageIfNeeded(5)
                canvas.drawText(
                    "${index + 1}. ${patient.username} — назначен ${fmt.format(Date(patient.assignedAt))}",
                    40f,
                    y,
                    bodyPaint
                )
                y += 16f
                canvas.drawText(
                    "   Рецептов: ${patient.prescriptionsCount}, отчётов: ${patient.reportsCount}",
                    40f,
                    y,
                    bodyPaint
                )
                y += 20f
            }
        }

        document.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            document.writeTo(out)
        }
        document.close()
        true
    } catch (_: Exception) {
        false
    }
}

fun assignmentsToShiftLines(
    assignments: List<DoctorAssignment>,
    prescriptionCounts: Map<Long, Int>,
    reportCounts: Map<Long, Int>
): List<DoctorShiftPatientLine> = assignments.map { a ->
    DoctorShiftPatientLine(
        username = a.patientUsername,
        assignedAt = a.assignedAt,
        prescriptionsCount = prescriptionCounts[a.id] ?: 0,
        reportsCount = reportCounts[a.id] ?: 0
    )
}
