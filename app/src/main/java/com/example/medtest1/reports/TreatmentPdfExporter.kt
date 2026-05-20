package com.example.medtest1.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.medtest1.data.TreatmentPlan
import com.example.medtest1.data.WellbeingEntry
import java.text.SimpleDateFormat
import java.util.Locale

fun exportTreatmentPlansToPdf(
    context: Context,
    uri: Uri,
    username: String,
    plans: List<TreatmentPlan>,
    userDisplayName: String? = null,
    birthDate: String? = null,
    wellbeingByDate: Map<String, WellbeingEntry> = emptyMap()
): Boolean {
    return try {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            isFakeBoldText = true
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
        }
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
        }
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = 40f
        fun drawHeader() {
            val nameToPrint = userDisplayName?.trim().orEmpty().ifBlank { username }
            val birthToPrint = birthDate?.trim().orEmpty()
            canvas.drawText("Отчёт по лечению", 40f, y, titlePaint)
            y += 28f
            canvas.drawText("Пациент: $nameToPrint", 40f, y, bodyPaint)
            y += 18f
            if (birthToPrint.isNotBlank()) {
                canvas.drawText("Дата рождения: $birthToPrint", 40f, y, bodyPaint)
                y += 18f
            }
            canvas.drawText(
                "Всего дней с записями: ${(plans.map { it.startDate } + wellbeingByDate.keys).distinct().size}",
                40f,
                y,
                bodyPaint
            )
            y += 26f
            canvas.drawLine(40f, y, 555f, y, hintPaint)
            y += 16f
        }

        fun ensureSpace(lines: Int = 1) {
            val required = lines * 18f
            if (y + required > 800f) {
                document.finishPage(page)
                pageNumber += 1
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
                drawHeader()
            }
        }

        drawHeader()
        val wellbeingPrintedDays = mutableSetOf<String>()

        fun parseDateTimeMillis(date: String, time: String): Long {
            return runCatching {
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                formatter.parse("$date $time")?.time ?: Long.MAX_VALUE
            }.getOrDefault(Long.MAX_VALUE)
        }

        fun drawWrapped(prefix: String, text: String, indent: String = "             ", chunkSize: Int = 80) {
            val chunks = text.chunked(chunkSize)
            chunks.forEachIndexed { index, chunk ->
                ensureSpace(lines = 1)
                val head = if (index == 0) prefix else indent
                canvas.drawText(head + chunk, 55f, y, bodyPaint)
                y += 16f
            }
        }

        val plansByDay = plans.groupBy { it.startDate }
        (plansByDay.keys + wellbeingByDate.keys)
            .distinct()
            .sorted()
            .forEach { day ->
                val dayPlans = plansByDay[day].orEmpty()
                ensureSpace(lines = 3)
                canvas.drawText("День: $day", 40f, y, sectionPaint)
                y += 22f
                canvas.drawText("Записей о приёме: ${dayPlans.size}", 55f, y, bodyPaint)
                y += 16f
                y += 6f

                if (dayPlans.isEmpty()) {
                    ensureSpace(lines = 1)
                    canvas.drawText("• Приёмы лекарств за этот день не добавлены.", 55f, y, hintPaint)
                    y += 18f
                } else {
                    dayPlans
                        .sortedBy { parseDateTimeMillis(it.startDate, it.reminderTime) }
                        .forEachIndexed { index, plan ->
                            ensureSpace(lines = 6)
                            canvas.drawText("${index + 1}) ${plan.medicineName}", 55f, y, bodyPaint); y += 16f
                            canvas.drawText("Дозировка: ${plan.dosage}", 70f, y, bodyPaint); y += 16f
                            canvas.drawText("Приёмов в день: ${plan.timesPerDay}", 70f, y, bodyPaint); y += 16f
                            canvas.drawText("Время: ${plan.reminderTime}", 70f, y, bodyPaint); y += 16f
                            canvas.drawText("Период: ${plan.startDate} - ${plan.endDate}", 70f, y, bodyPaint); y += 16f

                            if (plan.notes.isNotBlank()) {
                                drawWrapped(prefix = "Комментарий к приёму: ", text = plan.notes, indent = "                     ", chunkSize = 70)
                            }
                            y += 8f
                        }
                }

                wellbeingByDate[day]
                    ?.takeIf { (it.comment.isNotBlank() || it.status.isNotBlank()) && wellbeingPrintedDays.add(day) }
                    ?.let { wellbeing ->
                        val statusLabel = when (wellbeing.status) {
                            "excellent" -> "Отлично"
                            "good" -> "Хорошо"
                            "bad" -> "Плохо"
                            else -> "Не указан"
                        }
                        ensureSpace(lines = 2)
                        canvas.drawText("Самочувствие: $statusLabel", 55f, y, bodyPaint)
                        y += 16f
                        if (wellbeing.comment.isNotBlank()) {
                            drawWrapped(prefix = "Комментарий пациента: ", text = wellbeing.comment, indent = "                     ", chunkSize = 70)
                        }
                        y += 8f
                    }
                    ?: run {
                        ensureSpace(lines = 1)
                        canvas.drawText("Самочувствие: запись отсутствует.", 55f, y, hintPaint)
                        y += 16f
                    }

                y += 8f
                canvas.drawLine(40f, y, 555f, y, hintPaint)
                y += 14f
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
