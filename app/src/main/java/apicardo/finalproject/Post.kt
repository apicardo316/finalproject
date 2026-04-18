package apicardo.finalproject

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)