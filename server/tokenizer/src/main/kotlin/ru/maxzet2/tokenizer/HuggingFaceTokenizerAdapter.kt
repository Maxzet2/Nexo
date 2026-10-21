package ru.maxzet2.tokenizer

import Tokenizer
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ru.maxzet2.common.TokenizedInput
import java.nio.file.Path

class HuggingFaceTokenizerAdapter(
    tokenizerFile: Path,
) : Tokenizer {

    private val tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile)

    override fun encode(text: String): TokenizedInput {
        val e = tokenizer.encode(text)
        return TokenizedInput(
            inputIds = e.ids,
            attentionMask = e.attentionMask,
            tokenTypeIds = e.typeIds,
        )
    }

    override fun decode(ids: LongArray): String =
        tokenizer.decode(ids, true)

    override fun eosTokenId(): Int? = null
    override fun bosTokenId(): Int? = null
    override fun padTokenId(): Int? = null
}