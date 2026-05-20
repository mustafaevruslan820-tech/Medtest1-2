package com.example.medtest1.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medtest1.network.SupportMessage

private val TelegramReplyAccent = Color(0xFF2AABEE)
private val TelegramReplyBodyOnBubble = Color(0xFF000000).copy(alpha = 0.62f)
private val TelegramReplyBodyOnPrimary = Color.White.copy(alpha = 0.82f)

internal fun resolveReplyAuthorName(
    previewSender: String?,
    previewAuthor: String?,
    viewerIsAdmin: Boolean
): String = when {
    previewSender == "user" && !viewerIsAdmin -> "Вы"
    previewSender == "admin" && viewerIsAdmin -> "Вы"
    !previewAuthor.isNullOrBlank() -> previewAuthor
    previewSender == "admin" -> "Поддержка"
    previewSender == "user" -> "Пользователь"
    else -> "Сообщение"
}

@Composable
fun SupportMessagesList(
    messages: List<SupportMessage>,
    imageBaseUrl: String,
    viewerIsAdmin: Boolean,
    replyTargetId: Long?,
    onSelectForReply: (SupportMessage) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    emptyText: String = "Нет сообщений."
) {
    val repliedToIds = remember(messages) {
        messages.mapNotNull { it.replyToMessageId?.takeIf { id -> id > 0 } }.toSet()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    if (messages.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val fromMe = if (viewerIsAdmin) message.sender == "admin" else message.sender == "user"
                SupportMessageBubble(
                    message = message,
                    fromMe = fromMe,
                    viewerIsAdmin = viewerIsAdmin,
                    isSelectedForReply = replyTargetId == message.id,
                    isRepliedTo = repliedToIds.contains(message.id),
                    imageBaseUrl = imageBaseUrl,
                    onSelectForReply = { onSelectForReply(message) }
                )
            }
        }
    }
}

@Composable
fun SupportReplyQuote(
    previewText: String?,
    previewSender: String?,
    previewAuthor: String?,
    previewImageUrl: String?,
    isOnPrimary: Boolean,
    viewerIsAdmin: Boolean,
    modifier: Modifier = Modifier
) {
    val authorName = resolveReplyAuthorName(previewSender, previewAuthor, viewerIsAdmin)
    val bodyColor = if (isOnPrimary) TelegramReplyBodyOnPrimary else TelegramReplyBodyOnBubble
    val quoteBg = if (isOnPrimary) {
        Color.Black.copy(alpha = 0.14f)
    } else {
        Color(0xFF2AABEE).copy(alpha = 0.10f)
    }
    val showText = !previewText.isNullOrBlank() &&
        !(previewText == "Фото" && !previewImageUrl.isNullOrBlank())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(quoteBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(TelegramReplyAccent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                authorName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TelegramReplyAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showText) {
                Text(
                    previewText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (!previewImageUrl.isNullOrBlank()) {
                Text(
                    "Фото",
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                    maxLines = 1
                )
            }
        }
        if (!previewImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = previewImageUrl,
                contentDescription = "Превью",
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun SupportMessageBubble(
    message: SupportMessage,
    fromMe: Boolean,
    viewerIsAdmin: Boolean,
    isSelectedForReply: Boolean,
    isRepliedTo: Boolean,
    imageBaseUrl: String,
    onSelectForReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bubbleColor = if (fromMe) scheme.primary else scheme.surface
    val contentColor = if (fromMe) scheme.onPrimary else scheme.onSurface
    val fullImageUrl = message.imageUrl?.let { path ->
        if (path.startsWith("http://", true) || path.startsWith("https://", true)) path
        else imageBaseUrl.trimEnd('/') + path
    }

    val selectionBorder = if (fromMe) Color.White else scheme.primary
    val borderModifier = when {
        isSelectedForReply -> Modifier.border(3.dp, selectionBorder, RoundedCornerShape(14.dp))
        isRepliedTo -> Modifier.border(2.dp, scheme.tertiary, RoundedCornerShape(14.dp))
        else -> Modifier
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start) {
            if (isRepliedTo && !isSelectedForReply) {
                Text(
                    "На это сообщение ответили",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.tertiary,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                )
            }
            Surface(
                color = bubbleColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .then(borderModifier)
                    .clickable(onClick = onSelectForReply)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val replyId = message.replyToMessageId?.takeIf { it > 0 }
                    if (replyId != null) {
                        val quoteText = message.replyPreviewText?.takeIf { it.isNotBlank() }
                            ?: if (!message.replyPreviewImageUrl.isNullOrBlank()) "Фото"
                            else "Сообщение"
                        SupportReplyQuote(
                            previewText = quoteText,
                            previewSender = message.replyPreviewSender,
                            previewAuthor = message.replyPreviewAuthor,
                            previewImageUrl = message.replyPreviewImageUrl?.let { path ->
                                if (path.startsWith("http://", true) || path.startsWith("https://", true)) path
                                else imageBaseUrl.trimEnd('/') + path
                            },
                            isOnPrimary = fromMe,
                            viewerIsAdmin = viewerIsAdmin,
                            modifier = Modifier.height(IntrinsicSize.Min)
                        )
                    }
                    if (!fullImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = fullImageUrl,
                            contentDescription = "Вложение",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (message.text.isNotBlank()) {
                        Text(message.text)
                    }
                }
            }
        }
    }
}

@Composable
fun SupportReplyDraftBar(
    replyTarget: SupportMessage,
    imageBaseUrl: String,
    viewerIsAdmin: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val previewImage = replyTarget.imageUrl?.let { path ->
        if (path.startsWith("http://", true) || path.startsWith("https://", true)) path
        else imageBaseUrl.trimEnd('/') + path
    }
    val previewText = replyTarget.text.ifBlank { null }
    val previewAuthor = when (replyTarget.sender) {
        "admin" -> "Поддержка"
        else -> replyTarget.replyPreviewAuthor
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TelegramReplyAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
        color = scheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SupportReplyQuote(
                previewText = previewText ?: if (!previewImage.isNullOrBlank()) "Фото" else "Сообщение",
                previewSender = replyTarget.sender,
                previewAuthor = previewAuthor,
                previewImageUrl = previewImage,
                isOnPrimary = false,
                viewerIsAdmin = viewerIsAdmin,
                modifier = Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
            )
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Отменить ответ", tint = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SupportChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    isSending: Boolean,
    enabled: Boolean,
    canSend: Boolean = draft.isNotBlank(),
    onAttachImage: () -> Unit,
    onSend: () -> Unit,
    messageLabel: String = "Сообщение"
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        IconButton(
            onClick = onAttachImage,
            enabled = enabled && !isSending
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить фото")
        }
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            label = { Text(messageLabel) },
            enabled = enabled && !isSending
        )
        Button(
            onClick = onSend,
            enabled = enabled && !isSending && canSend
        ) { Text("Отправить") }
    }
}
