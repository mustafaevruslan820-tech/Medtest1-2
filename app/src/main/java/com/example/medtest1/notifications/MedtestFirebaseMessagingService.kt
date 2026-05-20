package com.example.medtest1.notifications

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MedtestFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
        if (inForeground) return

        val title = message.notification?.title ?: "Техподдержка"
        if (title.contains("поддерж", ignoreCase = true) || message.data["type"] == "support_reply") {
            val body = message.notification?.body ?: message.data["body"] ?: "Вам ответили на сообщение"
            SupportChatNotifier.showNewAdminReply(applicationContext, body)
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
