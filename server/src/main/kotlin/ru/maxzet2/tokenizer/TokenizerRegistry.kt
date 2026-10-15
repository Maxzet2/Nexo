package ru.maxzet2.tokenizer

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
            require(dir.startsWith(root)) { "Некорректное имя модели: $modelName" }

            val file = dir.resolve("tokenizer.json")
            require(Files.exists(file)) {
                "Не найден токенайзер для модели '$modelName': $file"
            }

            HuggingFaceTokenizerAdapter(file)
        }
}