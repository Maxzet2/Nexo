@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package ru.maxzet2.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object MessageTable : Table("message_nodes") {
    val id = uuid("id")
    val chatId = uuid("conversation_id")
    val parentId = uuid("parent_id").nullable()
    val role = text("role")
    val content = text("content")
    val status = text("status")
    val model = text("model").nullable()
    val promptTokens = integer("prompt_tokens").nullable()
    val completionTokens = integer("completion_tokens").nullable()
    val latencyMs = integer("latency_ms").nullable()
    val finishReason = text("finish_reason").nullable()
    val error = text("error").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}