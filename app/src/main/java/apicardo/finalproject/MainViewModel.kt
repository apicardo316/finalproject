package apicardo.finalproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repo = FindieRepository()

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> get() = _posts
    private val _uiState = MutableLiveData<UIState>(UIState.Loading)
    val uiState: LiveData<UIState> get() = _uiState

    // keeps track of the user across different screens
    private val _currentUser = MutableLiveData<ArtistUser?>()
    val currentUser: LiveData<ArtistUser?> get() = _currentUser

    fun verifyUser(token: String) {
        _uiState.value = UIState.Loading

        viewModelScope.launch {
            // simulate a network delay for the Spotify check (temporary)
            delay(1000)

            // temporary user details
            val user = ArtistUser(
                spotifyId = "user_arvin_123",
                displayName = "Arvin Picardo",
                isArtist = true,
                spotifyArtistId = "43ZHm0rCu0Sls9vTndvUzb",
                bio = "Indie Producer | California"
            )

            // save this user to Firebase immediately on login
            saveOrUpdateUser(user)
        }
    }

    fun saveOrUpdateUser(user: ArtistUser) {
        repo.saveUser(user) { success ->
            if (success) {
                _currentUser.postValue(user)
                _uiState.postValue(UIState.Success(user))
            } else {
                _uiState.postValue(UIState.Error("Failed to sync with Firebase"))
            }
        }
    }

    fun fetchPosts() {
        repo.getPosts { postList ->
            _posts.postValue(postList)
        }
    }
}

sealed class UIState {
    object Loading : UIState()
    data class Success(val user: ArtistUser) : UIState()
    data class Error(val message: String) : UIState()
}