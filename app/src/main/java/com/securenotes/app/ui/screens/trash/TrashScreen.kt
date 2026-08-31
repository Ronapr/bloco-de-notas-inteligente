package com.securenotes.app.ui.screens.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securenotes.app.R
import com.securenotes.app.domain.model.Note
import com.securenotes.app.ui.NoteColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashRoute(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = viewModel(factory = TrashViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showEmptyConfirmation by remember { mutableStateOf(false) }

    TrashScreen(
        notes = uiState.notes,
        onRestore = viewModel::restore,
        onPermanentlyDelete = { noteToDelete = it },
        onEmptyTrash = { showEmptyConfirmation = true },
        onNavigateBack = onNavigateBack
    )

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(stringResource(R.string.excluir_definitivamente_titulo)) },
            text = { Text(stringResource(R.string.excluir_definitivamente_mensagem)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDelete = null
                        viewModel.permanentlyDelete(note)
                    },
                    modifier = Modifier.testTag("confirm_permanent_delete")
                ) {
                    Text(stringResource(R.string.excluir_definitivamente))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    if (showEmptyConfirmation) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmation = false },
            title = { Text(stringResource(R.string.esvaziar_lixeira_titulo)) },
            text = { Text(stringResource(R.string.esvaziar_lixeira_mensagem)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyConfirmation = false
                        viewModel.emptyTrash()
                    },
                    modifier = Modifier.testTag("confirm_empty_trash")
                ) {
                    Text(stringResource(R.string.esvaziar_lixeira))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmation = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    notes: List<Note>,
    onRestore: (Note) -> Unit,
    onPermanentlyDelete: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.lixeira),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("trash_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.voltar)
                        )
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        TextButton(
                            onClick = onEmptyTrash,
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Text(stringResource(R.string.esvaziar_lixeira))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (notes.isEmpty()) {
            EmptyTrashState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    TrashNoteCard(
                        note = note,
                        onRestore = { onRestore(note) },
                        onPermanentlyDelete = { onPermanentlyDelete(note) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashNoteCard(
    note: Note,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    val noteColor = NoteColors.colorOf(note.color)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trash_note_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = if (noteColor != null) {
            CardDefaults.elevatedCardColors(containerColor = noteColor.copy(alpha = 0.07f))
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (noteColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(noteColor)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifEmpty { stringResource(R.string.nota_sem_titulo) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val preview = note.content.trim()
                if (preview.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.excluida_em,
                            formatDate(note.deletedAt ?: note.updatedAt)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onRestore,
                        modifier = Modifier.testTag("restore_note_${note.id}")
                    ) {
                        Text(stringResource(R.string.restaurar))
                    }
                    TextButton(
                        onClick = onPermanentlyDelete,
                        modifier = Modifier.testTag("permanent_delete_note_${note.id}")
                    ) {
                        Text(stringResource(R.string.excluir_definitivamente))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTrashState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.lixeira_vazia),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
