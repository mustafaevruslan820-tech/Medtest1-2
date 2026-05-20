package com.example.medtest1.network

import com.example.medtest1.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BackendAuthResult(
    val ok: Boolean,
    val username: String? = null,
    val email: String? = null,
    val token: String? = null,
    val error: String? = null
)

data class SupportMessage(
    val id: Long,
    val conversationId: Long,
    val sender: String,
    val text: String,
    val createdAt: Long,
    val replyToMessageId: Long? = null,
    val imageUrl: String? = null,
    val replyPreviewText: String? = null,
    val replyPreviewSender: String? = null,
    val replyPreviewAuthor: String? = null,
    val replyPreviewImageUrl: String? = null
)

data class SupportConversation(
    val id: Long,
    val userId: Long,
    val username: String,
    val email: String,
    val createdAt: Long,
    val updatedAt: Long,
    val unreadCount: Int = 0
)

data class BackendPasswordResetResult(
    val ok: Boolean,
    val error: String? = null,
    val devCode: String? = null,
    /** true, если код реально отправлен на почту (нет только консоли). */
    val mailDelivered: Boolean? = null
)

data class SendSupportMessageResult(
    val ok: Boolean,
    val error: String? = null,
    val httpCode: Int = -1
)

data class AdminConversationsResult(
    val conversations: List<SupportConversation>,
    val httpCode: Int,
    val unauthorized: Boolean = false,
    val networkError: Boolean = false
)

