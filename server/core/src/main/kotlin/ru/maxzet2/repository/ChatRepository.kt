package ru.maxzet2.repository

import ru.maxzet2.chat.Chat
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ChatRepository {
    fun create(chat: Chat)

    @OptIn(ExperimentalUuidApi::class)
    fun getForUpdate(id: Uuid): Chat?

    @OptIn(ExperimentalUuidApi::class)
    fun touch(id: Uuid)

    @OptIn(ExperimentalUuidApi::class)
    fun listByCreator(userId: Uuid): List<Chat>
}