package apicardo.finalproject

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val posts by viewModel.posts.observeAsState(initial = emptyList())

    // media player state and current playing tracker
    val mediaPlayer = remember { MediaPlayer() }
    var playingPostId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchPosts()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("findie", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(posts) { post ->
            // check specific post if it is the one playing
            val isThisPlaying = playingPostId == post.postId

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!post.imageUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "Post Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // audio player
                    if (!post.audioUrl.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                if (isThisPlaying) {
                                    // if currently playing then stop
                                    mediaPlayer.stop()
                                    mediaPlayer.reset()
                                    playingPostId = null
                                } else {
                                    // if not playing then start
                                    try {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(post.audioUrl)
                                        mediaPlayer.prepareAsync()
                                        mediaPlayer.setOnPreparedListener {
                                            it.start()
                                            playingPostId = post.postId
                                        }
                                        // reset when song ends
                                        mediaPlayer.setOnCompletionListener {
                                            playingPostId = null
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            // change button color when playing
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isThisPlaying) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isThisPlaying) "Stop Demo" else "Play Demo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = post.content)

                    Row {
                        IconButton(onClick = { }) { Icon(Icons.Default.FavoriteBorder, "Like") }
                        IconButton(onClick = { }) { Icon(Icons.Default.Send, "Reply") }
                    }
                }
            }
        }

        if (posts.isEmpty()) {
            item {
                Text(
                    "No posts yet. Be the first to share something!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
    }
}