package ru.maxzet2.routing

import io.ktor.server.application.Application
import ru.maxzet2.routing.routings.configureChatRouting
import ru.maxzet2.routing.routings.configureModelRouting
import ru.maxzet2.routing.routings.configureSystemRouting

fun Application.configureRouting() {
    configureModelRouting()
    configureChatRouting()
    configureSystemRouting()
}