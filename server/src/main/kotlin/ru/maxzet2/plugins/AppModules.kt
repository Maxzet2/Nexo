package ru.maxzet2.plugins

import ru.maxzet2.service.ModelService
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.maxzet2.db.ExposedChatRepository
import ru.maxzet2.db.ExposedMessageRepository
import ru.maxzet2.inference.InferenceEngine
import ru.maxzet2.inference.triton.GrpcTritonClient
import ru.maxzet2.inference.triton.HttpTritonClient
import ru.maxzet2.inference.triton.TritonEngine
import ru.maxzet2.repository.ChatRepository
import ru.maxzet2.repository.MessageRepository
import ru.maxzet2.service.ChatService
import ru.maxzet2.tokenizer.TokenizerRegistry
import java.lang.System
import java.nio.file.Path

fun getModules() : List<Module> {
    return listOf(
        module {
            single {
                TokenizerRegistry(
                    Path.of(System.getenv("MODEL_DOCKER_DIR") ?: "/models")
                )
            }

            single<InferenceEngine> {
                val host = System.getenv("INFERENCE_HOST") ?: "inference-server"
                val grpcPort = System.getenv("INFERENCE_GRPC_PORT")?.toIntOrNull() ?: 8001
                val httpPort = System.getenv("INFERENCE_HTTP_PORT")?.toIntOrNull() ?: 8000

                TritonEngine(
                    grpc = GrpcTritonClient(host, grpcPort),
                    http = HttpTritonClient("http://$host:$httpPort"),
                )
            }

            single<ChatRepository> { ExposedChatRepository() }
            single<MessageRepository> { ExposedMessageRepository() }

            single { ModelService(get(), get()) }
            single { ChatService(get(), get(), get()) }
        }
    )
}