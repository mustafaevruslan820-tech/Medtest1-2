package com.example.medtest1.support

import android.content.Context
import android.net.Uri
import com.example.medtest1.network.BackendApi
import com.example.medtest1.network.SendSupportMessageResult

object SupportMessageSender {
    suspend fun sendUserMessage(
        context: Context,
        token: String,
        text: String,
        replyToMessageId: Long?,
        imageUri: Uri?
    ): SendSupportMessageResult {
        val encoded = imageUri?.let { SupportImageUtils.encodeImageForUpload(context, it) }
        if (imageUri != null && encoded == null) {
            return SendSupportMessageResult(ok = false, error = "invalid_image")
        }
        val (b64, mime) = encoded ?: (null to null)
        val uploadedUrl = b64?.let { BackendApi.uploadSupportImage(token, it, mime!!) }
        return BackendApi.sendSupportMessage(
            token = token,
            text = text,
            replyToMessageId = replyToMessageId,
            imageUrl = uploadedUrl,
            imageBase64 = if (uploadedUrl == null) b64 else null,
            imageMimeType = mime
        )
    }

    suspend fun sendAdminMessage(
        context: Context,
        adminKey: String,
        conversationId: Long,
        text: String,
        replyToMessageId: Long?,
        imageUri: Uri?
    ): SendSupportMessageResult {
        val encoded = imageUri?.let { SupportImageUtils.encodeImageForUpload(context, it) }
        if (imageUri != null && encoded == null) {
            return SendSupportMessageResult(ok = false, error = "invalid_image")
        }
        val uploadedUrl = encoded?.let { (b64, mime) ->
            BackendApi.adminUploadSupportImage(adminKey, conversationId, b64, mime)
        }
        return BackendApi.adminSendSupportMessage(
            adminKey = adminKey,
            conversationId = conversationId,
            text = text,
            replyToMessageId = replyToMessageId,
            imageUrl = uploadedUrl,
            imageBase64 = if (uploadedUrl == null) encoded?.first else null,
            imageMimeType = encoded?.second
        )
    }
}
