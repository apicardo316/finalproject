package apicardo.finalproject

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun UploadScreen() {
    val repository = FindieRepository()
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Share Your Music", style = MaterialTheme.typography.headlineMedium)

        // image selection
        Button(onClick = { imageLauncher.launch("image/*") }) {
            Text(if (selectedImageUri == null) "Select Cover Art" else "Image Selected ✅")
        }

        // audio selection
        Button(onClick = { audioLauncher.launch("audio/*") }) {
            Text(if (selectedAudioUri == null) "Select Audio/Demo" else "Audio Selected 🎵")
        }

        OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Caption") })

        if (isUploading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    isUploading = true
                    // nested upload
                    uploadMediaAndPost(repository, selectedImageUri, selectedAudioUri, caption) {
                        isUploading = false
                        // reset UI when done
                    }
                },
                enabled = selectedAudioUri != null // require user to upload audio file
            ) {
                Text("Post")
            }
        }
    }
}


private fun uploadMediaAndPost(
    repo: FindieRepository,
    imageUri: Uri?,
    audioUri: Uri?,
    caption: String,
    onComplete: () -> Unit
) {
    // if image file exist upload first
    repo.uploadFile(imageUri!!, "user_arvin_123", "art_${System.currentTimeMillis()}.jpg", "artwork") { imgUrl ->

        // upload audio
        repo.uploadFile(audioUri!!, "user_arvin_123", "track_${System.currentTimeMillis()}.mp3", "tracks") { audioUrl ->

            // 3. Create the Post with both links
            val newPost = Post(
                authorId = "user_arvin_123",
                authorName = "Arvin Picardo",
                content = caption,
                imageUrl = imgUrl,
                audioUrl = audioUrl,
                timestamp = System.currentTimeMillis()
            )

            repo.uploadPost(newPost) { success ->
                if (success) println("Music Post Live!")
                onComplete()
            }
        }
    }
}

