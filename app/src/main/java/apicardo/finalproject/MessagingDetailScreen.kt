package apicardo.finalproject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MessagingDetailScreen(otherUser: ArtistUser, viewModel: MainViewModel, onBack: () -> Unit) {
    val currentUser = viewModel.currentUser.value ?: return
    var messages by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var textMessage by remember { mutableStateOf("") }
    val repository = FindieRepository()

    LaunchedEffect(otherUser.spotifyId) {
        repository.listenForMessages(currentUser.spotifyId, otherUser.spotifyId) { updatedMessages ->
            messages = updatedMessages
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(otherUser.displayName, style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                val isMe = msg["senderId"] == currentUser.spotifyId
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Surface(
                        color = if (isMe) Color(0xFF00BCD4) else Color.LightGray,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(if (isMe) Alignment.CenterEnd else Alignment.CenterStart)
                    ) {
                        Text(msg["text"].toString(), modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        Row(modifier = Modifier.windowInsetsPadding(WindowInsets.ime)) {
            TextField(
                value = textMessage,
                onValueChange = { textMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") }
            )
            IconButton(onClick = {
                if (textMessage.isNotBlank()) {
                    repository.sendMessage(currentUser.spotifyId, otherUser.spotifyId, textMessage)
                    textMessage = ""
                }
            }) { Icon(Icons.Default.Send, "Send") }
        }
    }
}