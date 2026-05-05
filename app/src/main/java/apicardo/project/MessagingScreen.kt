package apicardo.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MessagingScreen(viewModel: MainViewModel, onUserSelected: (ArtistUser) -> Unit) {
    val currentUser = viewModel.currentUser.value ?: return
    var conversationPartners by remember { mutableStateOf<List<ArtistUser>>(emptyList()) }
    val repository = FindieRepository()

    LaunchedEffect(currentUser.spotifyId) {
        repository.getActiveConversations(currentUser.spotifyId) { uids ->
            uids.forEach { uid ->
                repository.getUserData(uid) { user ->
                    if (user != null && !conversationPartners.any { it.spotifyId == user.spotifyId }) {
                        conversationPartners = conversationPartners + user
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Messages",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(conversationPartners) { user ->
                ListItem(
                    headlineContent = { Text(user.displayName) },
                    supportingContent = { Text("Click to chat") },
                    leadingContent = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(50.dp)
                        ) {
                            // check if the user has a profile picture
                            if (!user.profilePicUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = user.profilePicUrl,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {

                                Box(contentAlignment = Alignment.Center) {
                                    Text(user.displayName.take(1), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable { onUserSelected(user) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}