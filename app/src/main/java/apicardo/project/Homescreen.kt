package apicardo.project

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, onUserClick: (String) -> Unit) {
    val posts by viewModel.posts.observeAsState(initial = emptyList())
    val currentUser by viewModel.currentUser.observeAsState()
    val mediaPlayer = remember { MediaPlayer() }
    var playingPostId by remember { mutableStateOf<String?>(null) }
    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    val sheetState = rememberModalBottomSheetState()

    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchPosts()
    }

    if (selectedPostForComments != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPostForComments = null },
            sheetState = sheetState
        ) {
            CommentSheetContent(
                post = selectedPostForComments!!,
                onPostComment = { text ->
                    viewModel.postComment(selectedPostForComments!!.postId, text)
                }
            )
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("findie", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        items(posts) { post ->
            val isThisPlaying = playingPostId == post.postId
            val isLiked = post.likedBy.contains(currentUser?.spotifyId)

            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onUserClick(post.authorId) }
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

                    if (!post.audioUrl.isNullOrEmpty()) {
                        Button(
                            onClick = {
                                if (isThisPlaying) {
                                    mediaPlayer.stop()
                                    mediaPlayer.reset()
                                    playingPostId = null
                                } else {
                                    try {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(post.audioUrl)
                                        mediaPlayer.prepareAsync()
                                        mediaPlayer.setOnPreparedListener {
                                            it.start()
                                            playingPostId = post.postId
                                        }
                                        mediaPlayer.setOnCompletionListener { playingPostId = null }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ANIMATED LIKE BUTTON
                        LikeButton(
                            isLiked = isLiked,
                            onLikeClick = {
                                currentUser?.let { user ->
                                    viewModel.toggleLike(post.postId, user.spotifyId, isLiked)
                                }
                            }
                        )
                        Text("${post.likedBy.size}")

                        Spacer(modifier = Modifier.width(16.dp))

                        // COMMENT BUTTON
                        IconButton(onClick = { selectedPostForComments = post }) {
                            Icon(Icons.Default.Send, "Reply")
                        }
                        Text("${post.comments.size}")
                    }

                    if (post.comments.isNotEmpty()) {
                        val lastComment = post.comments.last()
                        Text(
                            text = "${lastComment.userName}: ${lastComment.text}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = Color.Gray
                        )
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

@Composable
fun LikeButton(isLiked: Boolean, onLikeClick: () -> Unit) {
    // animation for like button
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LikeScale"
    )

    IconButton(onClick = onLikeClick) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Like",
            tint = if (isLiked) Color.Red else Color.Gray,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun CommentSheetContent(post: Post, onPostComment: (String) -> Unit) {
    var newCommentText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxHeight(0.6f).padding(16.dp)) {
        Text("Comments", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            items(post.comments) { comment ->
                // animation for comment section
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Column {
                        Text(comment.userName, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...") }
            )
            IconButton(onClick = {
                if (newCommentText.isNotBlank()) {
                    onPostComment(newCommentText)
                    newCommentText = ""
                }
            }) {
                Icon(Icons.Default.Check, "Post")
            }
        }
    }
}