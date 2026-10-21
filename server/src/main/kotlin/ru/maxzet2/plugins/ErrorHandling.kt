package ru.maxzet2.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import ru.maxzet2.dto.ApiError
import ru.maxzet2.error.*

fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ServiceException> { call, error ->
            val status = when (error) {
                is InvalidRequestException -> HttpStatusCode.BadRequest
                is ChatNotFoundException, is MessageNotFoundException, is ModelNotFoundException -> HttpStatusCode.NotFound
                is InferenceUnavailableException -> HttpStatusCode.ServiceUnavailable
                is InferenceTimeoutException -> HttpStatusCode.GatewayTimeout
                is InferenceFailedException -> HttpStatusCode.BadGateway
            }
            if (status.value >= 500) call.application.log.error(error.code, error)
            call.respond(status, ApiError(error.code, error.message!!))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ApiError("invalid_request", "Invalid request body"))
        }
        exception<IllegalArgumentException> { call, error ->
            call.respond(HttpStatusCode.BadRequest, ApiError("invalid_request", error.message ?: "Invalid request"))
        }
        exception<Throwable> { call, error ->
            if (error is CancellationException) throw error
            call.application.log.error("Unhandled error", error)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal_error", "Internal server error"))
        }
    }
}
