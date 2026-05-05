package apicardo.project

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList(),
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
