package ru.maxzet2.inference.triton

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import ru.maxzet2.dto.TritonModelEntry

class HttpTritonClient (
    private val baseUrl: String,
) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun listModels(): List<TritonModelEntry> {
        return client.post("$baseUrl/v2/repository/index") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.body()
    }

    fun close() {
        client.close()
    }
}