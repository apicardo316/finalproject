package apicardo.project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repo = FindieRepository()

    private val spotifyApi = RetrofitClient.instance

    // spotify credentials from their developer's website
    private val clientId = "55f1df27135d483f9a2c9eeb25671135"
    private val clientSecret = "4dc5c2b1a3f74dfc950dd78741d7682e"
    private var spotifyAccessToken: String? = null

    // get spotify stats
    private val _selectedArtistStats = MutableLiveData<SpotifyArtistResponse?>()
    val selectedArtistStats: LiveData<SpotifyArtistResponse?> = _selectedArtistStats

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> get() = _posts

    private val _uiState = MutableLiveData<UIState>(UIState.Loading)
    val uiState: LiveData<UIState> get() = _uiState

    private val _currentUser = MutableLiveData<ArtistUser?>()
    val currentUser: LiveData<ArtistUser?> get() = _currentUser

    // spotify search functions
    fun searchSpotifyArtists(query: String, onResult: (List<SpotifyArtist>) -> Unit) {
        //println("DEBUG: Searching for $query")

        if (query.isBlank()) {
            onResult(emptyList())
            return
        }

        viewModelScope.launch {
            try {
                //println("DEBUG: Starting search for $query")
                if (spotifyAccessToken == null) {
                    //println("DEBUG: Requesting new token...")
                    val authString = "$clientId:$clientSecret"
                    val encodedAuth = android.util.Base64.encodeToString(
                        authString.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    val tokenResponse = spotifyApi.getAccessToken(auth = "Basic $encodedAuth")
                    spotifyAccessToken = tokenResponse.access_token
                    //println("DEBUG: Token received: ${tokenResponse.access_token.take(10)}...")
                }

                val searchResponse = spotifyApi.searchArtist(
                    token = "Bearer $spotifyAccessToken",
                    query = "artist:$query"
                )
                //val itemCount = searchResponse.artists.items.size
                //println("DEBUG: Spotify returned $itemCount artists")

                val results = searchResponse.artists.items.filter { it.type == "artist" }.map { item ->
                    SpotifyArtist(
                        id = item.id,
                        name = item.name,
                        genres = item.genres ?: emptyList(),
                        imageUrl = item.images?.firstOrNull()?.url,
                        followers = item.followers?.total ?: 0,
                        popularity = item.popularity
                    )
                }
                //println("DEBUG: Mapping complete. Sending ${results.size} items back to UI")
                onResult(results)
            } catch (e: Exception) {
                //println("DEBUG: SEARCH ERROR: ${e.message}")
                e.printStackTrace()
                onResult(emptyList())
            }
        }
    }


    fun fetchPosts() {
        repo.getPosts { postList -> _posts.postValue(postList) }
    }

    fun toggleLike(postId: String, userId: String, isAlreadyLiked: Boolean) {
        repo.toggleLike(postId, userId, isAlreadyLiked)
    }

    fun postComment(postId: String, text: String) {
        val user = _currentUser.value ?: return
        val newComment = Comment(userId = user.spotifyId, userName = user.displayName, text = text)
        repo.addComment(postId, newComment) { success ->
            if (success) println("Comment posted!")
        }
    }

    fun startUserSync(uid: String) {
        repo.getUserData(uid) { updatedUser -> _currentUser.value = updatedUser }
    }

    // helper function to handle token logic for all Spotify calls
    private suspend fun getValidToken(): String? {
        if (spotifyAccessToken == null) {
            //println("DEBUG: Requesting new token...")
            val authString = "$clientId:$clientSecret"
            val encodedAuth = android.util.Base64.encodeToString(
                authString.toByteArray(),
                android.util.Base64.NO_WRAP
            )
            val tokenResponse = spotifyApi.getAccessToken(auth = "Basic $encodedAuth")
            spotifyAccessToken = tokenResponse.access_token
        }
        return spotifyAccessToken
    }

    fun fetchArtistStats(artistId: String) {
        viewModelScope.launch {
            try {
                val token = getValidToken()
               // println("DEBUG: Fetching stats for ID: $artistId")

                if (token != null) {
                    val response = spotifyApi.getArtistDetails("Bearer $token", artistId)
                   // commented out debug statements
                    // println("DEBUG: Artist Object Type: ${response.type}")
                   // println("DEBUG: Followers Object is null: ${response.followers == null}")
                   // println("DEBUG: Popularity raw: ${response.popularity}")
                   // println("DEBUG: Artist Name: ${response.name}")
                   // println("DEBUG: Stats Received - Popularity: ${response.popularity}, Followers: ${response.followers?.total}")
                    _selectedArtistStats.postValue(response)
                   // println("DEBUG: Successfully posted stats for ${response.name}")
                }
            } catch (e: Exception) {
                //println("DEBUG: STATS ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

sealed class UIState {
    object Loading : UIState()
    data class Success(val user: ArtistUser) : UIState()
    data class Error(val message: String) : UIState()
}
