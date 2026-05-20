package com.example.medtest1.support

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import com.example.medtest1.network.SupportConversation
import com.example.medtest1.network.SupportMessage
import com.example.medtest1.ui.theme.LocalMedAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportAdminScreen(
    modifier: Modifier = Modifier,
    adminKeyProvider: () -> String,
    onBack: () -> Unit
) {
    val app = LocalMedAppColors.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val imageBaseUrl = remember { backendImageBaseUrl() }

    var conversations by remember { mutableStateOf(emptyList<SupportConversation>()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var messages by remember { mutableStateOf(emptyList<SupportMessage>()) }
    var draft by remember { mutableStateOf("") }
    var adminBanner by remember { mutableStateOf<String?>(null) }
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

    suspend fun refreshConversations() {
        val key = adminKeyProvider().trim()
        if (key.isBlank()) {
            adminBanner = "Сначала введите ADMIN_KEY: Настройки → 7 раз нажмите «Настройки»."
            conversations = emptyList()
            return
        }
        val res = runCatching { BackendApi.adminListSupportConversationsDetailed(key) }.getOrNull()
        if (res == null) {
            adminBanner = "Ошибка сети при загрузке диалогов."
            return
        }
        adminBanner = when {
            res.unauthorized -> "Неверный ключ администратора (сервер вернул 401). Проверьте ADMIN_KEY на ПК и в приложении."
            res.networkError -> "Сервер недоступен. Проверьте ${BuildConfig.BACKEND_BASE_URL} и запуск backend."
            else -> null
        }
        conversations = res.conversations
    }

    LaunchedEffect(conversations) {
        if (conversations.isEmpty()) {
            selectedId = null
            return@LaunchedEffect
        }
        val sel = selectedId
        if (sel == null || conversations.none { it.id == sel }) {
            selectedId = conversations.first().id
        }
    }

    suspend fun refreshMessages() {
        val key = adminKeyProvider()
        val cid = selectedId ?: return
        if (key.isBlank()) return
        messages = runCatching { BackendApi.adminGetSupportMessages(key, cid) }.getOrDefault(messages)
    }

    LaunchedEffect(selectedId) {
        replyTarget = null
        pendingImageUri = null
        pendingImagePreview = null
    }

    LaunchedEffect(Unit) {
        while (true) {
            refreshConversations()
            refreshMessages()
            delay(2000)
        }
    }

    fun sendMessage() {
        val key = adminKeyProvider()
        val cid = selectedId ?: return
        val text = draft.trim()
        val imageUri = pendingImageUri
        if (key.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Введите ADMIN_KEY в Настройки.") }
            return
        }
        if (text.isBlank() && imageUri == null) {
            scope.launch { snackbarHostState.showSnackbar("Введите текст или прикрепите фото.") }
            return
        }
        isSending = true
        scope.launch {
            val result: SendSupportMessageResult = runCatching {
                SupportMessageSender.sendAdminMessage(
                    context = context,
                    adminKey = key,
                    conversationId = cid,
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
                refreshMessages()
            } else {
                val msg = when (result.error) {
                    "invalid_image" -> "Сервер не принял фото. Обновите backend."
                    "image_not_saved" -> "Фото не сохранилось на сервере. Обновите backend."
                    "empty_message", "empty_text" -> "Добавьте текст или фото."
                    "network_error" -> "Нет связи с сервером."
                    else -> "Ответ не отправлен (${result.error ?: result.httpCode})."
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
                title = { Text("Панель поддержки") },
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
            Card(colors = CardDefaults.cardColors(containerColor = app.cardOnHero)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Диалоги", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    adminBanner?.let { b ->
                        Text(b, color = scheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (conversations.isEmpty()) {
                        Text("Пока нет обращений.", color = scheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 180.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(conversations, key = { it.id }) { c ->
                                val active = selectedId == c.id
                                val unread = if (active) 0 else c.unreadCount
                                Surface(
                                    color = if (active) scheme.primary.copy(alpha = 0.12f) else scheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedId = c.id }
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(c.username.ifBlank { "user#${c.userId}" })
                                            if (unread > 0) {
                                                Surface(
                                                    color = scheme.error,
                                                    contentColor = scheme.onError,
                                                    shape = RoundedCornerShape(999.dp)
                                                ) {
                                                    Text(
                                                        text = unread.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(c.email, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = app.cardOnHero), modifier = Modifier.weight(1f)) {
                if (selectedId == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Выберите диалог.", color = scheme.onSurfaceVariant)
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Text(
                            "Нажмите на сообщение, чтобы ответить",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SupportMessagesList(
                            messages = messages,
                            imageBaseUrl = imageBaseUrl,
                            viewerIsAdmin = true,
                            replyTargetId = replyTarget?.id,
                            onSelectForReply = { replyTarget = it },
                            modifier = Modifier.weight(1f),
                            emptyText = "В этом диалоге пока нет сообщений."
                        )
                    }
                }
            }

            replyTarget?.let { target ->
                SupportReplyDraftBar(
                    replyTarget = target,
                    imageBaseUrl = imageBaseUrl,
                    viewerIsAdmin = true,
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
                enabled = selectedId != null,
                canSend = draft.isNotBlank() || pendingImageUri != null,
                onAttachImage = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSend = { sendMessage() },
                messageLabel = "Ответ"
            )
        }
    }
}
