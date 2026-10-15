package ru.maxzet2.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection() {
    DatabaseFactory.init()
    install(Koin) {
        modules(getModules())
    }
}