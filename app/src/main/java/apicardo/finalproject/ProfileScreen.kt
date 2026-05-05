package apicardo.finalproject

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.runtime.livedata.observeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(user: ArtistUser, loginViewModel: LoginViewModel, viewModel: MainViewModel) {
    val repository = FindieRepository()

    // states for data
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var liveUser by remember { mutableStateOf(user) }
    var showUserList by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var isUploadingProfilePic by remember { mutableStateOf(false) }

    // dialog controls
    var showGenreDialog by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }

    // states for spotify
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SpotifyArtist>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // profile pic upload logic using firebase
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { safeUri ->
            isUploadingProfilePic = true
            val fileName = "profile_${liveUser.spotifyId}.jpg"

            // use uploadFile logic
            repository.uploadFile(safeUri, liveUser.spotifyId, fileName, "profile_pics") { downloadUrl ->
                if (downloadUrl != null) {
                    // 1. update firebase with pic url
                    repository.updateUserField(liveUser.spotifyId, "profilePicUrl", downloadUrl)

                    // 2.update local state so that the UI refreshes immediately
                    liveUser = liveUser.copy(profilePicUrl = downloadUrl)
                }
                isUploadingProfilePic = false
            }
        }
    }

    // states for profile data editing
    val availableGenres = listOf("Hip Hop", "Indie", "Electronic", "Rock", "R&B", "Pop", "Jazz", "Lo-fi", "Rap")
    var selectedGenres by remember {
        mutableStateOf(liveUser.genre.split(", ").filter { it.isNotBlank() })
    }
    var draftBio by remember { mutableStateOf(liveUser.bio) }

    // sync the data
    LaunchedEffect(user.spotifyId) {
        repository.getUserData(user.spotifyId) { updatedUser ->
            if (updatedUser != null) {
                liveUser = updatedUser
                selectedGenres = updatedUser.genre.split(", ").filter { it.isNotBlank() }
            }
        }
        repository.getUserPosts(user.spotifyId) { posts ->
            userPosts = posts
        }
    }

    // spotify verification
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Verify Spotify Artist Profile") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Search for your official Spotify artist page to verify identity.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            isSearching = true
                            viewModel.searchSpotifyArtists(it) { results ->
                                searchResults = results
                                isSearching = false
                            }
                        },
                        label = { Text("Artist Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (isSearching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp).padding(top = 8.dp)) {
                        items(searchResults) { artist ->
                            ListItem(
                                modifier = Modifier.clickable {
                                    viewModel.fetchArtistStats(artist.id)
                                    repository.updateUserField(liveUser.spotifyId, "spotifyArtistId", artist.id)
                                    if (liveUser.genre.isEmpty() && artist.genres?.isNotEmpty() == true) {
                                        repository.updateUserField(liveUser.spotifyId, "genre", artist.genres.take(3).joinToString(", "))
                                    }
                                    showVerifyDialog = false
                                },
                                headlineContent = { Text(artist.name) },
                                supportingContent = { Text("${artist.followers} followers • ${artist.genres?.take(2)?.joinToString(", ")}") },
                                leadingContent = {
                                    Surface(shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                        if (artist.imageUrl != null) {
                                            AsyncImage(model = artist.imageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                        } else {
                                            Box(contentAlignment = Alignment.Center) { Text("👤") }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") } }
        )
    }

    // edit bio
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
            dismissButton = { TextButton(onClick = { showBioDialog = false }) { Text("Cancel") } }
        )
    }

    // edit genre
    if (showGenreDialog) {
        AlertDialog(
            onDismissRequest = { showGenreDialog = false },
            title = { Text("Select Genres (Max 3)") },
            text = {
                Column {
                    availableGenres.chunked(3).forEach { rowGenres ->
                        Row {
                            rowGenres.forEach { genre ->
                                val isSelected = selectedGenres.contains(genre)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedGenres = selectedGenres - genre
                                        else if (selectedGenres.size < 3) selectedGenres = selectedGenres + genre
                                    },
                                    label = { Text(genre) },
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.updateUserField(liveUser.spotifyId, "genre", selectedGenres.joinToString(", "))
                    showGenreDialog = false
                }) { Text("Save") }
            }
        )
    }

    // main UI layout for profile screen
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { loginViewModel.logout() }) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // profile pic
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { if (!isUploadingProfilePic) photoPickerLauncher.launch("image/*") },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (!liveUser.profilePicUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = liveUser.profilePicUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(liveUser.displayName.take(1), style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }

                // loading spinner with upload of pic
                if (isUploadingProfilePic) {
                    CircularProgressIndicator(modifier = Modifier.size(84.dp), strokeWidth = 2.dp)
                } else {
                    // include the edit icon
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp).offset(x = 4.dp, y = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = liveUser.displayName, style = MaterialTheme.typography.headlineSmall)

                if (liveUser.spotifyArtistId.isNullOrEmpty()) {
                    TextButton(
                        onClick = { showVerifyDialog = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Verify Artist Identity +", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Spotify Verified", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                }

                Text(
                    text = if (liveUser.genre.isEmpty()) "Select Genres +" else liveUser.genre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showGenreDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // bio section
        Column(modifier = Modifier.fillMaxWidth().clickable { showBioDialog = true }.padding(vertical = 8.dp)) {
            Text(
                text = if (liveUser.bio.isEmpty()) "Add a bio to your profile..." else liveUser.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = if (liveUser.bio.isEmpty()) Color.Gray else Color.Unspecified
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // follower following stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem("${userPosts.size}", "Posts")
            StatItem("${liveUser.followers.size}", "Followers") { showUserList = "Followers" to liveUser.followers }
            StatItem("${liveUser.following.size}", "Following") { showUserList = "Following" to liveUser.following }
        }

        // spotify stats button
        if (!liveUser.spotifyArtistId.isNullOrEmpty()) {
            var showStatsDialog by remember { mutableStateOf(false) }
            val stats by viewModel.selectedArtistStats.observeAsState()

            Button(
                onClick = {
                    viewModel.fetchArtistStats(liveUser.spotifyArtistId)
                    showStatsDialog = true
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Spotify Stats")
            }

            if (showStatsDialog) {
                AlertDialog(
                    onDismissRequest = { showStatsDialog = false },
                    title = { Text("${stats?.name ?: "Artist"} Live Stats") },
                    text = {
                        if (stats == null) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF1DB954))
                                Spacer(Modifier.height(8.dp))
                                Text("Fetching live data...")
                            }
                        } else {
                            SpotifyStatsScreen(artist = stats!!)
                        }
                    },
                    confirmButton = { TextButton(onClick = { showStatsDialog = false }) { Text("Close") } }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()

        // post grid layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(userPosts) { post ->
                Surface(modifier = Modifier.aspectRatio(1f), color = MaterialTheme.colorScheme.surfaceVariant) {
                    if (post.imageUrl != null) {
                        AsyncImage(model = post.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text("🎵") }
                    }
                }
            }
        }
    }

    showUserList?.let { (title, ids) ->
        UserListSheet(title = title, userIds = ids, onDismiss = { showUserList = null })
    }
}

@Composable
fun StatItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Text(count, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}