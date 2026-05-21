package com.example.medtest1.support

/** Пользователь сейчас на экране чата техподдержки (для подавления уведомлений). */
object SupportChatSession {
    @Volatile
    var isUserInChat: Boolean = false
}
