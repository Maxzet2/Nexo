package ru.maxzet2.service

import ru.maxzet2.chat.Message
import ru.maxzet2.common.ModelInput
import ru.maxzet2.inference.InferenceEngine
import ru.maxzet2.tokenizer.TokenizerRegistry

class ModelService(
    private val engine: InferenceEngine,
    private val tokenizers: TokenizerRegistry,
) {

    private val maxPromptContextTokens =
        System.getenv("PROMPT_CONTEXT_TOKENS")?.toIntOrNull() ?: 4096

    private val eosByModel = mapOf("gpt2" to 50256L)

    suspend fun listAvailable() = engine.listModels()

    suspend fun generate(
        modelName: String,
        systemPrompt: String,
        history: List<Message>,
        maxTokens: Int,
    ): String {
        val tokenizer = tokenizers.forModel(modelName)
        val eos = eosByModel[modelName] ?: tokenizer.eosTokenId()?.toLong()

        val promptIds = tokenizer.encode(buildPrompt(systemPrompt, history)).inputIds
            .takeLast(maxPromptContextTokens)
            .toLongArray()
        val ids = promptIds.toMutableList()

        for (step in 0 until maxTokens) {
            val out = engine.inferLogits(
                modelName,
                ModelInput(
                    inputIds = ids.toLongArray(),
                    attentionMask = LongArray(ids.size) { 1L },
                ),
            )
            val from = (ids.size - 1) * out.vocabSize
            val next = argmax(out.values, from, out.vocabSize)

            if (next == eos) break
            ids += next
        }

        return tokenizer.decode(ids.drop(promptIds.size).toLongArray())
    }


    private fun argmax(a: FloatArray, from: Int, len: Int): Long {
        var best = from
        for (i in from until from + len) if (a[i] > a[best]) best = i
        return (best - from).toLong()
    }

    private fun buildPrompt(systemPrompt: String, history: List<Message>): String = buildString {
        if (systemPrompt.isNotBlank()) {
            appendLine(systemPrompt.trim())
            appendLine()
        }
        for (m in history) {
            appendLine(m.content)
        }
    }
}
