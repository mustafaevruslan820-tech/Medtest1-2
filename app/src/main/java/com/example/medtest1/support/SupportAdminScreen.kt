package com.example.medtest1.support

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    val snackbarHostState = remember { SnackbarHostState() }

    var conversations by remember { mutableStateOf(emptyList<SupportConversation>()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var messages by remember { mutableStateOf(emptyList<SupportMessage>()) }
    var draft by remember { mutableStateOf("") }
    var adminBanner by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<SupportMessage?>(null) }

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
        if (key.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Введите ADMIN_KEY в Настройки.") }
            return
        }
        if (text.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Введите текст ответа.") }
            return
        }
        isSending = true
        scope.launch {
            val result: SendSupportMessageResult = runCatching {
                SupportMessageSender.sendAdminMessage(
                    adminKey = key,
                    conversationId = cid,
                    text = text,
                    replyToMessageId = replyTarget?.id
                )
            }.getOrElse {
                SendSupportMessageResult(ok = false, error = "network_error")
            }
            if (result.ok) {
                draft = ""
                replyTarget = null
                refreshMessages()
            } else {
                val msg = when (result.error) {
                    "empty_message", "empty_text" -> "Сообщение не может быть пустым."
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
        containerColor = SupportChatBackground,
        topBar = {
            TopAppBar(
                title = { Text("Панель поддержки", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SupportIncomingBubble,
                    titleContentColor = Color.White
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SupportChatBackground)
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
                                val urgent = c.needsAdminAttention && !active
                                Surface(
                                    color = when {
                                        active -> scheme.primary.copy(alpha = 0.12f)
                                        urgent -> scheme.error.copy(alpha = 0.14f)
                                        else -> scheme.surface
                                    },
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
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (urgent) {
                                                    Text(
                                                        "Умник",
                                                        color = scheme.error,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                    )
                                                }
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
                                        }
                                        Text(c.email, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedId == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Выберите диалог.", color = SupportChatTime)
                }
            } else {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Нажмите на сообщение, чтобы ответить",
                        style = MaterialTheme.typography.bodySmall,
                        color = SupportChatTime
                    )
                        val peerName = conversations.firstOrNull { it.id == selectedId }
                            ?.username
                            ?.ifBlank { null }
                            ?: "Пользователь"
                        SupportMessagesList(
                            messages = messages,
                            viewerIsAdmin = true,
                            userDisplayName = peerName,
                            userPhotoModel = null,
                            replyTargetId = replyTarget?.id,
                            onSelectForReply = { replyTarget = it },
                            modifier = Modifier.weight(1f),
                            emptyText = "В этом диалоге пока нет сообщений."
                        )
                }
            }

            replyTarget?.let { target ->
                val peerName = conversations.firstOrNull { it.id == selectedId }
                    ?.username
                    ?.ifBlank { null }
                    ?: "Пользователь"
                SupportReplyDraftBar(
                    replyTarget = target,
                    viewerIsAdmin = true,
                    userDisplayName = peerName,
                    onClear = { replyTarget = null }
                )
            }

            SupportChatInputBar(
                draft = draft,
                onDraftChange = { draft = it },
                isSending = isSending,
                enabled = selectedId != null,
                onSend = { sendMessage() },
                messageLabel = "Ответ"
            )
        }
    }
}
