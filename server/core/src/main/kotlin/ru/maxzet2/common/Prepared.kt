package ru.maxzet2.common

import ru.maxzet2.chat.Chat
import ru.maxzet2.chat.Message

data class Prepared(
    val chat: Chat,
    val user: Message,
    val draft: Message,
)