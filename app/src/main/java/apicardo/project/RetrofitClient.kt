package apicardo.project

// In RetrofitClient.kt
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://api.spotify.com/"

    // GSON instance (lenient due to problems with API)
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val instance: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SpotifyApiService::class.java)
    }
}