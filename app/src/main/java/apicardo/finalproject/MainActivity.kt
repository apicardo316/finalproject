package apicardo.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import apicardo.finalproject.ui.theme.MyApplicationTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Person

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.observeAsState(UIState.Loading as UIState)

            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is UIState.Loading -> {
                            LoginGate(onLogin = { token -> viewModel.verifyUser(token) })
                        }
                        is UIState.Success -> {
                            if (state.user.isArtist) {
                                MainAppContent(state.user, viewModel = viewModel)
                            } else {
                                AccessDeniedScreen()
                            }
                        }
                        is UIState.Error -> {
                            ErrorScreen(state.message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(user: ArtistUser, viewModel: MainViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = destination == currentDestination,
                    onClick = { currentDestination = destination }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    // pass viewModel to HomeScreen to fetch the posts
                    AppDestinations.HOME -> HomeScreen(viewModel = viewModel)

                    AppDestinations.MESSAGES -> MessagingScreen()
                    AppDestinations.UPLOAD -> UploadScreen()

                    // passing directly temporarily, but viewModel will be implemented
                    AppDestinations.PROFILE -> ProfileScreen(user = user)
                }
            }
        }
    }
}

@Composable
fun AccessDeniedScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Artist Account Required", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "findie is an exclusive community for Spotify Artists.",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoginGate(onLogin: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { onLogin("example_token") }) {
            Text("Login with Spotify")
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall)
        Text(text = message, textAlign = TextAlign.Center)
    }
}



enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MESSAGES("Chat", Icons.Default.Email),
    UPLOAD("Upload", Icons.Default.AddCircle),
    PROFILE("Profile", Icons.Default.Person)
}