package ru.maxzet2.inference

import ru.maxzet2.common.LogitsOutput
import ru.maxzet2.common.ModelInput
import ru.maxzet2.common.ModelOutput
import ru.maxzet2.dto.EngineModel

interface InferenceEngine : AutoCloseable { // лучший кандидат на микросервисы
    fun isReady(): Boolean
    fun infer(
        modelName: String,
        input: ModelInput
    ): ModelOutput

    fun listModels(): List<EngineModel>

    fun inferLogits(modelName: String, input: ModelInput): LogitsOutput
}
