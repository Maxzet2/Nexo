package ru.maxzet2.plugins

import ru.maxzet2.error.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorHandlingTest {
    @Test
    fun `illegal argument returns 400 with its message`() = assertError(
        IllegalArgumentException("model is required"),
        HttpStatusCode.BadRequest,
        """{"code":"invalid_request","message":"model is required"}""",
    )

    @Test
    fun `illegal argument without message uses fallback`() = assertError(
        IllegalArgumentException(),
        HttpStatusCode.BadRequest,
        """{"code":"invalid_request","message":"Invalid request"}""",
    )

    @Test
    fun `null pointer is an internal error`() = assertError(
        NullPointerException("private value"), HttpStatusCode.InternalServerError,
        """{"code":"internal_error","message":"Internal server error"}""",
    )

    @Test
    fun `invalid request uses custom error`() = assertError(
        InvalidRequestException("message is required"), HttpStatusCode.BadRequest,
        """{"code":"invalid_request","message":"message is required"}""",
    )

    @Test
    fun `busy chat returns 409`() = assertError(
        ChatBusyException(), HttpStatusCode.Conflict,
        """{"code":"chat_busy","message":"В этом чате уже идёт генерация"}""",
    )

    @Test
    fun `missing chat returns 404`() = assertError(
        ChatNotFoundException("123"), HttpStatusCode.NotFound,
        """{"code":"chat_not_found","message":"Чат 123 не найден"}""",
    )

    @Test
    fun `missing message returns 404`() = assertError(
        MessageNotFoundException("123"), HttpStatusCode.NotFound,
        """{"code":"message_not_found","message":"Сообщение 123 не найдено"}""",
    )

    @Test
    fun `missing model returns 404`() = assertError(
        ModelNotFoundException("missing"), HttpStatusCode.NotFound,
        """{"code":"model_not_found","message":"Модель 'missing' не найдена"}""",
    )

    @Test
    fun `unavailable inference hides transport details`() = assertError(
        InferenceUnavailableException(Exception("private host")), HttpStatusCode.ServiceUnavailable,
        """{"code":"inference_unavailable","message":"Inference service is unavailable"}""",
    )

    @Test
    fun `inference timeout returns 504`() = assertError(
        InferenceTimeoutException(Exception("private details")), HttpStatusCode.GatewayTimeout,
        """{"code":"inference_timeout","message":"Inference service timed out"}""",
    )

    @Test
    fun `inference failure returns 502`() = assertError(
        InferenceFailedException(Exception("private details")), HttpStatusCode.BadGateway,
        """{"code":"inference_failed","message":"Inference service failed"}""",
    )

    @Test
    fun `bad request body hides parser details`() = assertError(
        BadRequestException("parser details"), HttpStatusCode.BadRequest,
        """{"code":"invalid_request","message":"Invalid request body"}""",
    )

    @Test
    fun `unexpected exception returns 500 without exposing its details`() = assertError(
        IllegalStateException("private database details"),
        HttpStatusCode.InternalServerError,
        """{"code":"internal_error","message":"Internal server error"}""",
    )

    @Test
    fun `fallback also handles throwable outside exception hierarchy`() = assertError(
        Throwable("private failure details"),
        HttpStatusCode.InternalServerError,
        """{"code":"internal_error","message":"Internal server error"}""",
    )

    @Test
    fun `successful response remains unchanged`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            routing {
                get("/success") { call.respondText("OK") }
            }
        }
        val response = client.get("/success")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    private fun assertError(error: Throwable, status: HttpStatusCode, expectedJson: String) = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            routing {
                get("/failure") { throw error }
            }
        }
        val response = client.get("/failure")
        assertEquals(status, response.status)
        assertEquals("application/json", response.headers[HttpHeaders.ContentType]?.substringBefore(';'))
        assertEquals(Json.parseToJsonElement(expectedJson), Json.parseToJsonElement(response.bodyAsText()))
    }
}
