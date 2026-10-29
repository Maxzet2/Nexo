package ru.maxzet2.chat

import ru.maxzet2.common.Role
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Message @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val chatId: Uuid,
    val parentId: Uuid?,
    val role: Role,
    val modelName: String?,
    val content: String,
    val status: MessageStatus,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val latencyMs: Int? = null,
    val finishReason: String? = null,
    val error: String? = null,
    val createdAt: Instant
)
