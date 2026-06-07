package com.example.medtest1.doctor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.DoctorAccountSummary
import com.example.medtest1.ui.MedSectionLabel
import com.example.medtest1.ui.MedSubScreenLayout
import com.example.medtest1.ui.MedSurfaceCard
import kotlinx.coroutines.launch

private val SPECIALTIES = listOf(
    "Педиатр",
    "Терапевт",
    "Кардиолог",
    "Невролог",
    "Хирург",
    "Дерматолог"
)

private fun formatCreateDoctorError(error: String?, httpCode: Int): String = when {
    error == "network_error" || httpCode < 0 ->
        "Нет связи с сервером. Проверьте Wi‑Fi и адрес backend в build.gradle."
    error == "unauthorized" || httpCode == 401 ->
        "Нет доступа. Введите ADMIN_KEY с Render в поле ниже или войдите как Admin."
    error == "forbidden" || httpCode == 403 ->
        "Аккаунт не имеет прав администратора на сервере. Укажите ADMIN_KEY."
    error == "user_exists" || httpCode == 409 -> "Такой логин или email уже занят."
    error == "invalid_username" -> "Логин: минимум 3 символа, без пробелов по краям."
    error == "invalid_email" -> "Укажите корректный email."
    error == "invalid_password" -> "Пароль: минимум 6 символов."
    error == "specialty_required" -> "Выберите специальность."
    httpCode == 404 ->
        "API врачей не найден (404). Перезапустите backend из папки backend/ на порту из build.gradle или обновите сервер на Render."
    else -> "Не удалось создать врача (код $httpCode)."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    modifier: Modifier = Modifier,
    tokenProvider: () -> String,
    adminKeyProvider: () -> String,
    onSaveAdminKey: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var doctors by remember { mutableStateOf<List<DoctorAccountSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminKeyDraft by remember { mutableStateOf(adminKeyProvider()) }
    var specialty by remember { mutableStateOf(SPECIALTIES.first()) }
    var specialtyExpanded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Long?>(null) }
    var doctorToDelete by remember { mutableStateOf<DoctorAccountSummary?>(null) }
    var serverWarning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(adminKeyProvider()) {
        adminKeyDraft = adminKeyProvider()
    }

    suspend fun refreshDoctors() {
        loading = true
        doctors = BackendApi.adminListDoctors(tokenProvider(), adminKeyDraft.trim())
        loading = false
    }

    LaunchedEffect(Unit) {
        val health = runCatching { BackendApi.fetchHealth() }.getOrNull()
        serverWarning = when {
            health == null || !health.ok ->
                "Сервер недоступен. Запустите backend из папки backend/ (npm run dev, PORT=8081)."
            !health.doctorApi ->
                "На Render старая версия (нет /api/admin/doctors). Закоммитьте backend/src/doctorApi.js, push в GitHub и снова Deploy."
            else -> null
        }
        refreshDoctors()
    }

    val formValid = username.trim().length >= 3 &&
        email.trim().contains("@") &&
        password.length >= 6 &&
        specialty.isNotBlank()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        MedSubScreenLayout(
            title = "Админ-панель",
            subtitle = "Создание аккаунтов врачей",
            onBack = onBack,
            modifier = Modifier.padding(innerPadding)
        ) {
            serverWarning?.let { warning ->
                MedSurfaceCard {
                    Text(
                        warning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (adminKeyDraft.isBlank() && tokenProvider().isBlank()) {
                MedSurfaceCard {
                    Text(
                        "Для создания врача нужен ADMIN_KEY с сервера (Render → Environment) или вход под Admin с токеном.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400)) + slideInVertically(tween(420)) { it / 6 }
            ) {
                MedSurfaceCard {
                    Text(
                        "Новый врач",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = adminKeyDraft,
                        onValueChange = {
                            adminKeyDraft = it
                            onSaveAdminKey(it.trim())
                        },
                        label = { Text("ADMIN_KEY (с Render)") },
                        placeholder = { Text("dev-admin-key-change-me") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Логин") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            if (username.isNotBlank() && username.trim().length < 3) {
                                Text("Минимум 3 символа")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            if (password.isNotBlank() && password.length < 6) {
                                Text("Минимум 6 символов")
                            }
                        }
                    )
                    ExposedDropdownMenuBox(
                        expanded = specialtyExpanded,
                        onExpandedChange = { specialtyExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = specialty,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Специальность") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = specialtyExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = specialtyExpanded,
                            onDismissRequest = { specialtyExpanded = false }
                        ) {
                            SPECIALTIES.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        specialty = item
                                        specialtyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (!formValid || creating) return@Button
                            creating = true
                            scope.launch {
                                onSaveAdminKey(adminKeyDraft.trim())
                                val result = BackendApi.adminCreateDoctor(
                                    token = tokenProvider(),
                                    adminKey = adminKeyDraft.trim(),
                                    username = username.trim(),
                                    email = email.trim(),
                                    password = password,
                                    specialty = specialty
                                )
                                creating = false
                                if (result.ok) {
                                    val createdName = username.trim()
                                    username = ""
                                    email = ""
                                    password = ""
                                    refreshDoctors()
                                    snackbarHostState.showSnackbar(
                                        "Врач «$createdName» ($specialty) создан. Передайте логин и пароль."
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        formatCreateDoctorError(result.error, result.httpCode)
                                    )
                                }
                            }
                        },
                        enabled = formValid && !creating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (creating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Создать аккаунт врача")
                        }
                    }
                }
            }

            MedSectionLabel("Созданные врачи")
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (doctors.isEmpty()) {
                Text(
                    "Пока нет врачей. После успешного создания список обновится здесь.",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                doctors.forEach { doc ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 }
                    ) {
                        MedSurfaceCard {
                            Text(doc.fullName.ifBlank { doc.username }, fontWeight = FontWeight.SemiBold)
                            Text("${doc.specialty} · ${doc.username}")
                            Text(doc.email, style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { doctorToDelete = doc },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Удалить аккаунт")
                            }
                        }
                    }
                }
            }
        }
    }

    doctorToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { if (deletingId == null) doctorToDelete = null },
            title = { Text("Удалить врача?") },
            text = {
                Text("Аккаунт «${doc.username}» и все связанные данные будут удалены без восстановления.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletingId = doc.id
                        scope.launch {
                            val ok = BackendApi.adminDeleteDoctor(
                                token = tokenProvider(),
                                adminKey = adminKeyDraft.trim(),
                                doctorUserId = doc.id
                            )
                            deletingId = null
                            doctorToDelete = null
                            if (ok) {
                                refreshDoctors()
                                snackbarHostState.showSnackbar("Врач ${doc.username} удалён.")
                            } else {
                                snackbarHostState.showSnackbar("Не удалось удалить врача.")
                            }
                        }
                    },
                    enabled = deletingId == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Удалить") }
            },
            dismissButton = {
                Button(onClick = { doctorToDelete = null }) { Text("Отмена") }
            }
        )
    }
}
