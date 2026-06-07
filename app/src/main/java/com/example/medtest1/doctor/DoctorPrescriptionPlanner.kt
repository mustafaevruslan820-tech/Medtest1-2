package com.example.medtest1.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.medtest1.ui.MedSurfaceCard
import com.example.medtest1.ui.theme.LocalMedAppColors

@Composable
fun DoctorPrescriptionPlanner(
    modifier: Modifier = Modifier,
    sending: Boolean,
    onPlanReady: (StructuredPrescriptionPlan) -> Unit
) {
    val app = LocalMedAppColors.current
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var startDate by remember { mutableStateOf("Выберите дату") }
    var endDate by remember { mutableStateOf("Выберите дату") }
    var isDayFlowStarted by remember { mutableStateOf(false) }
    var dayDates by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentDayIndex by remember { mutableIntStateOf(0) }
    val dayEntries = remember { mutableStateListOf(CalendarMedicationEntry()) }
    val builtDays = remember { mutableStateListOf<PrescriptionDayPlan>() }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outline,
        cursorColor = scheme.primary
    )

    MedSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Назначить лечение",
                fontWeight = FontWeight.Bold,
                color = app.onHero
            )
            Text(
                "Составьте план так же, как пациент — через календарь и заполнение по дням.",
                style = MaterialTheme.typography.bodySmall,
                color = app.onHeroMuted
            )
            Button(
                onClick = { openTreatmentDatePicker(context) { startDate = it } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending
            ) { Text("Дата начала: ${if (startDate == "Выберите дату") startDate else formatDisplayDate(startDate)}") }
            Button(
                onClick = { openTreatmentDatePicker(context) { endDate = it } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending
            ) { Text("Дата окончания: ${if (endDate == "Выберите дату") endDate else formatDisplayDate(endDate)}") }
            Button(
                onClick = {
                    val range = generateTreatmentDateRange(startDate, endDate)
                    if (range.isNotEmpty()) {
                        builtDays.clear()
                        dayDates = range
                        currentDayIndex = 0
                        isDayFlowStarted = true
                        dayEntries.clear()
                        dayEntries.add(CalendarMedicationEntry())
                    }
                },
                enabled = !sending &&
                    startDate != "Выберите дату" &&
                    endDate != "Выберите дату",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Заполнить план по дням")
            }
        }
    }

    if (isDayFlowStarted && dayDates.isNotEmpty()) {
        val currentDate = dayDates[currentDayIndex]
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("План лечения для пациента", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "День ${currentDayIndex + 1} из ${dayDates.size}: ${formatDisplayDate(currentDate)}",
                        color = scheme.onSurfaceVariant
                    )
                    dayEntries.forEachIndexed { index, entry ->
                        OutlinedTextField(
                            value = entry.medicineName,
                            onValueChange = { dayEntries[index] = entry.copy(medicineName = it) },
                            label = { Text("Препарат ${index + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors
                        )
                        OutlinedTextField(
                            value = entry.dosage,
                            onValueChange = { dayEntries[index] = entry.copy(dosage = it) },
                            label = { Text("Дозировка") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors
                        )
                        Button(
                            onClick = {
                                openTreatmentTimePicker(context, entry.reminderTime) { selected ->
                                    dayEntries[index] = entry.copy(reminderTime = selected)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Время приёма: ${entry.reminderTime}") }
                        OutlinedTextField(
                            value = entry.notes,
                            onValueChange = { dayEntries[index] = entry.copy(notes = it) },
                            label = { Text("Заметка (необязательно)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Button(
                        onClick = { dayEntries.add(CalendarMedicationEntry()) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Добавить препарат в этот день") }
                    Button(
                        onClick = {
                            val meds = dayEntries
                                .filter {
                                    it.medicineName.isNotBlank() &&
                                        it.dosage.isNotBlank() &&
                                        it.reminderTime.matches(Regex("\\d{2}:\\d{2}"))
                                }
                                .map {
                                    PrescriptionMedicineEntry(
                                        medicineName = it.medicineName.trim(),
                                        dosage = it.dosage.trim(),
                                        reminderTime = it.reminderTime.trim(),
                                        notes = it.notes.trim()
                                    )
                                }
                            if (meds.isEmpty()) return@Button
                            builtDays.add(PrescriptionDayPlan(currentDate, meds))
                            if (currentDayIndex < dayDates.lastIndex) {
                                currentDayIndex++
                                dayEntries.clear()
                                dayEntries.add(CalendarMedicationEntry())
                            } else {
                                isDayFlowStarted = false
                                onPlanReady(
                                    StructuredPrescriptionPlan(
                                        startDate = startDate,
                                        endDate = endDate,
                                        days = builtDays.toList()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dayEntries.any {
                            it.medicineName.isNotBlank() &&
                                it.dosage.isNotBlank() &&
                                it.reminderTime.matches(Regex("\\d{2}:\\d{2}"))
                        }
                    ) {
                        Text(
                            if (currentDayIndex < dayDates.lastIndex) {
                                "Сохранить день и перейти дальше"
                            } else {
                                "Отправить план пациенту"
                            }
                        )
                    }
                    Button(
                        onClick = {
                            isDayFlowStarted = false
                            builtDays.clear()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Отмена") }
                }
            }
        }
    }
}
