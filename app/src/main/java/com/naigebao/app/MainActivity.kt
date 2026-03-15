package com.naigebao.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.naigebao.auth.ui.LoginScreen
import com.naigebao.auth.ui.ScanQrCodeScreen
import com.naigebao.auth.viewmodel.AuthViewModel
import com.naigebao.chat.ui.ChatScreen
import com.naigebao.chat.viewmodel.ChatViewModel
import com.naigebao.sessions.ui.SessionListScreen
import com.naigebao.sessions.viewmodel.SessionListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaigebaoApp(
                container = (application as NaigebaoApplication).container,
                initialSessionId = intent?.getStringExtra("extra_session_id")
            )
        }
    }
}

@Composable
private fun NaigebaoApp(
    container: AppContainer,
    initialSessionId: String?
) {
    val navController = rememberNavController()
    val authViewModel = rememberAuthViewModel(container)
    val authState by authViewModel.state.collectAsState()
    MaterialTheme {
        LaunchedEffect(initialSessionId) {
            if (!initialSessionId.isNullOrBlank()) {
                navController.navigate("chat/$initialSessionId")
            }
        }
        NavHost(navController = navController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    state = authState,
                    onServerUrlChange = authViewModel::updateServerUrl,
                    onVcpKeyChange = authViewModel::updateVcpKey,
                    onGenerateQr = authViewModel::generateQrCode,
                    onScanQr = { navController.navigate("scan") },
                    onConnect = {
                        authViewModel.connect()
                        navController.navigate("sessions")
                    }
                )
            }
            composable("scan") {
                ScanQrCodeScreen(
                    onResult = {
                        authViewModel.onQrScanned(it)
                        navController.popBackStack()
                        navController.navigate("sessions")
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("sessions") {
                val sessionViewModel = rememberSessionListViewModel(container)
                SessionListScreen(
                    viewModel = sessionViewModel,
                    onSessionSelected = { sessionId ->
                        navController.navigate("chat/$sessionId")
                    }
                )
            }
            composable(
                route = "chat/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val chatViewModel = rememberChatViewModel(container, sessionId)
                ChatScreen(
                    viewModel = chatViewModel,
                    title = "Chat",
                    onOpenSession = { newSessionId ->
                        navController.navigate("chat/$newSessionId")
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberAuthViewModel(container: AppContainer): AuthViewModel {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(
                    authRepository = container.authRepository,
                    qrCodeManager = container.qrCodeManager,
                    deviceFingerprint = container.deviceFingerprint,
                    connectWebSocket = { url -> container.webSocketManager.connect(url) }
                ) as T
            }
        }
    }
    return viewModel(factory = factory)
}

@Composable
private fun rememberSessionListViewModel(container: AppContainer): SessionListViewModel {
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SessionListViewModel(
                    sessionRepository = container.sessionRepository,
                    refreshAction = { container.syncManager.requestSync() }
                ) as T
            }
        }
    }
    return viewModel(factory = factory)
}

@Composable
private fun rememberChatViewModel(container: AppContainer, sessionId: String): ChatViewModel {
    val context = LocalContext.current
    val factory = remember(sessionId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(
                    sessionId = sessionId,
                    chatService = container.chatService,
                    messageRepository = container.messageRepository,
                    webSocketManager = container.webSocketManager,
                    context = context
                ) as T
            }
        }
    }
    return viewModel(key = sessionId, factory = factory)
}
