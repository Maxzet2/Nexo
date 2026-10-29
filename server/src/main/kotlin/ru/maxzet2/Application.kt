package ru.maxzet2

import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import ru.maxzet2.routing.configureRouting
import ru.maxzet2.plugins.configureDependencyInjection
import ru.maxzet2.plugins.configureErrorHandling
import ru.maxzet2.plugins.configureSerialization

fun main() {
    embeddedServer(
        CIO,
        port = 8080,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureDependencyInjection()
    configureErrorHandling()
    configureSerialization()
    configureRouting()
}