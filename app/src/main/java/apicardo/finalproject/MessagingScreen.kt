package apicardo.finalproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// temporary messaging screen will implement actual screen
@Composable
fun MessagingScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Messages", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(10) { index ->
                val isMe = index % 2 != 0
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = if (!isMe) "Hey! Want to collab?" else "Yeah, let's do it!",
                        modifier = Modifier
                            .align(if (isMe) Alignment.CenterEnd else Alignment.CenterStart)
                            .background(if (isMe) Color.Cyan else Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            TextField(value = "", onValueChange = {}, modifier = Modifier.weight(1f), placeholder = { Text("Type message...") })
            IconButton(onClick = { }) { Icon(Icons.Default.Send, "Send") }
        }
    }
}