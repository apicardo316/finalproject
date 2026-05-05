package apicardo.project

import retrofit2.http.*
import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

interface SpotifyApiService {
    // get access token
    @FormUrlEncoded
    @POST("https://accounts.spotify.com/api/token")
    suspend fun getAccessToken(
        @Header("Authorization") auth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): TokenResponse

    // search for an artist
    @GET("v1/search")
    suspend fun searchArtist(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("type") type: String = "artist",
        @Query("limit") limit: Int = 10
    ): SpotifySearchResponse

    @GET("https://api.spotify.com/v1/artists/{id}")
    suspend fun getArtistDetails(
        @Header("Authorization") token: String,
        @Path("id") artistId: String
    ): SpotifyArtistResponse
}

// data classes for responses
data class TokenResponse(val access_token: String)

data class SpotifySearchResponse(val artists: ArtistList)
data class ArtistList(val items: List<SpotifyArtistResponse>)

// @SerializedName links the JSON object to my class --> due to problems with the API
// Keep is used to try and maintain data integrity
@Keep
data class SpotifyArtistResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("images") val images: List<SpotifyImage>?,


    @SerializedName("followers")
    val followers: FollowerCount?,

    //
    @SerializedName("popularity")
    val popularity: Int = 0
)

@Keep
data class FollowerCount(
    @SerializedName("total") val total: Int
)

data class SpotifyImage(
    @SerializedName("url") val url: String
)