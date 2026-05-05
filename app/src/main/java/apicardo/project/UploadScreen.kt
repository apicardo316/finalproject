package apicardo.project

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun UploadScreen(viewModel: MainViewModel) {
    val repository = FindieRepository()

    // observe current user
    val currentUser by viewModel.currentUser.observeAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    // picker for image file
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    // picker for audio file
    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedAudioUri = uri }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Share Your Music",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // button for selecting image
        Button(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedImageUri == null) "Select Cover Art" else "Image Selected ✅")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // button for selecting audio
        Button(
            onClick = { audioLauncher.launch("audio/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedAudioUri == null) "Select Audio/Demo" else "Audio Selected 🎵")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // caption input
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Write a caption...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isUploading) {
            CircularProgressIndicator()
        } else {
            // post button
            Button(
                onClick = {
                    currentUser?.let { safeUser ->
                        if (selectedAudioUri != null) {
                            isUploading = true
                            uploadMediaAndPost(
                                repository,
                                safeUser,
                                selectedImageUri,
                                selectedAudioUri,
                                caption
                            ) {
                                isUploading = false
                                caption = ""
                                selectedImageUri = null
                                selectedAudioUri = null
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = selectedAudioUri != null && currentUser != null,
            ) {
                Text("Post to Feed")
            }
        }
    }
}

// logic functions

private fun uploadMediaAndPost(
    repo: FindieRepository,
    user: ArtistUser,
    imageUri: Uri?,
    audioUri: Uri?,
    caption: String,
    onComplete: () -> Unit
) {
    val userId = user.spotifyId

    if (imageUri != null) {
        repo.uploadFile(imageUri, userId, "art_${System.currentTimeMillis()}.jpg", "artwork") { imgUrl ->
            uploadAudioAndFinalize(repo, user, audioUri!!, caption, imgUrl, onComplete)
        }
    } else {
        uploadAudioAndFinalize(repo, user, audioUri!!, caption, null, onComplete)
    }
}

private fun uploadAudioAndFinalize(
    repo: FindieRepository,
    user: ArtistUser,
    audioUri: Uri,
    caption: String,
    imgUrl: String?,
    onComplete: () -> Unit
) {
    repo.uploadFile(audioUri, user.spotifyId, "track_${System.currentTimeMillis()}.mp3", "tracks") { audioUrl ->
        val newPost = Post(
            authorId = user.spotifyId,
            authorName = user.displayName,
            content = caption,
            imageUrl = imgUrl,
            audioUrl = audioUrl,
            timestamp = System.currentTimeMillis()
        )

        repo.uploadPost(newPost) { success ->
            if (success) println("Music Post Live for ${user.displayName}!")
            onComplete()
        }
    }
}