package apicardo.finalproject

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SpotifyService {
    @GET("v1/me")
    suspend fun getProfile(
        @Header("Authorization") auth: String
    ): SpotifyProfile

    @GET("v1/search")
    suspend fun searchArtist(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("type") type: String = "artist"
    ): ArtistSearchResponse
}