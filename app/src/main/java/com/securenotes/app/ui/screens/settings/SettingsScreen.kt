package com.securenotes.app.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securenotes.app.MainActivity
import com.securenotes.app.R
import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.settings.NotificationSound
import com.securenotes.app.domain.settings.SettingsRepository
import com.securenotes.app.domain.settings.ThemeMode
import com.securenotes.app.reminder.ReminderScheduler
import com.securenotes.app.security.BiometricAuthenticator
import com.securenotes.app.security.BiometricAvailability
import com.securenotes.app.security.SecurityManager
import com.securenotes.app.ui.screens.folder.FolderViewModel
import com.securenotes.app.ui.screens.security.PinTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    securityManager: SecurityManager,
    biometricAuthenticator: BiometricAuthenticator,
    settingsRepository: SettingsRepository,
    reminderScheduler: ReminderScheduler,
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(securityManager, biometricAuthenticator, settingsRepository, reminderScheduler)
    }
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    LaunchedEffect(uiState.requestBiometricAuth) {
        if (uiState.requestBiometricAuth) {
            val success = biometricAuthenticator.authenticate()
            viewModel.onBiometricAuthResult(success)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.configuracoes),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.voltar)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.message?.let { message ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_message")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissMessage) {
                            Text(stringResource(R.string.cancelar))
                        }
                    }
                }
            }

            SectionHeader(text = stringResource(R.string.seguranca))

            ChangePinCard(uiState = uiState, viewModel = viewModel)

            BiometricCard(uiState = uiState, viewModel = viewModel)

            SectionHeader(text = stringResource(R.string.notificacoes))

            ReminderSoundCard(
                uiState = uiState,
                viewModel = viewModel,
                reminderScheduler = reminderScheduler,
                hasNotificationPermission = hasNotificationPermission,
                permissionLauncher = permissionLauncher
            )

            SectionHeader(text = stringResource(R.string.pasta))

            FoldersCard()

            SectionHeader(text = stringResource(R.string.aparencia))

            ThemeCard(uiState = uiState, viewModel = viewModel)

            SectionHeader(text = stringResource(R.string.sobre_o_aplicativo))

            AboutCard()
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun ChangePinCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.alterar_pin),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.alterar_pin_descricao),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (!uiState.showChangePinForm) {
                OutlinedButton(
                    onClick = viewModel::toggleChangePinForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_pin_button")
                ) {
                    Text(stringResource(R.string.alterar_pin))
                }
            } else {
                PinTextField(
                    value = uiState.currentPin,
                    onValueChange = viewModel::onCurrentPinChange,
                    label = stringResource(R.string.pin_atual),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pin_current",
                    isError = uiState.error != null
                )
                Spacer(modifier = Modifier.height(10.dp))
                PinTextField(
                    value = uiState.newPin,
                    onValueChange = viewModel::onNewPinChange,
                    label = stringResource(R.string.novo_pin),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pin_new",
                    isError = uiState.error != null
                )
                Spacer(modifier = Modifier.height(10.dp))
                PinTextField(
                    value = uiState.confirmNewPin,
                    onValueChange = viewModel::onConfirmNewPinChange,
                    label = stringResource(R.string.confirmar_novo_pin),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pin_confirm_new",
                    isError = uiState.error != null
                )
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("settings_error")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Button(
                        onClick = viewModel::savePinChange,
                        enabled = !uiState.busy,
                        modifier = Modifier.testTag("save_pin_change")
                    ) {
                        Text(stringResource(R.string.salvar))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = viewModel::toggleChangePinForm,
                        modifier = Modifier.testTag("cancel_pin_change")
                    ) {
                        Text(stringResource(R.string.cancelar))
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.biometria),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            if (uiState.biometricEnabled) R.string.status_ativada
                            else R.string.status_desativada
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.biometricEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.testTag("biometric_status")
                    )
                }
                Switch(
                    checked = uiState.biometricEnabled,
                    onCheckedChange = viewModel::onBiometricToggleRequested,
                    modifier = Modifier.testTag("biometric_switch")
                )
            }
            if (!uiState.biometricEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::onConfigureBiometric,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("configure_biometric")
                ) {
                    Text(stringResource(R.string.configurar_biometria))
                }
                if (uiState.biometricAvailability == BiometricAvailability.UNAVAILABLE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.biometria_indisponivel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.showBiometricOffConfirm) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.confirmar_biometria_off),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                PinTextField(
                    value = uiState.confirmPinToDisable,
                    onValueChange = viewModel::onConfirmPinToDisableChange,
                    label = stringResource(R.string.pin_atual),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pin_biometric_off",
                    isError = uiState.error != null
                )
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = viewModel::confirmDisableBiometric,
                        modifier = Modifier.testTag("confirm_biometric_off")
                    ) {
                        Text(stringResource(R.string.desbloquear))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        onClick = viewModel::cancelDisableBiometric
                    ) {
                        Text(stringResource(R.string.cancelar))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderSoundCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    reminderScheduler: ReminderScheduler,
    hasNotificationPermission: Boolean,
    permissionLauncher: ActivityResultLauncher<String>
) {
    val activity = LocalContext.current as? MainActivity
    val context = LocalContext.current

    var channelSettingsId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.notificationChannelVersion) {
        channelSettingsId = (reminderScheduler as? com.securenotes.app.reminder.AndroidReminderScheduler)
            ?.currentChannelId()
    }

    val soundOptions = listOf(
        NotificationSound.SYSTEM to stringResource(R.string.settings_sound_system),
        NotificationSound.RINGTONE to stringResource(R.string.settings_sound_ringtone),
        NotificationSound.CUSTOM to stringResource(R.string.settings_sound_custom),
        NotificationSound.SILENT to stringResource(R.string.settings_sound_silent)
    )
    val currentLabel = soundOptions
        .firstOrNull { it.first == uiState.notificationSound }
        ?.second
        .orEmpty()

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.som_lembretes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("reminder_sound_status")
                    )
                }
            }

            if (!hasNotificationPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_notification_permission_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_notification_permission_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_notification_permission")
                ) {
                    Text(stringResource(R.string.settings_notification_permission_button))
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(12.dp))

            soundOptions.forEach { (sound, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.notificationSound == sound,
                            onClick = { viewModel.setSound(sound) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.notificationSound == sound,
                        onClick = null
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (uiState.notificationSound == NotificationSound.CUSTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            activity?.launchRingtonePicker { uri: Uri? ->
                                val uriString = uri?.toString()
                                if (!uriString.isNullOrBlank()) {
                                    viewModel.setCustomSoundUri(uriString)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to launch ringtone picker", e)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_ringtone")
                ) {
                    Text(stringResource(R.string.settings_custom_sound_pick))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_vibration),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = viewModel::setVibrationEnabled,
                    modifier = Modifier.testTag("notification_vibration_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_notifications_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(
                            android.provider.Settings.EXTRA_CHANNEL_ID,
                            channelSettingsId
                        )
                        putExtra(
                            android.provider.Settings.EXTRA_APP_PACKAGE,
                            context.packageName
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_channel_settings")
            ) {
                Text(stringResource(R.string.config_som_notificacao))
            }
        }
    }
}

@Composable
private fun FoldersCard() {
    val folderViewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory)
    val folderUiState by folderViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var folderNameInput by remember { mutableStateOf("") }
    var createFolderError by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.pasta),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            folderUiState.folders.forEach { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { folderToDelete = folder }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(
                                R.string.excluir_pasta_acessibilidade,
                                folder.name
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = {
                        folderNameInput = it
                        createFolderError = false
                    },
                    label = { Text(stringResource(R.string.nome_da_pasta)) },
                    singleLine = true,
                    isError = createFolderError,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(200.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val created = folderViewModel.createFolder(folderNameInput.trim())
                            if (created) {
                                folderNameInput = ""
                                createFolderError = false
                            } else {
                                createFolderError = true
                            }
                        }
                    },
                    enabled = folderNameInput.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        stringResource(R.string.criar_pasta),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (createFolderError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.pasta_ja_existe),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(stringResource(R.string.excluir_pasta_titulo)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.excluir_pasta_mensagem,
                        folder.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = folder
                        folderToDelete = null
                        scope.launch { folderViewModel.deleteFolder(current.id) }
                    }
                ) {
                    Text(stringResource(R.string.excluir))
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@Composable
private fun ThemeCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Usa apenas GetContent para evitar requestCode > 16 bits (PickVisualMedia gera código grande)
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        android.util.Log.d("GalleryPicker", "GetContent result uri=$uri")
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                var finalUriStr = uri.toString()
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val file = java.io.File(context.filesDir, "custom_photo_${System.currentTimeMillis()}.jpg")
                        val bmp = android.graphics.BitmapFactory.decodeStream(input)
                        if (bmp != null) {
                            val ratio = minOf(1f, 1080f / bmp.width, 1920f / bmp.height)
                            val scaled = if (ratio < 1f) {
                                android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
                            } else bmp
                            java.io.FileOutputStream(file).use { out ->
                                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            if (scaled != bmp) scaled.recycle()
                            bmp.recycle()
                            finalUriStr = android.net.Uri.fromFile(file).toString()
                            android.util.Log.d("GalleryPicker", "Copied to internal file: $finalUriStr")
                        } else {
                            context.contentResolver.openInputStream(uri)?.use { input2 ->
                                val file2 = java.io.File(context.filesDir, "custom_photo_${System.currentTimeMillis()}.jpg")
                                file2.outputStream().use { out -> input2.copyTo(out) }
                                finalUriStr = android.net.Uri.fromFile(file2).toString()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GalleryPicker", "Copy failed", e)
                    finalUriStr = uri.toString()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    viewModel.setCustomPhotoUri(finalUriStr)
                }
            }
        } else {
            android.util.Log.d("GalleryPicker", "Picker cancelled")
        }
    }
    val themeOptions = listOf(
        ThemeMode.LIGHT to stringResource(R.string.tema_claro),
        ThemeMode.DARK to stringResource(R.string.tema_escuro),
        ThemeMode.VOLCANIC to stringResource(R.string.tema_vulcanico),
        ThemeMode.FLORAL to stringResource(R.string.tema_floral),
        ThemeMode.GALLERY to stringResource(R.string.tema_foto_galeria),
        ThemeMode.SYSTEM to stringResource(R.string.tema_padrao_sistema)
    )

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.tema),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            themeOptions.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.themeMode == mode,
                            onClick = {
                                android.util.Log.d("GalleryPicker", "Tema clicado: $mode uriBlank=${uiState.customPhotoUri.isBlank()}")
                                if (mode == ThemeMode.GALLERY) {
                                    // Sempre abre o seletor para garantir que o usuário possa escolher/trocar a foto
                                    android.util.Log.d("GalleryPicker", "Lançando picker para GALLERY (sempre)")
                                    try {
                                        photoPicker.launch("image/*")
                                        android.util.Log.d("GalleryPicker", "GetContent lançado")
                                    } catch (e: Exception) {
                                        android.util.Log.e("GalleryPicker", "GetContent falhou", e)
                                        android.widget.Toast.makeText(context, "Galeria não disponível", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    viewModel.setThemeMode(mode)
                                }
                            }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.themeMode == mode,
                        onClick = null
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            // Botão sempre visível para Foto da galeria — garante acesso ao seletor mesmo quando ainda não está em GALLERY
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    android.util.Log.d("GalleryPicker", "Botão Escolher foto clicado")
                    try {
                        photoPicker.launch("image/*")
                    } catch (e: Exception) {
                        android.util.Log.e("GalleryPicker", "GetContent failed", e)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.customPhotoUri.isBlank())
                        stringResource(R.string.escolher_foto) + " — " + stringResource(R.string.tema_foto_galeria)
                    else
                        stringResource(R.string.foto_selecionada)
                )
            }
            if (uiState.customPhotoUri.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.foto_galeria_vazia),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.versao, com.securenotes.app.BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sobre_descricao),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.desenvolvido_por),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.autor),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val TAG = "SettingsScreen"
