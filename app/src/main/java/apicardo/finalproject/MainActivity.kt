package apicardo.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import apicardo.finalproject.ui.theme.MyApplicationTheme
import androidx.compose.ui.graphics.vector.ImageVector

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()
            val currentUser by mainViewModel.currentUser.observeAsState()

            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isLoggedIn) {
                        LoginScreen(loginViewModel)
                    } else {
                        LaunchedEffect(isLoggedIn) {
                            loginViewModel.currentUserId?.let { uid ->
                                mainViewModel.startUserSync(uid)
                                mainViewModel.fetchPosts()
                            }
                        }

                        currentUser?.let { safeUser ->
                            MainAppContent(
                                user = safeUser,
                                viewModel = mainViewModel,
                                loginViewModel = loginViewModel
                            )
                        } ?: run {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(user: ArtistUser, viewModel: MainViewModel, loginViewModel: LoginViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // navigation states for other users
    var selectedOtherUserId by remember { mutableStateOf<String?>(null) }
    var directChatUser by remember { mutableStateOf<ArtistUser?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                    selected = (destination == currentDestination && selectedOtherUserId == null && directChatUser == null),
                    onClick = {
                        currentDestination = destination
                        // clear sub-navigation when switching main tabs
                        selectedOtherUserId = null
                        directChatUser = null
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {

                // navigation logic
                when {
                    // show direct chat
                    directChatUser != null -> {
                        MessagingDetailScreen(
                            otherUser = directChatUser!!,
                            viewModel = viewModel,
                            onBack = { directChatUser = null }
                        )
                    }

                    // show other user's profile
                    selectedOtherUserId != null -> {
                        OtherUserProfileScreen(
                            userId = selectedOtherUserId!!,
                            viewModel = viewModel,
                            onMessageClick = { artist ->
                                directChatUser = artist
                            },
                            onBack = { selectedOtherUserId = null }
                        )
                    }

                    // main tab destinations
                    else -> {
                        when (currentDestination) {
                            AppDestinations.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onUserClick = { userId -> selectedOtherUserId = userId }
                            )

                            AppDestinations.MESSAGES -> {
                                var selectedChatUser by remember { mutableStateOf<ArtistUser?>(null) }
                                if (selectedChatUser == null) {
                                    MessagingScreen(
                                        viewModel = viewModel,
                                        onUserSelected = { selectedChatUser = it }
                                    )
                                } else {
                                    MessagingDetailScreen(
                                        otherUser = selectedChatUser!!,
                                        viewModel = viewModel,
                                        onBack = { selectedChatUser = null }
                                    )
                                }
                            }

                            AppDestinations.UPLOAD -> UploadScreen(viewModel = viewModel)

                            AppDestinations.PROFILE -> ProfileScreen(
                                user = user,
                                loginViewModel = loginViewModel,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}


enum class AppDestinations(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    MESSAGES("Chat", Icons.Default.Email),
    UPLOAD("Upload", Icons.Default.AddCircle),
    PROFILE("Profile", Icons.Default.Person)
}

