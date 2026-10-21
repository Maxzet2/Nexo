package ru.maxzet2.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import ru.maxzet2.error.ChatNotFoundException
import ru.maxzet2.error.MessageNotFoundException
import ru.maxzet2.error.InvalidRequestException
import ru.maxzet2.chat.Chat
import ru.maxzet2.chat.Message
import ru.maxzet2.chat.MessageStatus
import ru.maxzet2.common.Prepared
import ru.maxzet2.common.Role
import ru.maxzet2.common.SendResult
import ru.maxzet2.repository.ChatRepository
import ru.maxzet2.repository.MessageRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatService(
    private val chats: ChatRepository,
    private val messages: MessageRepository,
    private val models: ModelService
) {
    private val gate = Semaphore(1)

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addMessage(
        userId: Uuid,
        chatId: Uuid,
        text: String,
        modelName: String,
    ): SendResult {
        val prep: Prepared = newSuspendedTransaction(Dispatchers.IO) {
            val chat = chats.getForUpdate(chatId) ?: throw ChatNotFoundException(chatId.toString())
            val user = userMessage(chat.id, chat.currentMessageId, text)
            messages.insert(user)
            val draft = assistantDraft(chat.id, user.id, modelName)
            messages.insert(draft)
            chats.setCurrentMessage(chat.id, draft.id)
            Prepared(chat, user, draft)
        }
        return generateAndFinish(prep, modelName)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addMessage(
        userId: Uuid,
        text: String,
        modelName: String,
        systemPrompt: String,
    ): SendResult {
        val prep: Prepared = newSuspendedTransaction(Dispatchers.IO) {
            val now = now()
            val chat = Chat(
                Uuid.random(),
                text.take(60),
                userId,
                systemPrompt,
                null,
                now,
                now).also {
                    chats.create(it)
                }
            val user = userMessage(chat.id, chat.currentMessageId, text)
            messages.insert(user)
            val draft = assistantDraft(chat.id, user.id, modelName)
            messages.insert(draft)
            chats.setCurrentMessage(chat.id, draft.id)
            Prepared(chat, user, draft)
        }
        return generateAndFinish(prep, modelName)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun edit(nodeId: Uuid, text: String, modelName: String): SendResult {
        val prep: Prepared = newSuspendedTransaction(Dispatchers.IO) {
            val old = messages.get(nodeId) ?: throw MessageNotFoundException(nodeId.toString())
            if (old.role != Role.USER) throw InvalidRequestException("Редактировать можно только user-сообщения")
            val chat = chats.getForUpdate(old.chatId) ?: throw ChatNotFoundException(old.chatId.toString())
            val user = userMessage(chat.id, old.parentId, text)   // «брат» старого узла
            messages.insert(user)
            val draft = assistantDraft(chat.id, user.id, modelName)
            messages.insert(draft)
            chats.setCurrentMessage(chat.id, draft.id)
            Prepared(chat, user, draft)
        }
        return generateAndFinish(prep, modelName)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun generateAndFinish(p: Prepared, modelName: String): SendResult {
        val history: List<Message> = newSuspendedTransaction(Dispatchers.IO) {
            messages.pathToRoot(p.user.id)
        }
        val started = System.nanoTime()
        try {
            val text = gate.withPermit {
                models.generate(modelName, p.chat.systemPrompt, history, maxTokens = 10)
            }
            finish(p.draft.id, text, MessageStatus.DONE, started, null)
        } catch (e: Exception) {
            finish(p.draft.id, "", MessageStatus.FAILED, started, e.message)
            throw e
        }
        return SendResult(p.chat.id, p.user.id, p.draft.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun finish(
        id: Uuid,
        content: String,
        status: MessageStatus,
        startedNanos: Long,
        error: String?,
    ) {
        val latency = ((System.nanoTime() - startedNanos) / 1_000_000).toInt()
        newSuspendedTransaction(Dispatchers.IO) {
            messages.finish(
                id = id,
                content = content,
                status = status,
                promptTokens = null,
                completionTokens = null,
                latencyMs = latency,
                finishReason = if (status == MessageStatus.DONE) "stop" else "error",
                error = error,
            )
        }
    }

    private fun now(): Instant = Clock.System.now()

    @OptIn(ExperimentalUuidApi::class)
    private fun userMessage(
        chatId: Uuid,
        parentId: Uuid?,
        text: String
    ) = Message(
        id = Uuid.random(),
        chatId = chatId,
        parentId = parentId,
        role = Role.USER,
        modelName = null,
        content = text,
        status = MessageStatus.DONE,
        createdAt = now(),
    )

    @OptIn(ExperimentalUuidApi::class)
    private fun assistantDraft(
        chatId: Uuid,
        parentId: Uuid,
        modelName: String
    ) = Message(
        id = Uuid.random(),
        chatId = chatId,
        parentId = parentId,
        role = Role.ASSISTANT,
        modelName = modelName,
        content = "",
        status = MessageStatus.GENERATING,
        createdAt = now(),
    )
}