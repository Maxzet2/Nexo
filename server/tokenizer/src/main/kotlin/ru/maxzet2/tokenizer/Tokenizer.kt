import ru.maxzet2.common.TokenizedInput

interface Tokenizer {
    fun encode(text: String): TokenizedInput
    fun decode(ids: LongArray): String
    fun eosTokenId(): Int?
    fun bosTokenId(): Int?
    fun padTokenId(): Int?
}