package com.example.medtest1.support

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medtest1.BuildConfig
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.SendSupportMessageResult
import com.example.medtest1.network.SupportMessage
import com.example.medtest1.ui.theme.LocalMedAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    modifier: Modifier = Modifier,
    tokenProvider: () -> String,
    imageBaseUrl: String,
    onMessagesSeen: (List<SupportMessage>) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalMedAppColors.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(emptyList<SupportMessage>()) }
    var isSending by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<SupportMessage?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImagePreview by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        pendingImageUri = uri
        pendingImagePreview = uri.toString()
    }

    suspend fun refresh() {
        val token = tokenProvider()
        if (token.isBlank()) return
        val loaded = runCatching { BackendApi.getSupportMessages(token) }.getOrDefault(messages)
        messages = loaded
        onMessagesSeen(loaded)
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(2000)
        }
    }

    fun sendMessage() {
        val token = tokenProvider()
        val text = draft.trim()
        val imageUri = pendingImageUri
        if (token.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Сессия истекла. Войдите заново.") }
            return
        }
        if (text.isBlank() && imageUri == null) {
            scope.launch { snackbarHostState.showSnackbar("Введите текст или прикрепите фото.") }
            return
        }
        isSending = true
        scope.launch {
            val result: SendSupportMessageResult = runCatching {
                SupportMessageSender.sendUserMessage(
                    context = context,
                    token = token,
                    text = text,
                    replyToMessageId = replyTarget?.id,
                    imageUri = imageUri
                )
            }.getOrElse {
                SendSupportMessageResult(ok = false, error = "network_error")
            }
            if (result.ok) {
                draft = ""
                replyTarget = null
                pendingImageUri = null
                pendingImagePreview = null
                refresh()
            } else {
                val msg = when (result.error) {
                    "invalid_image" -> "Сервер не принял фото. Обновите backend на сервере."
                    "image_not_saved" -> "Фото не сохранилось на сервере. Обновите backend (Render) до последней версии."
                    "empty_message", "empty_text" -> "Добавьте текст или фото."
                    "network_error" -> "Нет связи с сервером. Проверьте интернет и backend."
                    else -> "Сообщение не отправлено (${result.error ?: "ошибка ${result.httpCode}"})."
                }
                snackbarHostState.showSnackbar(msg)
            }
            isSending = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Техподдержка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(app.heroGradient)
                .padding(inner)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Нажмите на сообщение, чтобы ответить на него",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Card(colors = CardDefaults.cardColors(containerColor = app.cardOnHero), modifier = Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    SupportMessagesList(
                        messages = messages,
                        imageBaseUrl = imageBaseUrl,
                        viewerIsAdmin = false,
                        replyTargetId = replyTarget?.id,
                        onSelectForReply = { replyTarget = it },
                        modifier = Modifier.fillMaxSize(),
                        emptyText = "Напишите сообщение в поддержку."
                    )
                }
            }

            replyTarget?.let { target ->
                SupportReplyDraftBar(
                    replyTarget = target,
                    imageBaseUrl = imageBaseUrl,
                    viewerIsAdmin = false,
                    onClear = { replyTarget = null }
                )
            }

            pendingImagePreview?.let { preview ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = preview,
                            contentDescription = "Выбранное фото",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text("Фото будет отправлено", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            pendingImageUri = null
                            pendingImagePreview = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Убрать фото")
                        }
                    }
                }
            }

            SupportChatInputBar(
                draft = draft,
                onDraftChange = { draft = it },
                isSending = isSending,
                enabled = true,
                canSend = draft.isNotBlank() || pendingImageUri != null,
                onAttachImage = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSend = { sendMessage() }
            )
        }
    }
}

fun backendImageBaseUrl(): String {
    val raw = BuildConfig.BACKEND_BASE_URL.trim()
    val withScheme = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "http://$raw"
    return withScheme.trimEnd('/')
}