object BackendApi {
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL
        .trim()
        .let { raw ->
            if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
                raw
            } else {
                "http://$raw"
            }
        }
        .trimEnd('/')

    suspend fun register(
        username: String,
        email: String,
        password: String,
        deviceId: String,
        appVersion: String,
        platform: String = "android"
    ): BackendAuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("username", username)
            .put("email", email)
            .put("password", password)
            .put("deviceId", deviceId)
            .put("appVersion", appVersion)
            .put("platform", platform)
        postJson("$baseUrl/api/auth/register", body)
    }

    suspend fun login(
        usernameOrEmail: String,
        password: String,
        deviceId: String,
        appVersion: String,
        platform: String = "android"
    ): BackendAuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("usernameOrEmail", usernameOrEmail)
            .put("password", password)
            .put("deviceId", deviceId)
            .put("appVersion", appVersion)
            .put("platform", platform)
        postJson("$baseUrl/api/auth/login", body)
    }

    /** Обновить пароль в SQLite по Firebase idToken (после сброса пароля в Firebase). */
    suspend fun syncPasswordFromFirebase(
        idToken: String,
        password: String,
        deviceId: String,
        appVersion: String,
        platform: String = "android"
    ): BackendAuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("idToken", idToken)
            .put("password", password)
            .put("deviceId", deviceId)
            .put("appVersion", appVersion)
            .put("platform", platform)
        postJson("$baseUrl/api/auth/sync-password-from-firebase", body)
    }

    suspend fun trackInstall(
        deviceId: String,
        appVersion: String,
        username: String? = null,
        platform: String = "android"
    ): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("deviceId", deviceId)
            .put("appVersion", appVersion)
            .put("platform", platform)
        if (!username.isNullOrBlank()) body.put("username", username)
        val res = rawPost("$baseUrl/api/events/install", body, bearerToken = null, adminKey = null)
        res.first in 200..299
    }

    suspend fun deleteMe(token: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/api/users/me")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 12_000
            readTimeout = 12_000
        }
        try {
            val code = conn.responseCode
            code in 200..299
        } finally {
            conn.disconnect()
        }
    }

    suspend fun getSupportMessages(token: String): List<SupportMessage> = withContext(Dispatchers.IO) {
        val (code, text) = rawGet("$baseUrl/api/support/messages", bearerToken = token, adminKey = null)
        if (code !in 200..299 || text.isNullOrBlank()) return@withContext emptyList()
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return@withContext emptyList()
        val arr = json.optJSONArray("messages") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(parseSupportMessage(o))
            }
        }
    }

    suspend fun uploadSupportImage(
        token: String,
        imageBase64: String,
        imageMimeType: String
    ): String? = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("imageBase64", imageBase64)
            .put("imageMimeType", imageMimeType)
        val (code, responseText) = rawPost(
            url = "$baseUrl/api/support/upload-image",
            body = body,
            bearerToken = token,
            adminKey = null,
            readTimeoutMs = 90_000
        )
        if (code !in 200..299 || responseText.isNullOrBlank()) return@withContext null
        runCatching { JSONObject(responseText).optString("imageUrl").ifBlank { null } }.getOrNull()
    }

    suspend fun sendSupportMessage(
        token: String,
        text: String,
        replyToMessageId: Long? = null,
        imageUrl: String? = null,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): SendSupportMessageResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", text)
        if (replyToMessageId != null && replyToMessageId > 0) {
            body.put("replyToMessageId", replyToMessageId)
        }
        if (!imageUrl.isNullOrBlank()) {
            body.put("imageUrl", imageUrl)
        } else if (!imageBase64.isNullOrBlank()) {
            body.put("imageBase64", imageBase64)
            body.put("imageMimeType", imageMimeType ?: "image/jpeg")
        }
        val (code, responseText) = rawPost(
            url = "$baseUrl/api/support/messages",
            body = body,
            bearerToken = token,
            adminKey = null,
            readTimeoutMs = 90_000
        )
        if (code in 200..299) {
            val imageMissing = (!imageUrl.isNullOrBlank() || !imageBase64.isNullOrBlank()) &&
                !responseHasSavedImage(responseText)
            if (imageMissing) {
                return@withContext SendSupportMessageResult(ok = false, error = "image_not_saved", httpCode = code)
            }
            return@withContext SendSupportMessageResult(ok = true, httpCode = code)
        }
        val err = runCatching { JSONObject(responseText.orEmpty()).optString("error") }
            .getOrNull()
            ?.ifBlank { null }
        SendSupportMessageResult(
            ok = false,
            error = err ?: if (code < 0) "network_error" else "server_error",
            httpCode = code
        )
    }

    suspend fun markSupportRead(token: String): Boolean = withContext(Dispatchers.IO) {
        val (code, _) = rawPost(
            "$baseUrl/api/support/read",
            JSONObject(),
            bearerToken = token,
            adminKey = null
        )
        code in 200..299
    }

    suspend fun registerFcmToken(token: String, fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("token", fcmToken)
        val (code, _) = rawPost("$baseUrl/api/users/fcm-token", body, bearerToken = token, adminKey = null)
        code in 200..299
    }

    suspend fun forgotPassword(username: String, email: String): BackendPasswordResetResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("username", username.trim())
                .put("email", email.trim().lowercase())
            val (code, text) = rawPost("$baseUrl/api/auth/forgot-password", body, bearerToken = null, adminKey = null)
            when {
                code < 0 -> BackendPasswordResetResult(ok = false, error = "network_error")
                text.isNullOrBlank() -> BackendPasswordResetResult(ok = false, error = "empty_response")
                else -> {
                    val json = runCatching { JSONObject(text) }.getOrNull()
                        ?: return@withContext BackendPasswordResetResult(ok = false, error = "bad_json")
                    val ok = json.optBoolean("ok", false)
                    val err = json.optString("error").ifBlank { null }
                    val dev = json.optString("devCode").ifBlank { null }
                    val mailDel = if (json.has("mailDelivered")) json.optBoolean("mailDelivered") else null
                    when {
                        ok && code in 200..299 -> BackendPasswordResetResult(ok = true, devCode = dev, mailDelivered = mailDel)
                        err != null -> BackendPasswordResetResult(ok = false, error = err)
                        code !in 200..299 -> BackendPasswordResetResult(ok = false, error = "server_error")
                        else -> BackendPasswordResetResult(ok = false, error = "unknown")
                    }
                }
            }
        }

    suspend fun resetPasswordWithCode(
        username: String,
        email: String,
        code: String,
        newPassword: String
    ): BackendPasswordResetResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("username", username.trim())
                .put("email", email.trim().lowercase())
                .put("code", code.trim())
                .put("newPassword", newPassword)
            val (codeHttp, text) = rawPost("$baseUrl/api/auth/reset-password", body, bearerToken = null, adminKey = null)
            when {
                codeHttp < 0 -> BackendPasswordResetResult(ok = false, error = "network_error")
                text.isNullOrBlank() -> BackendPasswordResetResult(ok = false, error = "empty_response")
                else -> {
                    val json = runCatching { JSONObject(text) }.getOrNull()
                        ?: return@withContext BackendPasswordResetResult(ok = false, error = "bad_json")
                    val ok = json.optBoolean("ok", false) && codeHttp in 200..299
                    val err = json.optString("error").ifBlank { null }
                    if (ok) BackendPasswordResetResult(ok = true)
                    else BackendPasswordResetResult(ok = false, error = err ?: "unknown")
                }
            }
        }

    suspend fun verifyResetCode(
        username: String,
        email: String,
        code: String
    ): BackendPasswordResetResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("username", username.trim())
                .put("email", email.trim().lowercase())
                .put("code", code.trim())
            val (codeHttp, text) = rawPost("$baseUrl/api/auth/verify-reset-code", body, bearerToken = null, adminKey = null)
            when {
                codeHttp < 0 -> BackendPasswordResetResult(ok = false, error = "network_error")
                text.isNullOrBlank() -> BackendPasswordResetResult(ok = false, error = "empty_response")
                else -> {
                    val json = runCatching { JSONObject(text) }.getOrNull()
                        ?: return@withContext BackendPasswordResetResult(ok = false, error = "bad_json")
                    val ok = json.optBoolean("ok", false) && codeHttp in 200..299
                    val err = json.optString("error").ifBlank { null }
                    if (ok) BackendPasswordResetResult(ok = true)
                    else BackendPasswordResetResult(ok = false, error = err ?: "unknown")
                }
            }
        }

    suspend fun adminListSupportConversationsDetailed(adminKey: String): AdminConversationsResult =
        withContext(Dispatchers.IO) {
            val (code, text) = rawGet("$baseUrl/api/admin/support/conversations", bearerToken = null, adminKey = adminKey)
            when {
                code < 0 -> AdminConversationsResult(
                    conversations = emptyList(),
                    httpCode = code,
                    networkError = true
                )
                code == 401 -> AdminConversationsResult(
                    conversations = emptyList(),
                    httpCode = code,
                    unauthorized = true
                )
                code !in 200..299 || text.isNullOrBlank() -> AdminConversationsResult(
                    conversations = emptyList(),
                    httpCode = code,
                    networkError = true
                )
                else -> {
                    val json = runCatching { JSONObject(text) }.getOrNull()
                    if (json == null) {
                        return@withContext AdminConversationsResult(
                            conversations = emptyList(),
                            httpCode = code,
                            networkError = true
                        )
                    }
                    val arr = json.optJSONArray("conversations")
                        ?: return@withContext AdminConversationsResult(
                            emptyList(),
                            httpCode = code,
                            networkError = true
                        )
                    val list = buildList {
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            add(
                                SupportConversation(
                                    id = o.optLong("id"),
                                    userId = o.optLong("userId"),
                                    username = o.optString("username"),
                                    email = o.optString("email"),
                                    createdAt = o.optLong("createdAt"),
                                    updatedAt = o.optLong("updatedAt"),
                                    unreadCount = o.optInt("unreadCount", 0)
                                )
                            )
                        }
                    }
                    AdminConversationsResult(conversations = list, httpCode = code)
                }
            }
        }

    suspend fun adminGetSupportMessages(adminKey: String, conversationId: Long): List<SupportMessage> =
        withContext(Dispatchers.IO) {
            val (code, text) = rawGet(
                "$baseUrl/api/admin/support/conversations/$conversationId/messages",
                bearerToken = null,
                adminKey = adminKey
            )
            if (code !in 200..299 || text.isNullOrBlank()) return@withContext emptyList()
            val json = runCatching { JSONObject(text) }.getOrNull() ?: return@withContext emptyList()
            val arr = json.optJSONArray("messages") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(parseSupportMessage(o))
                }
            }
        }

    suspend fun adminUploadSupportImage(
        adminKey: String,
        conversationId: Long,
        imageBase64: String,
        imageMimeType: String
    ): String? = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("imageBase64", imageBase64)
            .put("imageMimeType", imageMimeType)
        val (code, responseText) = rawPost(
            url = "$baseUrl/api/admin/support/conversations/$conversationId/upload-image",
            body = body,
            bearerToken = null,
            adminKey = adminKey,
            readTimeoutMs = 90_000
        )
        if (code !in 200..299 || responseText.isNullOrBlank()) return@withContext null
        runCatching { JSONObject(responseText).optString("imageUrl").ifBlank { null } }.getOrNull()
    }

    suspend fun adminSendSupportMessage(
        adminKey: String,
        conversationId: Long,
        text: String,
        replyToMessageId: Long? = null,
        imageUrl: String? = null,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): SendSupportMessageResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("text", text)
            if (replyToMessageId != null && replyToMessageId > 0) {
                body.put("replyToMessageId", replyToMessageId)
            }
            if (!imageUrl.isNullOrBlank()) {
                body.put("imageUrl", imageUrl)
            } else if (!imageBase64.isNullOrBlank()) {
                body.put("imageBase64", imageBase64)
                body.put("imageMimeType", imageMimeType ?: "image/jpeg")
            }
            val (code, responseText) = rawPost(
                url = "$baseUrl/api/admin/support/conversations/$conversationId/messages",
                body = body,
                bearerToken = null,
                adminKey = adminKey,
                readTimeoutMs = 90_000
            )
            if (code in 200..299) {
                val imageMissing = (!imageUrl.isNullOrBlank() || !imageBase64.isNullOrBlank()) &&
                    !responseHasSavedImage(responseText)
                if (imageMissing) {
                    return@withContext SendSupportMessageResult(ok = false, error = "image_not_saved", httpCode = code)
                }
                return@withContext SendSupportMessageResult(ok = true, httpCode = code)
            }
            val err = runCatching { JSONObject(responseText.orEmpty()).optString("error") }
                .getOrNull()
                ?.ifBlank { null }
            SendSupportMessageResult(
                ok = false,
                error = err ?: if (code < 0) "network_error" else "server_error",
                httpCode = code
            )
        }

    private fun responseHasSavedImage(responseText: String?): Boolean {
        val json = runCatching { JSONObject(responseText.orEmpty()) }.getOrNull() ?: return false
        val message = json.optJSONObject("message") ?: return false
        return !message.optString("imageUrl").isNullOrBlank()
    }

    private fun parseSupportMessage(o: org.json.JSONObject): SupportMessage {
        val replyRaw = if (o.has("replyToMessageId") && !o.isNull("replyToMessageId")) {
            o.optLong("replyToMessageId").takeIf { it > 0 }
        } else {
            null
        }
        return SupportMessage(
            id = o.optLong("id"),
            conversationId = o.optLong("conversationId"),
            sender = o.optString("sender"),
            text = o.optString("text"),
            createdAt = o.optLong("createdAt"),
            replyToMessageId = replyRaw,
            imageUrl = o.optString("imageUrl").ifBlank { null },
            replyPreviewText = o.optString("replyPreviewText").ifBlank { null },
            replyPreviewSender = o.optString("replyPreviewSender").ifBlank { null },
            replyPreviewAuthor = o.optString("replyPreviewAuthor").ifBlank { null },
            replyPreviewImageUrl = o.optString("replyPreviewImageUrl").ifBlank { null }
        )
    }

    private fun postJson(url: String, body: JSONObject): BackendAuthResult {
        val (code, responseText) = rawPost(url, body, bearerToken = null, adminKey = null)
        if (code < 0) {
            return BackendAuthResult(ok = false, error = "network_error")
        }
        if (responseText.isNullOrBlank()) {
            return BackendAuthResult(ok = false, error = "empty_response")
        }
        val json = runCatching { JSONObject(responseText) }.getOrNull()
            ?: return BackendAuthResult(ok = false, error = "bad_json")

        val ok = json.optBoolean("ok", false) && code in 200..299
        val error = json.optString("error").ifBlank { null }
        val token = json.optString("token").ifBlank { null }
        val userObj = json.optJSONObject("user")
        val username = userObj?.optString("username")?.ifBlank { null }
        val email = userObj?.optString("email")?.ifBlank { null }
        return BackendAuthResult(ok = ok, username = username, email = email, token = token, error = error)
    }

    private fun rawGet(url: String, bearerToken: String?, adminKey: String?): Pair<Int, String?> {
        val conn = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (!bearerToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearerToken")
                if (!adminKey.isNullOrBlank()) setRequestProperty("x-admin-key", adminKey)
                connectTimeout = 12_000
                readTimeout = 12_000
            }
        } catch (_: Exception) {
            return -1 to null
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            code to text
        } catch (_: Exception) {
            -1 to null
        } finally {
            conn.disconnect()
        }
    }

    private fun rawPost(
        url: String,
        body: JSONObject,
        bearerToken: String?,
        adminKey: String?,
        readTimeoutMs: Int = 12_000
    ): Pair<Int, String?> {
        val payload = body.toString().toByteArray(Charsets.UTF_8)
        val conn = try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Length", payload.size.toString())
                if (!bearerToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearerToken")
                if (!adminKey.isNullOrBlank()) setRequestProperty("x-admin-key", adminKey)
                connectTimeout = 20_000
                readTimeout = readTimeoutMs
                setFixedLengthStreamingMode(payload.size)
            }
        } catch (_: Exception) {
            return -1 to null
        }
        return try {
            conn.outputStream.use { stream ->
                stream.write(payload)
                stream.flush()
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            code to text
        } catch (_: Exception) {
            -1 to null
        } finally {
            conn.disconnect()
        }
    }
}

