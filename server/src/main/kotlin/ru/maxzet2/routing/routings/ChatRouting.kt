package ru.maxzet2.routing.routings

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import ru.maxzet2.service.ChatService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Application.configureChatRouting() {
    val service: ChatService by inject()
//    val adminId = Uuid.parse(System.getenv("ADMIN_ID")) типа админка

    routing {
        route("/chat") {
            post("/chat/messages") {
                val req = call.receive<AddMessageRequest>()
                if (req.model.isBlank() || req.message.isBlank() || req.userId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "userid, model and message are required")
                    return@post
                }
                val result = service.addMessage(
                    userId = Uuid.parse(req.userId),
                    chatId = Uuid.parse(req.chatId.toString()),
                    text = req.message,
                    modelName = req.model,
                    systemPrompt = req.systemPrompt,
                )
                call.respond(HttpStatusCode.OK, result)
            }
        }
    }
}

@Serializable
data class AddMessageRequest(
    val chatId: String? = null,
    val userId: String,
    val model: String,
    val message: String,
    val systemPrompt: String = "",
)