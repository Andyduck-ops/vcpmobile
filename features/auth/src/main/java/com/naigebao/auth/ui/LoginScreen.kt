package com.naigebao.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.naigebao.auth.viewmodel.AuthUiState

@Composable
fun LoginScreen(
    state: AuthUiState,
    onServerUrlChange: (String) -> Unit,
    onVcpKeyChange: (String) -> Unit,
    onGenerateQr: () -> Unit,
    onScanQr: () -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Naigebao Login", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Device: ${state.deviceFingerprint.model} • ${state.deviceFingerprint.hashedId.take(10)}",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.vcpKey,
            onValueChange = onVcpKeyChange,
            label = { Text("VCP Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onGenerateQr, modifier = Modifier.fillMaxWidth()) {
            Text("Generate QR")
        }
        Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
            Text("Scan QR")
        }
        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
            Text("Connect WebSocket")
        }
        state.qrCode?.let { QrCodeDisplay(it) }
        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun QrCodeDisplay(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = "Login QR Code",
        modifier = Modifier.size(220.dp)
    )
}
