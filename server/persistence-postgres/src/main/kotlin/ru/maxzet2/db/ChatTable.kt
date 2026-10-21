@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package ru.maxzet2.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp


object ChatTable : Table("chats") {
    val id = uuid("id")
    val title = text("title")
    val creatorId = uuid("creator_id")
    val systemPrompt = text("system_prompt")
    val currentMessageId = uuid("active_leaf_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}