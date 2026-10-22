import kotlinx.serialization.Serializable

@Serializable
data class AddMessageRequest(
    val chatId: String,
    val parentId: String,
    val userId: String,
    val model: String,
    val message: String,
)