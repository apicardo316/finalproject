package apicardo.finalproject


data class SpotifyProfile(
    val id: String,
    val display_name: String
)

data class ArtistSearchResponse(
    val artists: ArtistItems
)

data class ArtistItems(
    val items: List<SpotifyArtist>
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    val followers: SpotifyFollowers,
    val popularity: Int,
    val genres: List<String>
)

data class SpotifyFollowers(
    val total: Int
)