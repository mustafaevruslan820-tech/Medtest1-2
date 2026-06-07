package com.example.medtest1.doctor

import com.example.medtest1.network.ApiActionResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

data class SpecialtyVisual(
    val icon: ImageVector,
    val accent: Color,
    val container: Color
)

fun formatDoctorSpecialtyLabel(raw: String): String {
    val key = raw.trim().lowercase(Locale.getDefault())
    if (key.isBlank()) return ""
    return when {
        "педиатр" in key -> "Педиатр"
        "терапевт" in key || "терап" in key -> "Терапевт"
        "кардиолог" in key -> "Кардиолог"
        "хирург" in key -> "Хирург"
        "невролог" in key -> "Невролог"
        "дерматолог" in key -> "Дерматолог"
        else -> raw.trim().replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }
}

fun specialtyVisual(specialty: String, scheme: ColorScheme): SpecialtyVisual {
    val key = specialty.lowercase(Locale.getDefault())
    return when {
        "педиатр" in key -> SpecialtyVisual(
            Icons.Filled.ChildCare,
            Color(0xFF38BDF8),
            Color(0xFF0C4A6E)
        )
        "терапевт" in key || "терап" in key -> SpecialtyVisual(
            Icons.Filled.HealthAndSafety,
            Color(0xFF2DD4BF),
            Color(0xFF134E4A)
        )
        "хирург" in key -> SpecialtyVisual(
            Icons.Filled.MedicalServices,
            Color(0xFFF87171),
            Color(0xFF7F1D1D)
        )
        "кардиолог" in key -> SpecialtyVisual(
            Icons.Filled.Favorite,
            Color(0xFFFB7185),
            Color(0xFF881337)
        )
        "невролог" in key || "псих" in key -> SpecialtyVisual(
            Icons.Filled.Psychology,
            Color(0xFFA78BFA),
            Color(0xFF4C1D95)
        )
        "дерматолог" in key -> SpecialtyVisual(
            Icons.Filled.Science,
            Color(0xFF34D399),
            Color(0xFF064E3B)
        )
        else -> SpecialtyVisual(
            Icons.Filled.LocalHospital,
            scheme.primary,
            scheme.primaryContainer
        )
    }
}

fun apiActionErrorMessage(
    default: String,
    result: ApiActionResult,
    knownErrors: Map<String, String> = emptyMap()
): String {
    result.error?.let { err ->
        knownErrors[err]?.let { return it }
        when (err) {
            "forbidden" -> return "Нет доступа к операции."
            "db_update_failed" -> return "Ошибка базы на сервере. Перезапустите backend."
            "not_found" -> return "На сервере нет этой функции. Обновите backend."
        }
    }
    return when {
        result.httpCode == 404 -> "На сервере нет этой функции. Обновите backend."
        result.httpCode == 403 -> "Нет доступа к операции."
        result.httpCode in 500..599 -> "Ошибка сервера (${result.httpCode})."
        result.httpCode == -1 -> "Нет связи с сервером."
        else -> default
    }
}
