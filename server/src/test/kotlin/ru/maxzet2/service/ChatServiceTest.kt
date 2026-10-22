@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package ru.maxzet2.service

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import ru.maxzet2.chat.Chat
import ru.maxzet2.chat.Message
import ru.maxzet2.chat.MessageStatus
import ru.maxzet2.common.*
import ru.maxzet2.db.*
import ru.maxzet2.error.*
import ru.maxzet2.inference.InferenceEngine
import ru.maxzet2.tokenizer.TokenizerRegistry
import java.nio.file.Files
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.Uuid

class ChatServiceTest {
    private val database = Database.connect("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    private val chats = ExposedChatRepository()
    private val messages = ExposedMessageRepository()
    private val tokenizerRoot = Files.createTempDirectory("chat-test-tokenizers")
    private val engine = object : InferenceEngine {
        override suspend fun isReady() = true
        override suspend fun listModels() = emptyList<ru.maxzet2.dto.EngineModel>()
        override suspend fun infer(modelName: String, input: ModelInput): ModelOutput = error("Unexpected inference")
        override suspend fun inferLogits(modelName: String, input: ModelInput): LogitsOutput = error("Unexpected inference")
        override fun close() = Unit
    }
    private val service = ChatService(chats, messages, ModelService(engine, TokenizerRegistry(tokenizerRoot)))
    private val owner = Uuid.random()

    @BeforeTest
    fun setup() = transaction(database) { SchemaUtils.create(ChatTable, MessageTable) }

    @AfterTest
    fun cleanup() {
        transaction(database) { SchemaUtils.drop(MessageTable, ChatTable) }
        Files.delete(tokenizerRoot)
    }

    @Test
    fun `send result preserves generated text including an empty answer`() {
        for (text in listOf("Ответ\nс продолжением", "")) {
            val result = SendResult(Uuid.random(), Uuid.random(), Uuid.random(), text)
            assertEquals(result, Json.decodeFromString<SendResult>(Json.encodeToString(result)))
        }
    }

    @Test
    fun `busy chat rejects both continuation and editing without adding nodes`() = runBlocking {
        val chat = createChat()
        val user = insertMessage(chat, null, Role.USER)
        val answer = insertMessage(chat, user.id, Role.ASSISTANT, MessageStatus.GENERATING)
        assertFailsWith<ChatBusyException> { continueChat(chat, answer.id) }
        assertFailsWith<ChatBusyException> { service.edit(user.id, "changed", "missing") }
        assertEquals(2, service.listMessages(chat.id).size)
    }

    @Test
    fun `parent from another chat and non assistant parents are rejected`() = runBlocking {
        val chat = createChat()
        val user = insertMessage(chat, null, Role.USER)
        val otherChat = createChat()
        val otherAnswer = insertMessage(otherChat, null, Role.ASSISTANT)
        assertFailsWith<InvalidRequestException> { continueChat(chat, otherAnswer.id) }
        assertFailsWith<InvalidRequestException> { continueChat(chat, user.id) }
        val failed = insertMessage(chat, user.id, Role.ASSISTANT, MessageStatus.FAILED)
        assertFailsWith<InvalidRequestException> { continueChat(chat, failed.id) }
        assertEquals(2, service.listMessages(chat.id).size)
    }

    @Test
    fun `editing creates a sibling and preserves the original subtree`() = runBlocking {
        val chat = createChat()
        val user = insertMessage(chat, null, Role.USER)
        val answer = insertMessage(chat, user.id, Role.ASSISTANT)
        // No real model is needed: the prepared branch must survive an inference failure.
        assertFailsWith<ModelNotFoundException> { service.edit(user.id, "changed", "missing") }
        val tree = service.listMessages(chat.id)
        assertEquals(user, tree.single { it.id == user.id })
        assertEquals(answer, tree.single { it.id == answer.id })
        val edited = tree.single { it.content == "changed" }
        assertEquals(user.parentId, edited.parentId)
        assertEquals(MessageStatus.FAILED, tree.single { it.parentId == edited.id }.status)
        assertEquals(4, tree.size)
    }

    @Test
    fun `client parent determines the branch and another busy chat does not block it`() = runBlocking {
        val chat = createChat()
        val user = insertMessage(chat, null, Role.USER)
        val first = insertMessage(chat, user.id, Role.ASSISTANT)
        val second = insertMessage(chat, user.id, Role.ASSISTANT)
        val busy = createChat()
        insertMessage(busy, null, Role.ASSISTANT, MessageStatus.GENERATING)
        for (parent in listOf(first, second)) {
            assertFailsWith<ModelNotFoundException> { continueChat(chat, parent.id) }
        }
        val tree = service.listMessages(chat.id)
        for (parent in listOf(first, second)) {
            assertEquals(1, tree.count { it.role == Role.USER && it.parentId == parent.id })
        }
        transaction(database) { assertNull(chats.getForUpdate(chat.id)!!.currentMessageId) }
        // Tree response retains parent links for rebuilding the client cache.
        val response = tree.map { MessageResponse(it) }
        assertEquals(response, Json.decodeFromString<List<MessageResponse>>(Json.encodeToString(response)))
    }

    private suspend fun continueChat(chat: Chat, parentId: Uuid) =
        service.addMessage(owner, chat.id, parentId, "continue", "missing")

    private fun createChat(): Chat = transaction(database) {
        val now = Clock.System.now()
        Chat(Uuid.random(), "test", owner, "", null, now, now).also { chats.create(it) }
    }

    private fun insertMessage(
        chat: Chat,
        parentId: Uuid?,
        role: Role,
        status: MessageStatus = MessageStatus.DONE,
    ): Message = transaction(database) {
        Message(Uuid.random(), chat.id, parentId, role, "missing", "original", status,
            createdAt = Clock.System.now()).also { messages.insert(it) }
    }
}
