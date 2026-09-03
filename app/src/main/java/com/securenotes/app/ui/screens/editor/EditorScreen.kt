package com.securenotes.app.ui.screens.editor

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securenotes.app.MainActivity
import com.securenotes.app.R
import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.ui.theme.SecureNotesTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun EditorRoute(
    noteId: Long,
    initialFolderId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val viewModel: EditorViewModel = viewModel(
        key = "editor_$noteId",
        factory = EditorViewModel.factory(noteId, initialFolderId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val contentField by viewModel.contentField.collectAsState()
    val folders by viewModel.folders.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var showChangeConfirmationDialog by remember { mutableStateOf(false) }
    var pendingActionAfterConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showSavedMessage by remember { mutableStateOf(false) }
    var showDeleteTextDialog by remember { mutableStateOf(false) }
    var pendingDeleteValue by remember { mutableStateOf<TextFieldValue?>(null) }

    // Salva automaticamente quando o app vai para segundo plano / é fechado
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.saveAsync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            kotlinx.coroutines.delay(2000)
            showSavedMessage = false
            onNavigateBack()
        }
    }

    val context = LocalContext.current
    var pendingReminderAction by remember { mutableStateOf<ReminderAction?>(null) }
    var showPermissionDenied by remember { mutableStateOf(false) }

    fun showReminderPickers(action: ReminderAction) {
        val initialMillis = when (action) {
            is ReminderAction.Edit -> action.reminderAt
            else -> System.currentTimeMillis() + 60 * 60 * 1000L
        }
        val initial = Calendar.getInstance().apply { timeInMillis = initialMillis }

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val pickedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val reminderAt = Calendar.getInstance().apply {
                            timeInMillis = pickedDate.timeInMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        when (action) {
                            ReminderAction.Add -> viewModel.addReminder(reminderAt)
                            is ReminderAction.Edit -> viewModel.updateReminder(
                                action.reminderId,
                                reminderAt
                            )
                        }
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    true
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val permissionLauncher: (() -> Unit) = {
        pendingReminderAction?.let { action ->
            (context as? MainActivity)?.requestNotificationPermission { granted ->
                pendingReminderAction = null
                if (granted) {
                    showReminderPickers(action)
                } else {
                    showPermissionDenied = true
                }
            }
        }
    }

    fun requestReminderCreation() {
        if (uiState.isNew) return
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            showReminderPickers(ReminderAction.Add)
        } else {
            pendingReminderAction = ReminderAction.Add
            permissionLauncher()
        }
    }

    val lastContentLength = remember { mutableStateOf(contentField.text.length) }

    EditorScreen(
        uiState = uiState,
        contentField = contentField,
        onTitleChange = viewModel::onTitleChange,
        onContentFieldChange = { newValue ->
            val oldLength = lastContentLength.value
            val newLength = newValue.text.length
            if (newLength < oldLength && oldLength > 0 && !uiState.isNew) {
                pendingDeleteValue = newValue
                showDeleteTextDialog = true
            } else {
                lastContentLength.value = newLength
                viewModel.onContentFieldChange(newValue)
            }
        },
        onColorChange = viewModel::onColorChange,
        onFavoriteToggle = viewModel::onFavoriteToggle,
        onPinToggle = viewModel::onPinToggle,
        folders = folders,
        onFolderChange = viewModel::onFolderChange,
        onAddReminder = { requestReminderCreation() },
        onEditReminder = { reminder ->
            showReminderPickers(ReminderAction.Edit(reminder.id, reminder.reminderAt))
        },
        onDeleteReminder = viewModel::deleteReminder,
        onSave = {
            if (viewModel.hasChanges()) {
                pendingActionAfterConfirm = {
                    viewModel.save { showSavedMessage = true }
                }
                showChangeConfirmationDialog = true
            } else {
                viewModel.save { showSavedMessage = true }
            }
        },
        onDelete = { showDeleteDialog = true },
        onBack = {
            if (viewModel.hasChanges()) {
                pendingActionAfterConfirm = {
                    viewModel.save { showSavedMessage = true }
                }
                showChangeConfirmationDialog = true
            } else {
                onNavigateBack()
            }
        },
        onNextIssue = viewModel::nextIssue,
        onAcceptSuggestion = viewModel::acceptSuggestion,
        onIgnoreWord = viewModel::ignoreWord,
        onAddToDictionary = viewModel::addToDictionary,
        onDismissCorrection = viewModel::dismissCurrentIssue,
        onShowAiMenu = { showAiMenu = true },
        onAiImprove = viewModel::improveText,
        onAiSummarize = viewModel::summarizeText,
        onAiCorrect = viewModel::correctText,
        onAiGenerateTitle = viewModel::generateTitleFromContent,
        onDismissAiResult = viewModel::dismissAiResult,
        onApplyAiResult = viewModel::applyAiResult
    )

    if (showSavedMessage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 8.dp
            ) {
                Text(
                    text = "Salvo",
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showPermissionDenied) {
        AlertDialog(
            onDismissRequest = { showPermissionDenied = false },
            title = { Text(stringResource(R.string.permissao_notificacoes_titulo)) },
            text = { Text(stringResource(R.string.permissao_notificacoes_mensagem)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDenied = false
                        pendingReminderAction = ReminderAction.Add
                        permissionLauncher()
                    }
                ) {
                    Text(stringResource(R.string.permitir))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDenied = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.mover_para_lixeira_titulo)) },
            text = { Text(stringResource(R.string.mover_para_lixeira_mensagem)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onNavigateBack)
                    },
                    modifier = Modifier.testTag("confirm_delete")
                ) {
                    Text(stringResource(R.string.mover_para_lixeira))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (showChangeConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showChangeConfirmationDialog = false },
            title = { Text("Tem certeza que deseja alterar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showChangeConfirmationDialog = false
                        pendingActionAfterConfirm?.invoke()
                        pendingActionAfterConfirm = null
                    }
                ) {
                    Text("SIM")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangeConfirmationDialog = false
                        viewModel.restoreOriginal()
                        pendingActionAfterConfirm = null
                    }
                ) {
                    Text("NÃO")
                }
            }
        )
    }

    if (showDeleteTextDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteTextDialog = false
                pendingDeleteValue = null
            },
            title = { Text("Tem certeza que quer excluir?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteTextDialog = false
                        pendingDeleteValue?.let {
                            lastContentLength.value = it.text.length
                            viewModel.onContentFieldChange(it)
                        }
                        pendingDeleteValue = null
                    }
                ) {
                    Text("SIM")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteTextDialog = false
                        pendingDeleteValue = null
                    }
                ) {
                    Text("NÃO")
                }
            }
        )
    }

    if (showAiMenu) {
        AlertDialog(
            onDismissRequest = { showAiMenu = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Assistente IA") },
            text = {
                Column {
                    Text(
                        text = "Escolha uma ação:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextButton(
                        onClick = {
                            showAiMenu = false
                            viewModel.improveText()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Melhorar texto", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            showAiMenu = false
                            viewModel.summarizeText()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Resumir", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            showAiMenu = false
                            viewModel.correctText()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Corrigir ortografia", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            showAiMenu = false
                            viewModel.generateTitleFromContent()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gerar título", modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAiMenu = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (uiState.aiResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiResult() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Resultado da IA") },
            text = {
                Text(
                    text = uiState.aiResult ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applyAiResult() }) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAiResult() }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (uiState.aiError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiResult() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Erro na IA") },
            text = {
                Text(
                    text = uiState.aiError ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAiResult() }) {
                    Text("OK")
                }
            }
        )
    }
}

private sealed class ReminderAction {
    data object Add : ReminderAction()
    data class Edit(val reminderId: Long, val reminderAt: Long) : ReminderAction()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    contentField: TextFieldValue,
    onTitleChange: (String) -> Unit,
    onContentFieldChange: (TextFieldValue) -> Unit,
    onColorChange: (String) -> Unit,
    onFavoriteToggle: () -> Unit,
    onPinToggle: () -> Unit,
    folders: List<Folder>,
    onFolderChange: (Long?) -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onNextIssue: () -> Unit,
    onAcceptSuggestion: (word: String, suggestion: String) -> Unit,
    onIgnoreWord: (String) -> Unit,
    onAddToDictionary: (String) -> Unit,
    onDismissCorrection: () -> Unit,
    onShowAiMenu: () -> Unit = {},
    onAiImprove: () -> Unit = {},
    onAiSummarize: () -> Unit = {},
    onAiCorrect: () -> Unit = {},
    onAiGenerateTitle: () -> Unit = {},
    onDismissAiResult: () -> Unit = {},
    onApplyAiResult: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    var shouldContinueListening by remember { mutableStateOf(false) }
    var pendingVoiceStart by remember { mutableStateOf(false) }

    val inlineIssue = uiState.currentIssue ?: uiState.spellingIssues.lastOrNull()
    var activeRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    // Evita closure obsoleto — sempre usa o valor mais recente do campo
    val latestContentField by rememberUpdatedState(contentField)

    // Timer discreto estilo WhatsApp — conta enquanto ouve
    LaunchedEffect(isListening) {
        if (isListening) {
            elapsedSeconds = 0
            while (isListening) {
                kotlinx.coroutines.delay(1000)
                if (isListening) elapsedSeconds++
            }
        } else {
            elapsedSeconds = 0
        }
    }

    fun destroyRecognizer() {
        val recognizer = activeRecognizer
        activeRecognizer = null
        recognizer?.let {
            try {
                it.destroy()
            } catch (_: Exception) {
            }
        }
    }

    fun insertVoiceText(text: String) {
        if (text.isBlank()) return
        val current = latestContentField
        val cursor = current.selection.start.coerceIn(0, current.text.length)
        val needsSpaceBefore = cursor > 0 && !current.text[cursor - 1].isWhitespace()
        val prefix = if (needsSpaceBefore) " " else ""
        val newText = current.text.substring(0, cursor) + prefix + text +
            current.text.substring(cursor)
        val newCursor = cursor + prefix.length + text.length
        onContentFieldChange(
            TextFieldValue(newText, selection = androidx.compose.ui.text.TextRange(newCursor))
        )
        Log.d("VoiceInput", "Texto de voz inserido no cursor $cursor (conteudo=${newText.length} chars)")
    }

    fun startVoiceInput(isRestart: Boolean = false) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("VoiceInput", "Servico de reconhecimento indisponivel")
                Toast.makeText(context, context.getString(R.string.voz_indisponivel), Toast.LENGTH_SHORT).show()
                isListening = false
                shouldContinueListening = false
                return
            }
            destroyRecognizer()
            val recognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (e: Exception) {
                activeRecognizer = null
                Log.e("VoiceInput", "Falha ao criar SpeechRecognizer", e)
                Toast.makeText(context, context.getString(R.string.voz_indisponivel), Toast.LENGTH_SHORT).show()
                isListening = false
                shouldContinueListening = false
                return
            }
            activeRecognizer = recognizer
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // WhatsApp-like: mantém aberto por ~25s mesmo com silêncio
                try {
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 25000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 25000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 25000L)
                } catch (_: Exception) {}
            }

            fun scheduleRestart() {
                if (!shouldContinueListening) return
                Log.d("VoiceInput", "Agendando reinício contínuo")
                destroyRecognizer()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (shouldContinueListening) {
                        Log.d("VoiceInput", "Reiniciando sessão contínua")
                        startVoiceInput(isRestart = true)
                    }
                }, 350)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Log.d("VoiceInput", "onReadyForSpeech") }
                override fun onBeginningOfSpeech() { Log.d("VoiceInput", "onBeginningOfSpeech") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { Log.d("VoiceInput", "onEndOfSpeech") }
                override fun onError(error: Int) {
                    Log.e("VoiceInput", "SpeechRecognizer onError: $error")
                    // Erros que indicam fim de enunciado — reinicia automaticamente se usuário ainda quer ouvir
                    val shouldRestart = error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                    if (shouldRestart && shouldContinueListening) {
                        scheduleRestart()
                        return
                    }
                    // Erros fatais ou usuário finalizou — encerra
                    if (shouldContinueListening && error == SpeechRecognizer.ERROR_CLIENT) {
                        // CLIENT pode ser stopListening() — não reinicia
                        isListening = false
                        shouldContinueListening = false
                        destroyRecognizer()
                        return
                    }
                    isListening = false
                    shouldContinueListening = false
                    if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                        error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ) {
                        // Não mostra toast se foi restart — só se realmente parou
                    } else if (error != SpeechRecognizer.ERROR_CLIENT) {
                        if (error == SpeechRecognizer.ERROR_NETWORK ||
                            error == SpeechRecognizer.ERROR_SERVER ||
                            error == SpeechRecognizer.ERROR_AUDIO
                        ) {
                            Toast.makeText(context, context.getString(R.string.voz_indisponivel), Toast.LENGTH_SHORT).show()
                        }
                    }
                    destroyRecognizer()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    Log.d("VoiceInput", "onResults: \"$text\" shouldContinue=$shouldContinueListening")
                    if (!text.isNullOrBlank()) {
                        insertVoiceText(text)
                    }
                    if (shouldContinueListening) {
                        scheduleRestart()
                    } else {
                        isListening = false
                        destroyRecognizer()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    // Ignora parciais para evitar duplicação — só finais são inseridos
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            isListening = true
            if (!isRestart) shouldContinueListening = true
            // Tenta silenciar o apito do SpeechRecognizer (estilo WhatsApp discreto)
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_MUTE, 0)
                    am.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, android.media.AudioManager.ADJUST_MUTE, 0)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_UNMUTE, 0)
                            am.adjustStreamVolume(android.media.AudioManager.STREAM_SYSTEM, android.media.AudioManager.ADJUST_UNMUTE, 0)
                        } catch (_: Exception) {}
                    }, 900)
                } else {
                    @Suppress("DEPRECATION")
                    am.setStreamMute(android.media.AudioManager.STREAM_MUSIC, true)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        @Suppress("DEPRECATION")
                        try { am.setStreamMute(android.media.AudioManager.STREAM_MUSIC, false) } catch (_: Exception) {}
                    }, 900)
                }
            } catch (_: Exception) {}
            try {
                recognizer.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                shouldContinueListening = false
                destroyRecognizer()
                Log.e("VoiceInput", "Falha ao iniciar escuta", e)
                Toast.makeText(context, context.getString(R.string.voz_indisponivel), Toast.LENGTH_SHORT).show()
                return
            }
            Log.d("VoiceInput", "startListening pt-BR continuo=$shouldContinueListening restart=$isRestart")
        } catch (e: Exception) {
            isListening = false
            shouldContinueListening = false
            destroyRecognizer()
            Log.e("VoiceInput", "Erro inesperado no reconhecimento de voz", e)
            Toast.makeText(context, context.getString(R.string.voz_indisponivel), Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceInput() {
        Log.d("VoiceInput", "Finalizando reconhecimento contínuo")
        shouldContinueListening = false
        isListening = false
        try {
            activeRecognizer?.stopListening()
        } catch (_: Exception) {}
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            destroyRecognizer()
        }, 400)
    }

    fun onMicClicked() {
        Log.d("VoiceInput", "Botao de microfone tocado isListening=$isListening")
        if (isListening) {
            stopVoiceInput()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("VoiceInput", "Solicitando permissao RECORD_AUDIO")
            (context as? MainActivity)?.requestMicrophonePermission { granted ->
                Log.d("VoiceInput", "RECORD_AUDIO permission result: granted=$granted")
                if (granted) {
                    pendingVoiceStart = true
                }
            }
        } else {
            startVoiceInput()
        }
    }

    LaunchedEffect(pendingVoiceStart) {
        if (pendingVoiceStart) {
            pendingVoiceStart = false
            startVoiceInput()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            shouldContinueListening = false
            destroyRecognizer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isNew) R.string.nova_nota else R.string.editar_nota
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.voltar)
                        )
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(
                            onClick = onPinToggle,
                            modifier = Modifier.testTag("pin_button")
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_pin),
                                contentDescription = stringResource(
                                    if (uiState.pinned) R.string.desafixar_nota
                                    else R.string.fixar_nota
                                ),
                                tint = if (uiState.pinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(
                                if (uiState.favorite) R.string.desmarcar_como_favorita
                                else R.string.marcar_como_favorita
                            ),
                            tint = if (uiState.favorite) {
                                Color(0xFFFFC107)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (!uiState.isNew) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.excluir)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { onMicClicked() },
                        containerColor = if (isListening) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .padding(start = 12.dp)
                            .testTag("mic_button")
                    ) {
                        if (isListening) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.microfone_finalizar),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_mic),
                                contentDescription = stringResource(R.string.falar_para_escrever),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = onSave,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp)
                        .testTag("save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.salvar),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 4.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isListening) {
                    // Barra discreta estilo WhatsApp — suave e moderna
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("listening_banner"),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        Color(0xFFE53935),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                text = stringResource(R.string.microfone_ouvindo) + " " +
                                    String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { stopVoiceInput() },
                                modifier = Modifier.testTag("mic_finalizar")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(stringResource(R.string.microfone_finalizar))
                            }
                        }
                    }
                }
                BasicTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            if (uiState.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.titulo_nota),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_field")
                )
                if (uiState.isNew) {
                    Column {
                        Text(
                            text = stringResource(R.string.lembrete_salvar_primeiro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.lembretes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onAddReminder,
                                modifier = Modifier.testTag("add_reminder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(stringResource(R.string.adicionar_lembrete))
                            }
                        }
                        if (uiState.reminders.isEmpty()) {
                            Text(
                                text = stringResource(R.string.sem_lembretes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.reminders.forEach { reminder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .testTag("reminder_item_${reminder.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = stringResource(
                                            R.string.lembrete_data,
                                            formatReminderDate(reminder.reminderAt)
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onEditReminder(reminder) },
                                        modifier = Modifier.testTag("edit_reminder_${reminder.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.editar_lembrete),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteReminder(reminder.id) },
                                        modifier = Modifier.testTag("delete_reminder_${reminder.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.excluir_lembrete),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    androidx.compose.material3.TextField(
                        value = contentField,
                        onValueChange = onContentFieldChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        visualTransformation = SpellingUnderlineTransformation(uiState.spellingIssues),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("content_field")
                    )
                }
                val inlineSuggestions = inlineIssue?.suggestions ?: emptyList()
                androidx.compose.animation.AnimatedVisibility(
                    visible = inlineSuggestions.isNotEmpty(),
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inlineIssue?.word ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        inlineSuggestions.forEach { suggestion ->
                            TextButton(
                                onClick = { onAcceptSuggestion(inlineIssue!!.word, suggestion) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("inline_suggestion_${suggestion}")
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatReminderDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    SecureNotesTheme {
        EditorScreen(
            uiState = EditorUiState(
                noteId = 1,
                title = "Reunião de segunda-feira",
                content = "Preparar a apresentação do projeto.",
                isNew = false,
                isLoading = false
            ),
            contentField = TextFieldValue("Preparar a apresentação do projeto."),
            onTitleChange = {},
            onContentFieldChange = {},
            onColorChange = {},
            onFavoriteToggle = {},
            onPinToggle = {},
            folders = emptyList(),
            onFolderChange = {},
            onAddReminder = {},
            onEditReminder = {},
            onDeleteReminder = {},
            onSave = {},
            onDelete = {},
            onBack = {},
            onNextIssue = {},
            onAcceptSuggestion = { _, _ -> },
            onIgnoreWord = {},
            onAddToDictionary = {},
            onDismissCorrection = {},
            onShowAiMenu = {},
            onAiImprove = {},
            onAiSummarize = {},
            onAiCorrect = {},
            onAiGenerateTitle = {},
            onDismissAiResult = {},
            onApplyAiResult = {}
        )
    }
}

private class SpellingUnderlineTransformation(
    private val issues: List<com.securenotes.app.spellcheck.SpellingIssue>
) : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        if (issues.isEmpty()) return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
        for (issue in issues) {
            val start = issue.offset.coerceIn(0, text.length)
            val end = (issue.offset + issue.length).coerceIn(0, text.length)
            if (start < end) {
                builder.addStyle(
                    androidx.compose.ui.text.SpanStyle(
                        color = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    start,
                    end
                )
            }
        }
        return androidx.compose.ui.text.input.TransformedText(builder.toAnnotatedString(), androidx.compose.ui.text.input.OffsetMapping.Identity)
    }
}
