package ru.maxzet2.plugins

import io.grpc.Status
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
    fun `null pointer returns current 400 contract`() = assertError(
        NullPointerException("missing value"),
        HttpStatusCode.BadRequest,
        """{"code":"invalid_request","message":"missing value"}""",
    )

    @Test
    fun `null pointer without message uses fallback`() = assertError(
        NullPointerException(),
        HttpStatusCode.BadRequest,
        """{"code":"invalid_request","message":"Invalid request"}""",
    )

    @Test
    fun `grpc runtime exception returns status and description`() = assertError(
        Status.UNAVAILABLE.withDescription("Inference server is unavailable").asRuntimeException(),
        HttpStatusCode.InternalServerError,
        """{"grpc_status":"UNAVAILABLE","grpc_desc":"Inference server is unavailable"}""",
    )

    @Test
    fun `grpc runtime exception without description returns empty description`() = assertError(
        Status.INTERNAL.asRuntimeException(),
        HttpStatusCode.InternalServerError,
        """{"grpc_status":"INTERNAL","grpc_desc":""}""",
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
