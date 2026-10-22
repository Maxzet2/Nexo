package ru.maxzet2.routing.routings

import AddMessageInNewChatRequest
import AddMessageRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import ru.maxzet2.common.MessageResponse
import ru.maxzet2.common.EditMessageRequest
import org.koin.ktor.ext.inject
import ru.maxzet2.error.InvalidRequestException
import ru.maxzet2.service.ChatService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Application.configureChatRouting() {
    val service: ChatService by inject()

    routing {
        route("/chat") {
            get("/{chatId}/messages") {
                val chatId = call.parameters["chatId"]?.let { Uuid.parseOrNull(it) }
                    ?: throw InvalidRequestException("chatId must be a UUID")
                call.respond(HttpStatusCode.OK, service.listMessages(chatId).map { MessageResponse(it) })
            }
            patch("/messages/{nodeId}") {
                val nodeId = call.parameters["nodeId"]?.let { Uuid.parseOrNull(it) }
                    ?: throw InvalidRequestException("nodeId must be a UUID")
                val req = call.receive<EditMessageRequest>()
                call.respond(HttpStatusCode.OK, service.edit(nodeId, req.message, req.model))
            }
            post("/chat/message") {
                val req = call.receive<AddMessageRequest>()
                if (req.model.isBlank() || req.message.isBlank() || req.userId.isBlank()) {
                    throw InvalidRequestException("userid, model and message are required")
                }
                val result = service.addMessage(
                    userId = Uuid.parseOrNull(req.userId) ?: throw InvalidRequestException("userId must be a UUID"),
                    chatId = Uuid.parseOrNull(req.chatId) ?: throw InvalidRequestException("chatId must be a UUID"),
                    parentId = Uuid.parseOrNull(req.parentId) ?: throw InvalidRequestException("parentId must be a UUID"),
                    text = req.message,
                    modelName = req.model,
                )
                call.respond(HttpStatusCode.OK, result)
            }
            post("/chat/message-in-new-chat") {
                val req = call.receive<AddMessageInNewChatRequest>()
                if (req.model.isBlank() || req.message.isBlank() || req.userId.isBlank()) {
                    throw InvalidRequestException("userid, model and message are required")
                }
                val result = service.addMessage(
                    userId = Uuid.parseOrNull(req.userId) ?: throw InvalidRequestException("userId must be a UUID"),
                    text = req.message,
                    modelName = req.model,
                    systemPrompt = req.systemPrompt,
                )
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}