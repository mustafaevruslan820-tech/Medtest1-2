package com.example.medtest1.doctor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarMedicationEntry(
    val medicineName: String = "",
    val dosage: String = "",
    val reminderTime: String = "08:00",
    val notes: String = ""
)

fun openTreatmentDatePicker(context: Context, onSelected: (String) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onSelected("%04d-%02d-%02d".format(year, month + 1, day))
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun openTreatmentTimePicker(
    context: Context,
    currentTime: String,
    onSelected: (String) -> Unit
) {
    val parts = currentTime.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(
        context,
        { _, h, m -> onSelected(String.format(Locale.getDefault(), "%02d:%02d", h, m)) },
        hour,
        minute,
        true
    ).show()
}

fun generateTreatmentDateRange(startDate: String, endDate: String): List<String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    formatter.isLenient = false
    return try {
        val start = formatter.parse(startDate) ?: return emptyList()
        val end = formatter.parse(endDate) ?: return emptyList()
        if (start.after(end)) return emptyList()
        val result = mutableListOf<String>()
        val cursor = Calendar.getInstance().apply { time = start }
        val endCalendar = Calendar.getInstance().apply { time = end }
        while (!cursor.after(endCalendar)) {
            result += formatter.format(cursor.time)
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun formatDisplayDate(isoDate: String): String = try {
    val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val parsed = inFmt.parse(isoDate) ?: return isoDate
    outFmt.format(parsed)
} catch (_: Exception) {
    isoDate
}
