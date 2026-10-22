package ru.maxzet2.inference.triton

import ru.maxzet2.common.LogitsOutput
import ru.maxzet2.common.ModelInput
import ru.maxzet2.common.ModelOutput
import ru.maxzet2.dto.EngineModel
import ru.maxzet2.inference.InferenceEngine

class TritonEngine (
    private val grpc: GrpcTritonClient,
    private val http: HttpTritonClient,
) : InferenceEngine {

    override suspend fun isReady(): Boolean = grpc.isReady()

    override suspend fun infer(
        modelName: String,
        input: ModelInput
    ): ModelOutput =
        tritonRequest(modelName) { grpc.infer(modelName, input) }

    override suspend fun listModels(): List<EngineModel> =
        tritonRequest { http.listModels() }.map { entry ->
            EngineModel(
                id = entry.name,
                state = entry.state ?: "UNKNOWN",
            )
        }

    override suspend fun inferLogits(modelName: String, input: ModelInput): LogitsOutput {
        return tritonRequest(modelName) { grpc.inferLogits(modelName, input) }
    }

    override fun close() {
        grpc.close()
        http.close()
    }
}