package com.securenotes.app.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securenotes.app.R
import com.securenotes.app.security.BiometricAuthenticator
import com.securenotes.app.security.SecurityManager

@Composable
fun LockRoute(
    securityManager: SecurityManager,
    biometricAuthenticator: BiometricAuthenticator
) {
    val viewModel: LockViewModel = viewModel {
        LockViewModel(securityManager, biometricAuthenticator)
    }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.requestBiometric) {
        if (uiState.requestBiometric) {
            val success = biometricAuthenticator.authenticate()
            viewModel.onBiometricResult(success)
        }
    }

    LockScreen(
        uiState = uiState,
        onPinChange = viewModel::onPinChange,
        onUnlock = viewModel::unlock,
        onUseBiometric = viewModel::useBiometric,
        onUsePin = viewModel::usePin
    )
}

@Composable
private fun LockScreen(
    uiState: LockUiState,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onUseBiometric: () -> Unit,
    onUsePin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.bloqueio_titulo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.bloqueio_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (uiState.showBiometric) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onUseBiometric,
                enabled = !uiState.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("lock_biometric")
            ) {
                Text(stringResource(R.string.desbloquear_com_biometria))
            }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onUsePin,
                modifier = Modifier.testTag("lock_use_pin")
            ) {
                Text(stringResource(R.string.usar_pin))
            }
        }
        if (uiState.usePin) {
            Spacer(modifier = Modifier.height(24.dp))
            PinTextField(
                value = uiState.pin,
                onValueChange = onPinChange,
                label = stringResource(R.string.campo_pin),
                modifier = Modifier.fillMaxWidth(),
                testTag = "lock_pin",
                isError = uiState.error != null
            )
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("lock_error")
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onUnlock,
                enabled = uiState.pin.isNotEmpty() && !uiState.busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("lock_submit")
            ) {
                Text(stringResource(R.string.desbloquear))
            }
        }
    }
}