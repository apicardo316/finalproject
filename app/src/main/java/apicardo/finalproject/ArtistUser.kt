package apicardo.finalproject

data class ArtistUser(
    val spotifyId: String = "",
    val displayName: String = "",
    val isArtist: Boolean = false,
    val spotifyArtistId: String? = null,
    val profilePicUrl: String? = null,
    val genre: String = "",
    val bio: String = ""
)