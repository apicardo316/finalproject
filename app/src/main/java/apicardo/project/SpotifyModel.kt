package apicardo.project

// model for UI
data class SpotifyArtist(
    val id: String,
    val name: String,
    val genres: List<String>?,
    val imageUrl: String? = null,
    val followers: Int = 0,
    val popularity: Int
)