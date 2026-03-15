package com.naigebao.auth.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naigebao.auth.device.DeviceFingerprint
import com.naigebao.auth.qr.QrCodeManager
import com.naigebao.auth.qr.QrLoginPayload
import com.naigebao.auth.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val serverUrl: String = "",
    val vcpKey: String = "",
    val deviceFingerprint: DeviceFingerprint,
    val qrCode: ImageBitmap? = null,
    val status: AuthStatus = AuthStatus.Idle,
    val error: String? = null
)

sealed class AuthStatus {
    data object Idle : AuthStatus()
    data object Ready : AuthStatus()
    data object Connecting : AuthStatus()
    data object Connected : AuthStatus()
    data class Failed(val reason: String) : AuthStatus()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val qrCodeManager: QrCodeManager,
    private val deviceFingerprint: DeviceFingerprint,
    private val connectWebSocket: (String) -> Unit
) : ViewModel() {
    private val _state = MutableStateFlow(
        AuthUiState(
            serverUrl = authRepository.currentServerConfig()?.serverUrl.orEmpty(),
            vcpKey = authRepository.currentServerConfig()?.vcpKey.orEmpty(),
            deviceFingerprint = deviceFingerprint
        )
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun updateServerUrl(value: String) {
        _state.value = _state.value.copy(serverUrl = value)
    }

    fun updateVcpKey(value: String) {
        _state.value = _state.value.copy(vcpKey = value)
    }

    fun generateQrCode() {
        val current = _state.value
        if (current.serverUrl.isBlank() || current.vcpKey.isBlank()) {
            _state.value = current.copy(
                status = AuthStatus.Failed("Server URL and VCP Key are required"),
                error = "Server URL and VCP Key are required"
            )
            return
        }
        viewModelScope.launch {
            val payload = QrLoginPayload(
                serverUrl = current.serverUrl,
                vcpKey = current.vcpKey,
                deviceId = current.deviceFingerprint.hashedId,
                issuedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + QR_TTL_MILLIS
            )
            val qr = qrCodeManager.generateImageBitmap(payload)
            _state.value = current.copy(qrCode = qr, status = AuthStatus.Ready, error = null)
        }
    }

    fun onQrScanned(raw: String) {
        val payload = authRepository.decodeQrPayload(raw)
        if (payload == null) {
            _state.value = _state.value.copy(status = AuthStatus.Failed("Invalid QR payload"), error = "Invalid QR payload")
            return
        }
        when (val result = authRepository.authenticate(payload)) {
            is AuthRepository.AuthResult.Success -> {
                _state.value = _state.value.copy(
                    serverUrl = result.serverUrl,
                    vcpKey = result.vcpKey,
                    status = AuthStatus.Connecting,
                    error = null
                )
                connect()
            }
            is AuthRepository.AuthResult.Failed -> {
                _state.value = _state.value.copy(status = AuthStatus.Failed(result.reason), error = result.reason)
            }
        }
    }

    fun connect() {
        val current = _state.value
        if (current.serverUrl.isBlank() || current.vcpKey.isBlank()) {
            _state.value = current.copy(status = AuthStatus.Failed("Missing server config"))
            return
        }
        val payload = QrLoginPayload(
            serverUrl = current.serverUrl,
            vcpKey = current.vcpKey,
            deviceId = current.deviceFingerprint.hashedId,
            issuedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + QR_TTL_MILLIS
        )
        when (val result = authRepository.authenticate(payload)) {
            is AuthRepository.AuthResult.Success -> {
                val wsUrl = authRepository.buildWebSocketUrl() ?: return
                _state.value = _state.value.copy(status = AuthStatus.Connecting)
                connectWebSocket(wsUrl)
                _state.value = _state.value.copy(status = AuthStatus.Connected)
            }
            is AuthRepository.AuthResult.Failed -> {
                _state.value = _state.value.copy(status = AuthStatus.Failed(result.reason))
            }
        }
    }

    companion object {
        private const val QR_TTL_MILLIS = 10 * 60 * 1000L
    }
}
