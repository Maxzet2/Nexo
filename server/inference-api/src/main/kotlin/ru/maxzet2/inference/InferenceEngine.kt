package ru.maxzet2.inference

import ru.maxzet2.common.LogitsOutput
import ru.maxzet2.common.ModelInput
import ru.maxzet2.common.ModelOutput
import ru.maxzet2.dto.EngineModel

interface InferenceEngine : AutoCloseable { // лучший кандидат на микросервисы
    suspend fun isReady(): Boolean
    suspend fun infer(
        modelName: String,
        input: ModelInput
    ): ModelOutput

    suspend fun listModels(): List<EngineModel>

    suspend fun inferLogits(modelName: String, input: ModelInput): LogitsOutput
}
