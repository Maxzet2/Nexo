package ru.maxzet2.common

import kotlinx.serialization.Serializable
import ru.maxzet2.chat.Message
import ru.maxzet2.chat.MessageStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class MessageResponse(
    val id: Uuid,
    val chatId: Uuid,
    val parentId: Uuid?,
    val role: Role,
    val modelName: String?,
    val content: String,
    val status: MessageStatus,
    val createdAt: String,
) {
    constructor(message: Message) : this(
        message.id, message.chatId, message.parentId, message.role,
        message.modelName, message.content, message.status, message.createdAt.toString(),
    )
}
