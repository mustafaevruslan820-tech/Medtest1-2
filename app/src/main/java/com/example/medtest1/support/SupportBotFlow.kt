package com.example.medtest1.support

import com.example.medtest1.network.SupportMessage

enum class SupportBotFlowState {
    Idle,
    Greeted,
    AppIssue,
    FeatureIssue,
    Escalated
}

object SupportBotScripts {
    const val GREETING =
        "Здравствуйте! Я Бот-Умник — помощник техподдержки Medtest. " +
            "Помогу с типовыми вопросами по приложению. Если понадобится живой специалист — подключу Умника. " +
            "Этот чат для сообщений об ошибках, подсказок по функциям и связи с техподдержкой."

    const val APP_ISSUE_REPLY =
        "Если приложение ведёт себя странно: полностью закройте его, проверьте интернет и обновите версию. " +
            "Опишите, на каком экране и после какого действия появилась ошибка — при необходимости передам Умнику."

    const val FEATURE_ISSUE_REPLY =
        "Кратко по разделам: лечение — главный экран, дневник — в нижнем меню, профиль и настройки — через иконку профиля. " +
            "Напишите, какой раздел не нашли — подскажу пошагово."

    const val ESCALATION_REPLY =
        "Бот-Умник подключил специалиста Умника. Он ответит в этом чате — обычно в ближайшее время. " +
            "Можете подробно описать вопрос в поле ниже."

    const val CHIP_HELLO = "Здравствуйте"
    const val CHIP_APP = "Проблема с приложением — ошибка"
    const val CHIP_FEATURE = "Проблема с функционалом — непонятно где что"
    const val CHIP_OTHER = "Другое"

    const val USER_TOPIC_APP = "Проблема с приложением — ошибка"
    const val USER_TOPIC_FEATURE = "Проблема с функционалом — непонятно, где что находится"
    const val USER_TOPIC_OTHER = "Другое — нужен специалист Умник"
    const val USER_CALL_UMNIK = "Позвать Умника — нужен специалист"

    val ESCALATION_USER_TEXTS = setOf(USER_TOPIC_OTHER, USER_CALL_UMNIK)

    fun localBotMessage(text: String, createdAt: Long = System.currentTimeMillis()): SupportMessage =
        SupportMessage(
            id = -createdAt,
            conversationId = 0L,
            sender = "bot",
            text = text,
            createdAt = createdAt
        )
}
