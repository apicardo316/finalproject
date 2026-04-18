package apicardo.finalproject

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class FindieRepository {
    private val db: FirebaseFirestore = Firebase.firestore
    private val storage = Firebase.storage.reference

    // save user profile
    fun saveUser(user: ArtistUser, onResult: (Boolean) -> Unit) {
        db.collection("users").document(user.spotifyId)
            .set(user)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    // social feed

    // upload new post
    fun uploadPost(post: Post, onResult: (Boolean) -> Unit) {
        val newPostRef = db.collection("posts").document()
        val finalPost = post.copy(postId = newPostRef.id)

        newPostRef.set(finalPost)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    // fetch all posts for home screen
    fun getPosts(onUpdate: (List<Post>) -> Unit) {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING) // newest posts first
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                onUpdate(posts)
            }
    }

    // messaging and storage

    fun uploadFile(uri: Uri, artistId: String, fileName: String, folder: String, onComplete: (String?) -> Unit) {
        val ref = storage.child("$folder/$artistId/$fileName")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                onComplete(downloadUrl.toString())
            }
        }.addOnFailureListener {
            onComplete(null)
        }
    }

    fun sendMessage(senderId: String, receiverId: String, text: String) {
        val message = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "text" to text,
            "timestamp" to Timestamp.now()
        )
        db.collection("messages").add(message)
    }

    fun listenForMessages(currentUserId: String, otherUserId: String, onUpdate: (List<Map<String, Any>>) -> Unit) {
        db.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                val filtered = messages.filter {
                    (it["senderId"] == currentUserId && it["receiverId"] == otherUserId) ||
                            (it["senderId"] == otherUserId && it["receiverId"] == currentUserId)
                }
                onUpdate(filtered)
            }
    }

    // (personal posts)
    fun getUserPosts(userId: String, onUpdate: (List<Post>) -> Unit) {
        db.collection("posts")
            .whereEqualTo("authorId", userId) // filter posts into grid style
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) } ?: emptyList()
                onUpdate(posts)
            }
    }

    fun updateUserField(userId: String, field: String, value: String) {
        db.collection("users").document(userId)
            .update(field, value)
            .addOnSuccessListener { println("Successfully updated $field") }
            .addOnFailureListener { e -> println("Error updating: ${e.message}") }
    }

    fun getUserData(userId: String, onUpdate: (ArtistUser?) -> Unit) {
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val user = snapshot?.toObject(ArtistUser::class.java)
                onUpdate(user)
            }
    }

}