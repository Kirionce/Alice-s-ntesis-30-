package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.FirebaseGeminiService
import com.example.data.ai.configureAppCheck
import com.example.data.firebase.FirestoreChatRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOutUser
import com.example.data.preferences.AppPreferences
import com.example.data.voice.VoicePlaybackManager
import com.example.ui.MainViewModel
import com.example.ui.screens.AssistantsProfileScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.theme.AliceGold
import com.example.ui.theme.AliceGoldContainer
import com.example.ui.theme.AliceYuiTheme
import com.example.ui.theme.MidnightAbyss
import com.example.ui.theme.MidnightCanvas
import com.example.ui.theme.StarlightMuted
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val ROUTE_CHAT = "chat"
private const val ROUTE_ASSISTANTS = "assistants"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val AppDestinations = listOf(
    NavDestination(
        route = ROUTE_CHAT,
        labelRes = R.string.nav_chat,
        selectedIcon = Icons.Rounded.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_item_chat"
    ),
    NavDestination(
        route = ROUTE_ASSISTANTS,
        labelRes = R.string.nav_assistants,
        selectedIcon = Icons.Rounded.Groups,
        unselectedIcon = Icons.Outlined.Groups,
        testTag = "nav_item_assistants"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModel creation
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AliceYuiTheme(darkTheme = true) {
                AppNavigationRoot()
            }
        }
    }
}

@Composable
fun AppNavigationRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener emits updated FirebaseUser */ })
    } else {
        key(user.uid) {
            AuthenticatedAliceYuiApp(
                currentUserId = user.uid,
                userEmail = user.email
            )
        }
    }
}

@Composable
private fun AuthenticatedAliceYuiApp(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    firestoreRepository = FirestoreChatRepository(db),
                    geminiService = FirebaseGeminiService(),
                    preferences = AppPreferences(app),
                    voicePlayer = VoicePlaybackManager(app),
                    currentUserId = currentUserId,
                    userEmail = userEmail
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CHAT

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val speakingMessageId by viewModel.speakingMessageId.collectAsStateWithLifecycle()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = MidnightAbyss,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MidnightCanvas,
                        tonalElevation = 0.dp
                    ) {
                        AppDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AliceGold,
                                    selectedTextColor = AliceGold,
                                    indicatorColor = AliceGoldContainer,
                                    unselectedIconColor = StarlightMuted,
                                    unselectedTextColor = StarlightMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MidnightCanvas,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                modifier = Modifier.testTag(dest.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AliceGold,
                                    selectedTextColor = AliceGold,
                                    indicatorColor = AliceGoldContainer,
                                    unselectedIconColor = StarlightMuted,
                                    unselectedTextColor = StarlightMuted
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_CHAT,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_CHAT) {
                        ChatScreen(
                            uiState = uiState,
                            messagesState = messagesState,
                            speakingMessageId = speakingMessageId,
                            onSelectChannel = viewModel::selectChannel,
                            onSendMessage = viewModel::sendMessage,
                            onToggleSpeak = viewModel::toggleSpeakMessage,
                            onToggleAutoVoice = viewModel::setAutoVoiceReply,
                            onClearChat = viewModel::clearCurrentChannel,
                            onDismissBanner = viewModel::dismissBanner
                        )
                    }

                    composable(ROUTE_ASSISTANTS) {
                        AssistantsProfileScreen(
                            uiState = uiState,
                            onToggleAffectionateMode = viewModel::setAffectionateMode,
                            onToggleAutoVoice = viewModel::setAutoVoiceReply,
                            onSelectModel = viewModel::selectGeminiModel,
                            onOpenChannel = { channel ->
                                viewModel.selectChannel(channel)
                                navigateTo(ROUTE_CHAT)
                            },
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }
                }
            }
        }
    }
}
