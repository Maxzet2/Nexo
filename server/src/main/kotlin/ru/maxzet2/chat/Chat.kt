package ru.maxzet2.chat

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Chat @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    var title: String,
    val creatorId: Uuid,
    val systemPrompt: String,
//    val collaboratorsId: List<Uuid>, здесь предполагается что можно делиться чатом и настраивать разные права доступа
    var currentMessageId: Uuid?,
    val createdAt: Instant,
    var updatedAt: Instant
)