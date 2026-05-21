package com.example.medtest1.notifications

import com.example.medtest1.support.SupportAgentInfo
import com.example.medtest1.support.SupportChatSession
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MedtestFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: ""
        val title = message.notification?.title ?: ""
        if (type == "support_escalation" || title.contains("Нужен Умник", ignoreCase = true)) {
            val body = message.notification?.body ?: message.data["body"] ?: "Пользователь ждёт ответа"
            val username = message.data["username"] ?: "Пользователь"
            SupportAdminNotifier.showEscalation(applicationContext, username, body)
            val requestedAt = System.currentTimeMillis()
            applicationContext.getSharedPreferences("medtest_session", MODE_PRIVATE)
                .edit()
                .putLong("admin_last_escalation_at", requestedAt)
                .apply()
            return
        }
        if (SupportChatSession.isUserInChat) return
        if (title.contains("поддерж", ignoreCase = true) || type == "support_reply") {
            val body = message.notification?.body ?: message.data["body"] ?: "Вам ответили на сообщение"
            SupportChatNotifier.showNewAdminReply(
                applicationContext,
                body,
                senderName = SupportAgentInfo.HUMAN_NAME
            )
            val prefs = applicationContext.getSharedPreferences("medtest_session", MODE_PRIVATE)
            val messageId = message.data["messageId"]?.toLongOrNull()
            if (messageId != null) {
                val prev = prefs.getLong("support_last_notified_admin_id", 0L)
                if (messageId > prev) {
                    prefs.edit()
                        .putLong("support_last_notified_admin_id", messageId)
                        .apply()
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        // Token is registered from the main app after login.
    }
}
