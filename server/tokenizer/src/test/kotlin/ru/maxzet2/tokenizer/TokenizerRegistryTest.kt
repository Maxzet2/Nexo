package ru.maxzet2.tokenizer

import java.nio.file.Files
import ru.maxzet2.error.InvalidRequestException
import ru.maxzet2.error.ModelNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TokenizerRegistryTest {
    @Test
    fun `paths outside model root are rejected`() {
        val root = Files.createTempDirectory("tokenizer-test")
        try {
            val registry = TokenizerRegistry(root)
            assertFailsWith<InvalidRequestException> { registry.forModel("../outside") }
            assertFailsWith<InvalidRequestException> { registry.forModel(root.resolveSibling("outside").toString()) }
            assertFailsWith<InvalidRequestException> { registry.forModel("") }
        } finally {
            Files.delete(root)
        }
    }

    @Test
    fun `missing tokenizer produces model not found`() {
        val root = Files.createTempDirectory("tokenizer-test")
        try {
            assertFailsWith<ModelNotFoundException> { TokenizerRegistry(root).forModel("missing") }
        } finally {
            Files.delete(root)
        }
    }
}
