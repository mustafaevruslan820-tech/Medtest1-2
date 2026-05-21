package com.example.medtest1.support

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medtest1.network.SupportMessage
import java.util.Calendar

private val QuoteAccent = SupportAccent
private val QuoteBody = Color(0xFFCBD5E1)

private fun formatChatTime(createdAt: Long): String {
    if (createdAt <= 0L) return ""
    val cal = Calendar.getInstance().apply { timeInMillis = createdAt }
    return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

private fun bubbleShape(fromMe: Boolean) = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = if (fromMe) 18.dp else 6.dp,
    bottomEnd = if (fromMe) 6.dp else 18.dp
)

@Composable
fun SupportMessagesList(
    messages: List<SupportMessage>,
    viewerIsAdmin: Boolean,
    userDisplayName: String,
    userPhotoModel: Any?,
    replyTargetId: Long?,
    onSelectForReply: (SupportMessage) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    emptyText: String = "Нет сообщений."
) {
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(SupportChatGradient)
    ) {
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SupportChatAvatar(
                        displayName = SupportAgentInfo.BOT_NAME,
                        photoModel = null,
                        isAgent = true,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(emptyText, color = SupportChatTime, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { "${it.sender}_${it.id}" }) { message ->
                    val fromMe = if (viewerIsAdmin) message.sender == "admin" else message.sender == "user"
                    val canReply = message.id > 0L
                    SupportMessageRow(
                        message = message,
                        fromMe = fromMe,
                        viewerIsAdmin = viewerIsAdmin,
                        userDisplayName = userDisplayName,
                        userPhotoModel = userPhotoModel,
                        isSelectedForReply = canReply && replyTargetId == message.id,
                        onSelectForReply = if (canReply) {
                            { onSelectForReply(message) }
                        } else {
                            {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportMessageRow(
    message: SupportMessage,
    fromMe: Boolean,
    viewerIsAdmin: Boolean,
    userDisplayName: String,
    userPhotoModel: Any?,
    isSelectedForReply: Boolean,
    onSelectForReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBot = message.sender == "bot"
    val isHumanAdmin = message.sender == "admin"
    val rowName = supportSenderDisplayName(message.sender, userDisplayName)
    val nameColor = if (isBot || isHumanAdmin) SupportNameAdmin else SupportNameUser

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!fromMe) {
            SupportChatAvatar(
                displayName = rowName,
                photoModel = if (isHumanAdmin) null else userPhotoModel,
                isAgent = isBot,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                rowName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            SupportMessageBubble(
                message = message,
                fromMe = fromMe,
                viewerIsAdmin = viewerIsAdmin,
                userDisplayName = userDisplayName,
                isSelectedForReply = isSelectedForReply,
                onSelectForReply = onSelectForReply,
                replyEnabled = message.id > 0L
            )
        }
        if (fromMe) {
            Spacer(Modifier.width(8.dp))
            SupportChatAvatar(
                displayName = rowName,
                photoModel = userPhotoModel,
                isAgent = false,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun SupportReplyQuote(
    previewText: String?,
    previewSender: String?,
    previewAuthor: String?,
    isOnPrimary: Boolean,
    viewerIsAdmin: Boolean,
    userDisplayName: String,
    modifier: Modifier = Modifier
) {
    val authorName = resolveReplyAuthorName(previewSender, previewAuthor, viewerIsAdmin, userDisplayName)
    val quoteBg = Color.Black.copy(alpha = if (isOnPrimary) 0.16f else 0.24f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(quoteBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(QuoteAccent)
        )
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                authorName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = QuoteAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!previewText.isNullOrBlank()) {
                Text(
                    previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = QuoteBody,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SupportMessageBubble(
    message: SupportMessage,
    fromMe: Boolean,
    viewerIsAdmin: Boolean,
    userDisplayName: String,
    isSelectedForReply: Boolean,
    onSelectForReply: () -> Unit,
    modifier: Modifier = Modifier,
    replyEnabled: Boolean = true
) {
    val bubbleBrush = if (fromMe) {
        Brush.linearGradient(listOf(SupportOutgoingBubble, SupportOutgoingBubbleHi))
    } else {
        Brush.linearGradient(listOf(SupportIncomingBubble, Color(0xFF273548)))
    }
    val selectionBorder = if (isSelectedForReply) SupportAccent else Color.Transparent

    Surface(
        modifier = modifier
            .animateContentSize(spring(stiffness = Spring.StiffnessMedium))
            .border(
                width = if (isSelectedForReply) 2.dp else 0.dp,
                color = selectionBorder,
                shape = bubbleShape(fromMe)
            )
            .clip(bubbleShape(fromMe))
            .then(
                if (replyEnabled) Modifier.clickable(onClick = onSelectForReply)
                else Modifier
            ),
        color = Color.Transparent,
        shape = bubbleShape(fromMe)
    ) {
        Column(
            modifier = Modifier
                .background(bubbleBrush)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val replyId = message.replyToMessageId?.takeIf { it > 0 }
            if (replyId != null) {
                SupportReplyQuote(
                    previewText = message.replyPreviewText?.takeIf { it.isNotBlank() } ?: "Сообщение",
                    previewSender = message.replyPreviewSender,
                    previewAuthor = message.replyPreviewAuthor,
                    isOnPrimary = fromMe,
                    viewerIsAdmin = viewerIsAdmin,
                    userDisplayName = userDisplayName,
                    modifier = Modifier.height(IntrinsicSize.Min)
                )
            }
            if (message.text.isNotBlank()) {
                Text(
                    message.text,
                    color = SupportChatText,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                formatChatTime(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = SupportChatTime.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun SupportReplyDraftBar(
    replyTarget: SupportMessage,
    viewerIsAdmin: Boolean,
    userDisplayName: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewAuthor = when (replyTarget.sender) {
        "admin" -> SupportAgentInfo.HUMAN_NAME
        "bot" -> SupportAgentInfo.BOT_NAME
        else -> userDisplayName
    }
    val previewText = replyTarget.text.ifBlank { "Сообщение" }

    AnimatedVisibility(
        visible = true,
        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SupportIncomingBubble.copy(alpha = 0.95f),
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SupportReplyQuote(
                    previewText = previewText,
                    previewSender = replyTarget.sender,
                    previewAuthor = previewAuthor,
                    isOnPrimary = false,
                    viewerIsAdmin = viewerIsAdmin,
                    userDisplayName = userDisplayName,
                    modifier = Modifier
                        .weight(1f)
                        .height(IntrinsicSize.Min)
                )
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Отменить ответ", tint = SupportChatTime)
                }
            }
        }
    }
}

@Composable
fun SupportTypingIndicator(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SupportChatAvatar(
                displayName = SupportAgentInfo.BOT_NAME,
                photoModel = null,
                isAgent = true
            )
            Surface(
                color = SupportIncomingBubble,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) { i ->
                        SupportTypingDot(delayMs = i * 180)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportTypingDot(delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "dot$delayMs")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = delayMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(SupportAccent.copy(alpha = alpha))
    )
}

@Composable
fun SupportChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    isSending: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
    messageLabel: String = "Сообщение"
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(messageLabel, color = SupportChatTime) },
            enabled = enabled && !isSending,
            minLines = 1,
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SupportChatText,
                unfocusedTextColor = SupportChatText,
                focusedBorderColor = SupportAccent,
                unfocusedBorderColor = SupportIncomingBubble,
                focusedContainerColor = SupportIncomingBubble.copy(alpha = 0.9f),
                unfocusedContainerColor = SupportIncomingBubble.copy(alpha = 0.75f),
                cursorColor = SupportAccent
            ),
            shape = RoundedCornerShape(20.dp)
        )
        FilledIconButton(
            onClick = onSend,
            enabled = enabled && !isSending && draft.isNotBlank(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = SupportAccentDim,
                contentColor = Color.White,
                disabledContainerColor = SupportIncomingBubble,
                disabledContentColor = SupportChatTime
            )
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
            }
        }
    }
}

@Composable
fun SupportChatHeader(
    userDisplayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0C4A52).copy(alpha = 0.92f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = SupportChatText)
            }
            SupportChatAvatar(
                displayName = SupportAgentInfo.BOT_NAME,
                photoModel = null,
                isAgent = true,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    SupportAgentInfo.BOT_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SupportChatText
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SupportOnlineDot()
                    Text(
                        "онлайн · $userDisplayName",
                        style = MaterialTheme.typography.bodySmall,
                        color = SupportChatTime,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
