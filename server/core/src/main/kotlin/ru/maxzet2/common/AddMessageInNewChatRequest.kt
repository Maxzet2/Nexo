import kotlinx.serialization.Serializable

@Serializable
data class AddMessageInNewChatRequest(
    val userId: String,
    val model: String,
    val message: String,
    val systemPrompt: String = "",
)