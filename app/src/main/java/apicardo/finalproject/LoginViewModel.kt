package apicardo.finalproject

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repository = FindieRepository()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    // helper function to get the current Firebase UID
    val currentUserId: String? get() = auth.currentUser?.uid

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _isLoggedIn.value = true
                onResult(null)
            }
            .addOnFailureListener { onResult(it.message) }
    }

    fun signup(email: String, password: String, onResult: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val firebaseUid = result.user?.uid ?: ""

                // create the Findie Profile in Firestore immediately upon signup
                val newUser = ArtistUser(
                    spotifyId = firebaseUid,
                    displayName = email.split("@")[0], // get name from email
                    isArtist = true,
                    bio = ""
                )
                repository.saveUser(newUser) { success ->
                    if (success) {
                        _isLoggedIn.value = true
                        onResult(null)
                    } else {
                        onResult("Auth succeeded, but profile creation failed.")
                    }
                }
            }
            .addOnFailureListener { onResult(it.message) }
    }
    fun logout() {
        auth.signOut()
        _isLoggedIn.value = false
    }
}