package apicardo.finalproject

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// will convert into view models in future version
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: ArtistUser) {
    val repository = FindieRepository()

    // live data states
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var liveUser by remember { mutableStateOf(user) }

    // state for dialogs
    var showGenreDialog by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }

    // choices for genre drop down
    val availableGenres = listOf("Hip Hop", "Indie", "Electronic", "Rock", "R&B", "Pop", "Jazz", "Lo-fi", "Rap")
    var selectedGenres by remember {
        mutableStateOf(liveUser.genre.split(", ").filter { it.isNotBlank() })
    }

    // draft text for bio editor
    var draftBio by remember { mutableStateOf(liveUser.bio) }

    // real time listeners
    LaunchedEffect(user.spotifyId) {

        // listens for User profile changes (Bio, Genre, etc)
        repository.getUserData(user.spotifyId) { updatedUser ->
            if (updatedUser != null) {
                liveUser = updatedUser
                // sync the selected genres when the database updates
                selectedGenres = updatedUser.genre.split(", ").filter { it.isNotBlank() }
            }
        }
        // listens for Post changes
        repository.getUserPosts(user.spotifyId) { posts ->
            userPosts = posts
        }
    }

    // edit bio dialog
    if (showBioDialog) {
        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text("Edit Bio") },
            text = {
                OutlinedTextField(
                    value = draftBio,
                    onValueChange = { if (it.length <= 150) draftBio = it },
                    label = { Text("About you (max 150 chars)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.updateUserField(liveUser.spotifyId, "bio", draftBio)
                    showBioDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBioDialog = false }) { Text("Cancel") }
            }
        )
    }

    // select genre dialog
    if (showGenreDialog) {
        AlertDialog(
            onDismissRequest = { showGenreDialog = false },
            title = { Text("Select Genres (Max 3)") },
            text = {
                Column {
                    availableGenres.forEach { genre ->
                        val isSelected = selectedGenres.contains(genre)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedGenres = selectedGenres - genre
                                } else if (selectedGenres.size < 3) {
                                    selectedGenres = selectedGenres + genre
                                }
                            },
                            label = { Text(genre) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val genreString = selectedGenres.joinToString(", ")
                    repository.updateUserField(liveUser.spotifyId, "genre", genreString)
                    showGenreDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showGenreDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // header for profile
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(liveUser.displayName.take(1), style = MaterialTheme.typography.headlineLarge)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.clickable { showGenreDialog = true }) {
                Text(text = liveUser.displayName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = if (liveUser.genre.isEmpty()) "Select Genres +" else liveUser.genre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // editable bio section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    draftBio = liveUser.bio // reset draft to actual bio before opening
                    showBioDialog = true
                }
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = if (liveUser.bio.isEmpty()) "Add a bio to your profile..." else liveUser.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = if (liveUser.bio.isEmpty()) Color.Gray else Color.Unspecified
            )
            Text(
                text = "Edit Bio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // app stats Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${userPosts.size}", style = MaterialTheme.typography.titleLarge)
                Text("Posts", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("0", style = MaterialTheme.typography.titleLarge)
                Text("Followers", style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("0", style = MaterialTheme.typography.titleLarge)
                Text("Following", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()

        // grid layout for posts
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(userPosts) { post: Post ->
                Surface(
                    modifier = Modifier.aspectRatio(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (!post.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎵", style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }
            }
        }
    }
}