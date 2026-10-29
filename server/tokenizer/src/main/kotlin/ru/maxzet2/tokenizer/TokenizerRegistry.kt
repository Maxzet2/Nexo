package ru.maxzet2.tokenizer

import ru.maxzet2.error.InvalidRequestException
import ru.maxzet2.error.ModelNotFoundException
import Tokenizer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class TokenizerRegistry(
    rootDir: Path,
) {
    private val root = rootDir.toAbsolutePath().normalize()
    private val cache = ConcurrentHashMap<String, Tokenizer>()

    fun forModel(modelName: String): Tokenizer =
        cache.computeIfAbsent(modelName) {
            val dir = root.resolve(modelName).normalize()
            if (!dir.startsWith(root) || dir == root) {
                throw InvalidRequestException("Некорректное имя модели: $modelName")
            }

            val file = dir.resolve("tokenizer.json")
            if (!Files.isRegularFile(file)) throw ModelNotFoundException(modelName)

            HuggingFaceTokenizerAdapter(file)
        }
}