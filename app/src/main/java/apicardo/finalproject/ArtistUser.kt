package apicardo.finalproject

data class ArtistUser(
    val spotifyId: String = "",
    val displayName: String = "",
    val isArtist: Boolean = false,
    val spotifyArtistId: String = "",
    val profilePicUrl: String? = null,
    val genre: String = "",
    val bio: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
)
