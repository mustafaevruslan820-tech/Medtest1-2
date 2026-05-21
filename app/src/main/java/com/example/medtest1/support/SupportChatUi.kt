package com.example.medtest1.support

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.medtest1.data.UserProfile
import com.example.medtest1.ui.AssetImage
import com.example.medtest1.ui.theme.MedTeal300
import com.example.medtest1.ui.theme.MedTeal400
import com.example.medtest1.ui.theme.MedTeal600
import com.example.medtest1.ui.theme.MedTeal700
import com.example.medtest1.ui.theme.MedTeal800
import com.example.medtest1.ui.theme.Slate900
import java.io.File

object SupportAgentInfo {
    const val BOT_NAME = "Бот-Умник"
    const val HUMAN_NAME = "Умник"
    const val AVATAR_ASSET = "assistant_character.png"
    const val SUBTITLE = "Техподдержка приложения"
}

fun supportSenderDisplayName(sender: String, userDisplayName: String): String = when (sender) {
    "bot" -> SupportAgentInfo.BOT_NAME
    "admin" -> SupportAgentInfo.HUMAN_NAME
    "user" -> userDisplayName
    else -> userDisplayName
}

fun isSupportAgentSender(sender: String): Boolean = sender == "admin" || sender == "bot"

internal val SupportChatBackground = Slate900
internal val SupportChatGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF0C3D4A),
        Color(0xFF0F172A)
    )
)
internal val SupportIncomingBubble = Color(0xFF1E293B)
internal val SupportOutgoingBubble = Color(0xFF0F5C66)
internal val SupportOutgoingBubbleHi = Color(0xFF14B8A6)
internal val SupportChatText = Color(0xFFF8FAFC)
internal val SupportChatTime = Color(0xFF94A3B8)
internal val SupportAccent = MedTeal400
internal val SupportAccentDim = MedTeal600
internal val SupportNameAdmin = MedTeal300
internal val SupportNameUser = Color(0xFFBAE6FD)

fun userDisplayName(profile: UserProfile?, username: String): String {
    val full = profile?.fullName?.trim().orEmpty()
    if (full.isNotBlank()) return full
    return username.trim().ifBlank { "Вы" }
}

fun userPhotoModel(photoUri: String?): Any? {
    val t = photoUri?.trim().orEmpty()
    if (t.isBlank()) return null
    if (t.startsWith("/")) return File(t)
    if (t.startsWith("file://")) {
        val path = runCatching { android.net.Uri.parse(t).path }.getOrNull()
        if (!path.isNullOrBlank()) return File(path)
    }
    return t
}

internal fun resolveReplyAuthorName(
    previewSender: String?,
    previewAuthor: String?,
    viewerIsAdmin: Boolean,
    userDisplayName: String
): String = when {
    previewSender == "user" && !viewerIsAdmin -> "Вы"
    previewSender == "admin" && viewerIsAdmin -> "Вы"
    previewSender == "bot" -> SupportAgentInfo.BOT_NAME
    previewSender == "admin" -> SupportAgentInfo.HUMAN_NAME
    previewSender == "user" -> previewAuthor?.takeIf { it.isNotBlank() } ?: userDisplayName
    !previewAuthor.isNullOrBlank() -> previewAuthor
    else -> "Сообщение"
}

@Composable
fun SupportChatAvatar(
    displayName: String,
    photoModel: Any?,
    isAgent: Boolean,
    modifier: Modifier = Modifier.size(40.dp)
) {
    val ringBrush = Brush.linearGradient(listOf(MedTeal400, MedTeal700))
    Box(
        modifier = modifier
            .border(2.dp, ringBrush, CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isAgent) MedTeal800 else SupportIncomingBubble),
        contentAlignment = Alignment.Center
    ) {
        when {
            isAgent -> {
                AssetImage(
                    fileName = SupportAgentInfo.AVATAR_ASSET,
                    contentDescription = SupportAgentInfo.BOT_NAME,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            photoModel != null -> {
                AsyncImage(
                    model = photoModel,
                    contentDescription = displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }
            else -> {
                Text(
                    text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = SupportChatText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SupportOnlineDot(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "online")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "onlineAlpha"
    )
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(SupportAccent.copy(alpha = alpha))
    )
}
