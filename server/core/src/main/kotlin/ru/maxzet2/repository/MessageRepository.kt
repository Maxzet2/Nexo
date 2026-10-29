package ru.maxzet2.repository

import ru.maxzet2.chat.Message
import ru.maxzet2.chat.MessageStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface MessageRepository {
    fun insert(m: Message)
    @OptIn(ExperimentalUuidApi::class)
    fun get(id: Uuid): Message?
    @OptIn(ExperimentalUuidApi::class)
    fun pathToRoot(id: Uuid): List<Message>
    @OptIn(ExperimentalUuidApi::class)
    fun listByChat(chatId: Uuid): List<Message>
    @OptIn(ExperimentalUuidApi::class)
    fun finish(id: Uuid,
               content: String,
               status: MessageStatus,
               promptTokens: Int?,
               completionTokens: Int?,
               latencyMs: Int?,
               finishReason: String?,
               error: String?)
    fun failStale()
}