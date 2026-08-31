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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
fun SecuritySetupRoute(
    securityManager: SecurityManager,
    biometricAuthenticator: BiometricAuthenticator,
    onComplete: () -> Unit
) {
    val viewModel: SecuritySetupViewModel = viewModel {
        SecuritySetupViewModel(securityManager, biometricAuthenticator)
    }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.requestBiometricAuth) {
        if (uiState.requestBiometricAuth) {
            val success = biometricAuthenticator.authenticate()
            viewModel.onBiometricAuthResult(success)
        }
    }

    SecuritySetupScreen(
        uiState = uiState,
        onPinChange = viewModel::onPinChange,
        onConfirmPinChange = viewModel::onConfirmPinChange,
        onBiometricCandidateChange = viewModel::onBiometricCandidateChange,
        onSave = { viewModel.save(onComplete) }
    )
}

@Composable
private fun SecuritySetupScreen(
    uiState: SecuritySetupUiState,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onBiometricCandidateChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.configurar_seguranca),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.configurar_seguranca_mensagem),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        PinTextField(
            value = uiState.pin,
            onValueChange = onPinChange,
            label = stringResource(R.string.criar_pin),
            modifier = Modifier.fillMaxWidth(),
            testTag = "setup_pin",
            isError = uiState.error != null
        )
        Spacer(modifier = Modifier.height(12.dp))
        PinTextField(
            value = uiState.confirmPin,
            onValueChange = onConfirmPinChange,
            label = stringResource(R.string.confirmar_pin),
            modifier = Modifier.fillMaxWidth(),
            testTag = "setup_confirm_pin",
            isError = uiState.error != null
        )
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("setup_error")
            )
        }
        if (uiState.biometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.ativar_biometria),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.biometria_descricao),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Switch(
                        checked = uiState.biometricEnabledCandidate,
                        onCheckedChange = onBiometricCandidateChange,
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("setup_biometric_switch")
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onSave,
            enabled = !uiState.busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("setup_save")
        ) {
            Text(stringResource(R.string.salvar_seguranca))
        }
    }
}