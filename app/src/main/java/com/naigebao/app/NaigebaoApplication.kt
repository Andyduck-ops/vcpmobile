package com.naigebao.app

import android.app.Application
import android.util.Log
import com.naigebao.network.websocket.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NaigebaoApplication : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.syncManager.start()
        appScope.launch {
            container.cleanupManager.cleanup()
        }
        appScope.launch {
            container.webSocketManager.connectionState.collectLatest { state ->
                if (state is ConnectionState.Connected) {
                    container.messageResendManager.resendPending()
                }
            }
        }
        Log.i("NaigebaoApplication", "Naigebao bootstrap initialized")
    }
}
