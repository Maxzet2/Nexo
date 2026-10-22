package ru.maxzet2.routing.routings

import ru.maxzet2.service.ModelService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureModelRouting() {

    val service: ModelService by inject()

    routing {
        route("/model") {

            get("/get-available") {
                call.respond(
                    HttpStatusCode.OK,
                    service.listAvailable()
                )
            }
        }
    }
}