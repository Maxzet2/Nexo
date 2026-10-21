@file:OptIn(ExperimentalUuidApi::class)

package ru.maxzet2.db

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import ru.maxzet2.chat.*
import ru.maxzet2.common.Role
import ru.maxzet2.repository.ChatRepository
import ru.maxzet2.repository.MessageRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private fun ResultRow.toChat() = Chat(
    id = this[ChatTable.id],
    title = this[ChatTable.title],
    creatorId = this[ChatTable.creatorId],
    systemPrompt = this[ChatTable.systemPrompt],
    currentMessageId = this[ChatTable.currentMessageId],
    createdAt = this[ChatTable.createdAt],
    updatedAt = this[ChatTable.updatedAt],
)

private fun ResultRow.toMessage() = Message(
    id = this[MessageTable.id],
    chatId = this[MessageTable.chatId],
    parentId = this[MessageTable.parentId],
    role = Role.valueOf(this[MessageTable.role].uppercase()),
    modelName = this[MessageTable.model],
    content = this[MessageTable.content],
    status = MessageStatus.valueOf(this[MessageTable.status].uppercase()),
    promptTokens = this[MessageTable.promptTokens],
    completionTokens = this[MessageTable.completionTokens],
    latencyMs = this[MessageTable.latencyMs],
    finishReason = this[MessageTable.finishReason],
    error = this[MessageTable.error],
    createdAt = this[MessageTable.createdAt],
)

class ExposedChatRepository : ChatRepository {

    override fun create(chat: Chat) {
        ChatTable.insert {
            it[id] = chat.id
            it[title] = chat.title
            it[creatorId] = chat.creatorId
            it[systemPrompt] = chat.systemPrompt
            it[currentMessageId] = chat.currentMessageId
            it[createdAt] = chat.createdAt
            it[updatedAt] = chat.updatedAt
        }
    }

    override fun getForUpdate(id: Uuid): Chat? =
        ChatTable.selectAll()
            .where { (ChatTable.id eq id) and ChatTable.deletedAt.isNull() }
            .forUpdate()
            .singleOrNull()
            ?.toChat()

    override fun setCurrentMessage(id: Uuid, messageId: Uuid) {
        ChatTable.update({ ChatTable.id eq id }) {
            it[currentMessageId] = messageId
            it[updatedAt] = kotlin.time.Clock.System.now()
        }
    }

    override fun listByCreator(userId: Uuid): List<Chat> =
        ChatTable.selectAll()
            .where { (ChatTable.creatorId eq userId) and ChatTable.deletedAt.isNull() }
            .orderBy(ChatTable.updatedAt, SortOrder.DESC)
            .map { it.toChat() }
}

class ExposedMessageRepository : MessageRepository {

    override fun insert(m: Message) {
        MessageTable.insert {
            it[id] = m.id
            it[chatId] = m.chatId
            it[parentId] = m.parentId
            it[role] = m.role.name.lowercase()
            it[content] = m.content
            it[status] = m.status.name.lowercase()
            it[model] = m.modelName
            it[promptTokens] = m.promptTokens
            it[completionTokens] = m.completionTokens
            it[latencyMs] = m.latencyMs
            it[finishReason] = m.finishReason
            it[error] = m.error
            it[createdAt] = m.createdAt
        }
    }

    override fun get(id: Uuid): Message? =
        MessageTable.selectAll().where { MessageTable.id eq id }
            .singleOrNull()?.toMessage()

    override fun listByChat(chatId: Uuid): List<Message> =
        MessageTable.selectAll().where { MessageTable.chatId eq chatId }
            .orderBy(MessageTable.createdAt)
            .map { it.toMessage() }

    override fun pathToRoot(id: Uuid): List<Message> {
        val path = ArrayList<Message>()
        var current = get(id)
        while (current != null) {
            path += current
            current = current.parentId?.let { get(it) }
        }
        return path.reversed()
    }

    override fun finish(
        id: Uuid, content: String, status: MessageStatus,
        promptTokens: Int?, completionTokens: Int?, latencyMs: Int?,
        finishReason: String?, error: String?,
    ) {
        MessageTable.update({ MessageTable.id eq id }) {
            it[MessageTable.content] = content
            it[MessageTable.status] = status.name.lowercase()
            it[MessageTable.promptTokens] = promptTokens
            it[MessageTable.completionTokens] = completionTokens
            it[MessageTable.latencyMs] = latencyMs
            it[MessageTable.finishReason] = finishReason
            it[MessageTable.error] = error
        }
    }

    override fun failStale() {
        MessageTable.update({ MessageTable.status eq "generating" }) {
            it[status] = "failed"
            it[error] = "Прервано перезапуском сервера"
        }
    }
}