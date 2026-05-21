package com.example.medtest1.support

import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.SendSupportMessageResult

object SupportMessageSender {
    suspend fun sendUserMessage(
        token: String,
        text: String,
        replyToMessageId: Long?
    ): SendSupportMessageResult =
        BackendApi.sendSupportMessage(
            token = token,
            text = text,
            replyToMessageId = replyToMessageId
        )

    suspend fun sendAdminMessage(
        adminKey: String,
        conversationId: Long,
        text: String,
        replyToMessageId: Long?
    ): SendSupportMessageResult =
        BackendApi.adminSendSupportMessage(
            adminKey = adminKey,
            conversationId = conversationId,
            text = text,
            replyToMessageId = replyToMessageId
        )
}
