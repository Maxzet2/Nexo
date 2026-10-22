package ru.maxzet2.routing.routings

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureSystemRouting() {
    routing {
        route("/system") {
            post("/send_attachment") {
                // логика загрузки вложений
                // также предполагается что клиенту вернется адрес вложения, а потом отправится запрос с адресом вложения
            }
        }
        get("/healthy") {
            call.respondText(
                "OK",
                ContentType.Text.Plain
            )
        }
    }
}