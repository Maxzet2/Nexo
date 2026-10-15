package ru.maxzet2.plugins

import io.grpc.StatusRuntimeException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.plugins.statuspages.StatusPages
import ru.maxzet2.dto.ApiError

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, error ->
            call.application.environment.log.info(
                "[ERROR HANDLER] status=400 code=invalid_request message={}",
                error.message,
            )
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid_request", error.message ?: "Invalid request"),
            )
        }

        exception<StatusRuntimeException> { call, cause ->
            call.application.log.error("gRPC error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "grpc_status" to cause.status.code.name,
                    "grpc_desc" to (cause.status.description ?: "")
                )
            )
        }

        exception<NullPointerException> { call, error ->
            call.application.environment.log.info(
                "[ERROR HANDLER] status=400 code=invalid_request message={}",
                error.message,
            )
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("invalid_request", error.message ?: "Invalid request"),
            )
        }

        exception<Throwable> { call, error ->
            call.application.environment.log.error("[ERROR HANDLER] unhandled error", error)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal_error", "Internal server error"),
            )
        }
    }
}