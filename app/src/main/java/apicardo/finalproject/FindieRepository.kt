package apicardo.finalproject


import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.google.firebase.firestore.FieldValue


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


    // messaging, storage, and file upload


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
            "timestamp" to Timestamp.now(),
            "participants" to listOf(senderId, receiverId)

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


    // personal posts for profile screen
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

    // fetch user data once
    fun getUserDataOnce(userId: String, onResult: (ArtistUser?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.toObject(ArtistUser::class.java))
            }
    }


    // function for likes on post
    fun toggleLike(postId: String, userId: String, isLiked: Boolean) {
        val postRef = db.collection("posts").document(postId)

        if (isLiked) {
            // remove user from list
            postRef.update("likedBy", FieldValue.arrayRemove(userId))

        } else {
            // add user to list
            postRef.update("likedBy", FieldValue.arrayUnion(userId))
        }

    }

    fun addComment(postId: String, comment: Comment, onResult: (Boolean) -> Unit) {
        db.collection("posts").document(postId)
            .update("comments", FieldValue.arrayUnion(comment))
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }

    // messaging
    fun getActiveConversations(currentUserId: String, onUpdate: (List<String>) -> Unit) {
        db.collection("messages")
            .whereArrayContains("participants", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING) // search for messages involving user
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                // get all unique IDs that are not the user's
                val partnerIds = snapshot.documents.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<String>
                    participants?.firstOrNull { it != currentUserId }
                }.distinct()

                onUpdate(partnerIds)
            }
    }




    // function for follow logic
    fun toggleFollow(currentUserId: String, targetUserId: String, isFollowing: Boolean) {
        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)

        if (isFollowing) {
            // unfollow: Remove IDs from both lists
            currentUserRef.update("following", FieldValue.arrayRemove(targetUserId))
            targetUserRef.update("followers", FieldValue.arrayRemove(currentUserId))
        } else {
            // follow: Add IDs to both lists
            currentUserRef.update("following", FieldValue.arrayUnion(targetUserId))
            targetUserRef.update("followers", FieldValue.arrayUnion(currentUserId))
        }
    }


}
