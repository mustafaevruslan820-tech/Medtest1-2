package com.example.medtest1.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medtest1.data.UserProfile
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.SendSupportMessageResult
import com.example.medtest1.network.SupportMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    modifier: Modifier = Modifier,
    tokenProvider: () -> String,
    userProfile: UserProfile?,
    username: String,
    onMessagesSeen: (List<SupportMessage>) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val displayName = remember(userProfile, username) { userDisplayName(userProfile, username) }
    val photoModel = remember(userProfile?.photoUri) { userPhotoModel(userProfile?.photoUri) }

    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(emptyList<SupportMessage>()) }
    val localBotMessages = remember { mutableStateListOf<SupportMessage>() }
    var botFlowState by remember { mutableStateOf(SupportBotFlowState.Idle) }
    var showQuickChips by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<SupportMessage?>(null) }

    val displayMessages = (messages + localBotMessages.toList()).sortedBy { it.createdAt }

    val quickReplyChips = if (!showQuickChips) {
        emptyList()
    } else {
        when (botFlowState) {
            SupportBotFlowState.Idle -> listOf(SupportBotScripts.CHIP_HELLO)
            SupportBotFlowState.Greeted -> listOf(
                SupportBotScripts.CHIP_APP,
                SupportBotScripts.CHIP_FEATURE,
                SupportBotScripts.CHIP_OTHER
            )
            else -> emptyList()
        }
    }

    DisposableEffect(Unit) {
        SupportChatSession.isUserInChat = true
        onDispose { SupportChatSession.isUserInChat = false }
    }

    fun syncBotFlowFromServer(serverMessages: List<SupportMessage>) {
        when {
            serverMessages.any { it.sender == "user" && it.text in SupportBotScripts.ESCALATION_USER_TEXTS } ->
                botFlowState = SupportBotFlowState.Escalated
            serverMessages.any { it.text == SupportBotScripts.USER_TOPIC_APP } ->
                botFlowState = SupportBotFlowState.AppIssue
            serverMessages.any { it.text == SupportBotScripts.USER_TOPIC_FEATURE } ->
                botFlowState = SupportBotFlowState.FeatureIssue
            serverMessages.isNotEmpty() && botFlowState == SupportBotFlowState.Idle ->
                botFlowState = SupportBotFlowState.Greeted
        }
        if (botFlowState == SupportBotFlowState.Escalated) {
            showQuickChips = false
        }
    }

    fun anchorAfterUser(userText: String?): Long {
        val userMsg = if (userText != null) {
            messages.lastOrNull { it.sender == "user" && it.text == userText }
        } else {
            messages.lastOrNull { it.sender == "user" }
        }
        return (userMsg?.createdAt ?: System.currentTimeMillis()) + 1L
    }

    fun setLocalBotMessage(text: String, createdAt: Long) {
        localBotMessages.removeAll { it.sender == "bot" && it.text == text }
        localBotMessages.add(SupportBotScripts.localBotMessage(text, createdAt))
    }

    fun reanchorLocalBotMessages() {
        if (messages.isEmpty()) return
        when (botFlowState) {
            SupportBotFlowState.Escalated -> {
                val userEsc = messages.lastOrNull { m ->
                    m.sender == "user" && m.text in SupportBotScripts.ESCALATION_USER_TEXTS
                }
                val firstAdminAfter = userEsc?.let { esc ->
                    messages.firstOrNull { m ->
                        m.sender == "admin" && m.createdAt > esc.createdAt
                    }
                }
                var botTs = (userEsc?.createdAt ?: System.currentTimeMillis()) + 1L
                if (firstAdminAfter != null && botTs >= firstAdminAfter.createdAt) {
                    botTs = firstAdminAfter.createdAt - 1L
                }
                setLocalBotMessage(SupportBotScripts.ESCALATION_REPLY, botTs)
            }
            SupportBotFlowState.AppIssue -> {
                setLocalBotMessage(
                    SupportBotScripts.APP_ISSUE_REPLY,
                    anchorAfterUser(SupportBotScripts.USER_TOPIC_APP)
                )
            }
            SupportBotFlowState.FeatureIssue -> {
                setLocalBotMessage(
                    SupportBotScripts.FEATURE_ISSUE_REPLY,
                    anchorAfterUser(SupportBotScripts.USER_TOPIC_FEATURE)
                )
            }
            SupportBotFlowState.Greeted -> {
                if (localBotMessages.none { it.text == SupportBotScripts.GREETING }) {
                    setLocalBotMessage(SupportBotScripts.GREETING, anchorAfterUser(null))
                }
            }
            SupportBotFlowState.Idle -> Unit
        }
    }

    suspend fun refresh() {
        val token = tokenProvider()
        if (token.isBlank()) return
        val loaded = runCatching { BackendApi.getSupportMessages(token) }.getOrDefault(messages)
        messages = loaded
        syncBotFlowFromServer(loaded)
        reanchorLocalBotMessages()
        onMessagesSeen(loaded)
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(2000)
        }
    }

    fun sendToServer(text: String) {
        val token = tokenProvider()
        if (token.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Сессия истекла. Войдите заново.") }
            return
        }
        isSending = true
        scope.launch {
            val result: SendSupportMessageResult = runCatching {
                SupportMessageSender.sendUserMessage(
                    token = token,
                    text = text,
                    replyToMessageId = replyTarget?.id
                )
            }.getOrElse {
                SendSupportMessageResult(ok = false, error = "network_error")
            }
            if (result.ok) {
                replyTarget = null
                refresh()
            } else {
                val msg = when (result.error) {
                    "empty_message", "empty_text" -> "Сообщение не может быть пустым."
                    "network_error" -> "Нет связи с сервером. Проверьте интернет и backend."
                    else -> "Сообщение не отправлено (${result.error ?: "ошибка ${result.httpCode}"})."
                }
                snackbarHostState.showSnackbar(msg)
            }
            isSending = false
        }
    }

    fun sendMessage() {
        val text = draft.trim()
        if (text.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Введите текст сообщения.") }
            return
        }
        draft = ""
        sendToServer(text)
    }

    fun escalateToUmnik(userText: String) {
        botFlowState = SupportBotFlowState.Escalated
        showQuickChips = false
        if (messages.none { it.sender == "user" && it.text == userText }) {
            sendToServer(userText)
        } else {
            reanchorLocalBotMessages()
        }
    }

    fun callUmnik() {
        escalateToUmnik(SupportBotScripts.USER_CALL_UMNIK)
    }

    fun onQuickReply(chip: String) {
        when (chip) {
            SupportBotScripts.CHIP_HELLO -> {
                if (botFlowState != SupportBotFlowState.Idle) return
                setLocalBotMessage(SupportBotScripts.GREETING, anchorAfterUser(null))
                botFlowState = SupportBotFlowState.Greeted
            }
            SupportBotScripts.CHIP_APP -> {
                if (botFlowState == SupportBotFlowState.AppIssue ||
                    botFlowState == SupportBotFlowState.Escalated
                ) return
                if (botFlowState == SupportBotFlowState.Idle) {
                    setLocalBotMessage(SupportBotScripts.GREETING, anchorAfterUser(null))
                    botFlowState = SupportBotFlowState.Greeted
                }
                setLocalBotMessage(SupportBotScripts.APP_ISSUE_REPLY, anchorAfterUser(SupportBotScripts.USER_TOPIC_APP))
                botFlowState = SupportBotFlowState.AppIssue
                sendToServer(SupportBotScripts.USER_TOPIC_APP)
            }
            SupportBotScripts.CHIP_FEATURE -> {
                if (botFlowState == SupportBotFlowState.FeatureIssue ||
                    botFlowState == SupportBotFlowState.Escalated
                ) return
                if (botFlowState == SupportBotFlowState.Idle) {
                    setLocalBotMessage(SupportBotScripts.GREETING, anchorAfterUser(null))
                    botFlowState = SupportBotFlowState.Greeted
                }
                setLocalBotMessage(
                    SupportBotScripts.FEATURE_ISSUE_REPLY,
                    anchorAfterUser(SupportBotScripts.USER_TOPIC_FEATURE)
                )
                botFlowState = SupportBotFlowState.FeatureIssue
                sendToServer(SupportBotScripts.USER_TOPIC_FEATURE)
            }
            SupportBotScripts.CHIP_OTHER -> {
                if (botFlowState == SupportBotFlowState.Escalated) return
                if (botFlowState == SupportBotFlowState.Idle) {
                    setLocalBotMessage(SupportBotScripts.GREETING, anchorAfterUser(null))
                }
                escalateToUmnik(SupportBotScripts.USER_TOPIC_OTHER)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = SupportChatBackground,
        topBar = {
            SupportChatHeader(
                userDisplayName = displayName,
                onBack = onBack
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SupportChatBackground)
                .padding(inner)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Нажмите на сообщение, чтобы ответить",
                style = MaterialTheme.typography.bodySmall,
                color = SupportChatTime,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            SupportMessagesList(
                messages = displayMessages,
                viewerIsAdmin = false,
                userDisplayName = displayName,
                userPhotoModel = photoModel,
                replyTargetId = replyTarget?.id,
                onSelectForReply = { replyTarget = it },
                modifier = Modifier.weight(1f),
                emptyText = "Нажмите «${SupportBotScripts.CHIP_HELLO}» или напишите ${SupportAgentInfo.BOT_NAME}у."
            )
            replyTarget?.let { target ->
                SupportReplyDraftBar(
                    replyTarget = target,
                    viewerIsAdmin = false,
                    userDisplayName = displayName,
                    onClear = { replyTarget = null }
                )
            }
            SupportTypingIndicator(visible = isSending)
            SupportChatActionButtons(
                showQuickChipsButton = !showQuickChips || quickReplyChips.isEmpty(),
                onShowQuickChips = {
                    showQuickChips = true
                    if (botFlowState == SupportBotFlowState.Escalated) {
                        botFlowState = SupportBotFlowState.Greeted
                    }
                },
                onCallUmnik = { callUmnik() },
                enabled = !isSending
            )
            SupportQuickReplyBar(
                chips = quickReplyChips,
                enabled = !isSending,
                onChipClick = { onQuickReply(it) }
            )
            SupportChatInputBar(
                draft = draft,
                onDraftChange = { draft = it },
                isSending = isSending,
                enabled = true,
                onSend = { sendMessage() },
                messageLabel = "Сообщение"
            )
        }
    }
}
