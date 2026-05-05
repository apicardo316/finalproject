package apicardo.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
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
fun OtherUserProfileScreen(
    userId: String,
    viewModel: MainViewModel,
    onMessageClick: (ArtistUser) -> Unit,
    onBack: () -> Unit
) {
    val repository = FindieRepository()
    val currentUser by viewModel.currentUser.observeAsState()

    // able to observe global artist stats from ViewModel
    val stats by viewModel.selectedArtistStats.observeAsState()

    // states for data
    var userProfile by remember { mutableStateOf<ArtistUser?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var showUserList by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var showStatsDialog by remember { mutableStateOf(false) }

    val isFollowing = currentUser?.following?.contains(userId) == true

    LaunchedEffect(userId) {
        repository.getUserDataOnce(userId) { fetchedUser ->
            userProfile = fetchedUser
        }
        repository.getUserPosts(userId) { posts ->
            userPosts = posts
        }
    }

    if (userProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val user = userProfile!!

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // formatting
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("Artist Profile", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // profile header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    // check firebase for profile pic update
                    if (!user.profilePicUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = user.profilePicUrl,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(user.displayName.take(1), style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = user.displayName, style = MaterialTheme.typography.headlineSmall)
                        if (!user.spotifyArtistId.isNullOrEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (user.genre.isEmpty()) "No Genres Listed" else user.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // buttons present for user to click
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        currentUser?.let { me ->
                            repository.toggleFollow(me.spotifyId, userId, isFollowing)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color.LightGray else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isFollowing) "Unfollow" else "Follow",
                        color = if (isFollowing) Color.Black else Color.White
                    )
                }

                Button(
                    onClick = { onMessageClick(user) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Message")
                }
            }

            // spotify stats button for verified artists
            if (!user.spotifyArtistId.isNullOrEmpty()) {
                Button(
                    onClick = {
                        viewModel.fetchArtistStats(user.spotifyArtistId!!)
                        showStatsDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Spotify Stats")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // bio
            Text("About", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                text = if (user.bio.isEmpty()) "No bio provided." else user.bio,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // display stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${userPosts.size}", style = MaterialTheme.typography.titleLarge)
                    Text("Posts", style = MaterialTheme.typography.bodySmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showUserList = "Followers" to user.followers }
                ) {
                    Text("${user.followers.size}", style = MaterialTheme.typography.titleLarge)
                    Text("Followers", style = MaterialTheme.typography.bodySmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showUserList = "Following" to user.following }
                ) {
                    Text("${user.following.size}", style = MaterialTheme.typography.titleLarge)
                    Text("Following", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()

            // post grid layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(userPosts) { post ->
                    Surface(modifier = Modifier.aspectRatio(1f), color = MaterialTheme.colorScheme.surfaceVariant) {
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

    // stats dialog
    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("${userProfile?.displayName}'s Live Stats") },
            text = {
                if (stats == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1DB954))
                    }
                } else {
                    SpotifyStatsScreen(artist = stats!!)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) { Text("Close") }
            }
        )
    }

    showUserList?.let { (title, ids) ->
        UserListSheet(title = title, userIds = ids, onDismiss = { showUserList = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListSheet(title: String, userIds: List<String>, onDismiss: () -> Unit) {
    val repository = FindieRepository()
    var users by remember { mutableStateOf<List<ArtistUser>>(emptyList()) }

    LaunchedEffect(userIds) {
        userIds.forEach { id ->
            repository.getUserData(id) { user ->
                if (user != null) users = (users + user).distinctBy { it.spotifyId }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxHeight(0.6f).padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(users) { user ->
                    ListItem(
                        headlineContent = { Text(user.displayName) },
                        supportingContent = { Text(user.genre) },
                        leadingContent = {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                                if (!user.profilePicUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = user.profilePicUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(user.displayName.take(1))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}