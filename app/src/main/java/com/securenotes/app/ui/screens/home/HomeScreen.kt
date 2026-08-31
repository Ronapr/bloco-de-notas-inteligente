package com.securenotes.app.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securenotes.app.R
import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.model.Note
import com.securenotes.app.ui.NoteColors
import com.securenotes.app.ui.screens.folder.FolderViewModel
import com.securenotes.app.ui.theme.SecureNotesTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoute(
    folderId: Long? = null,
    favoritesOnly: Boolean = false,
    viewModel: HomeViewModel = viewModel(
        key = when {
            folderId != null -> "home_folder_$folderId"
            favoritesOnly -> "home_favorites"
            else -> "home_all"
        },
        factory = HomeViewModel.factory(favoritesOnly, folderId)
    ),
    folderViewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory),
    onNoteClick: (Long) -> Unit,
    onNewNote: (Long?) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenMyNotes: () -> Unit = {},
    onOpenFolder: (Long) -> Unit = {},
    onOpenReminders: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val folderUiState by folderViewModel.uiState.collectAsState()
    val folderName = folderUiState.folders.firstOrNull { it.id == folderId }?.name
    HomeScreen(
        notes = uiState.notes,
        reminderAtByNote = uiState.reminderAtByNote,
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        folderId = folderId,
        folderName = folderName,
        folders = folderUiState.folders,
        favoritesOnly = favoritesOnly,
        onNoteClick = onNoteClick,
        onNewNote = onNewNote,
        onToggleFavorite = viewModel::toggleFavorite,
        onTogglePin = viewModel::togglePin,
        onOpenSettings = onOpenSettings,
        onOpenTrash = onOpenTrash,
        onOpenFavorites = onOpenFavorites,
        onOpenMyNotes = onOpenMyNotes,
        onOpenFolder = onOpenFolder,
        onOpenReminders = onOpenReminders,
        onCreateFolder = folderViewModel::createFolder,
        onDeleteFolder = folderViewModel::deleteFolder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    reminderAtByNote: Map<Long, Long> = emptyMap(),
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    folderId: Long? = null,
    folderName: String? = null,
    folders: List<Folder> = emptyList(),
    favoritesOnly: Boolean = false,
    onNoteClick: (Long) -> Unit,
    onNewNote: (Long?) -> Unit,
    onToggleFavorite: (Note) -> Unit = {},
    onTogglePin: (Note) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenTrash: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenMyNotes: () -> Unit = {},
    onOpenFolder: (Long) -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onCreateFolder: suspend (String) -> Boolean = { false },
    onDeleteFolder: suspend (Long) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    var createFolderError by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.minhas_notas)) },
                    selected = !favoritesOnly,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            if (favoritesOnly) onOpenMyNotes()
                        }
                    },
                    modifier = Modifier.testTag("drawer_my_notes")
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.favoritos)) },
                    selected = favoritesOnly,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (favoritesOnly) {
                                Color(0xFFFFC107)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            if (!favoritesOnly) onOpenFavorites()
                        }
                    },
                    modifier = Modifier.testTag("drawer_favorites")
                )
                folders.forEach { folder ->
                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = folder.noteCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { folderToDelete = folder },
                                    modifier = Modifier.testTag("delete_folder_${folder.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(
                                            R.string.excluir_pasta_acessibilidade,
                                            folder.name
                                        )
                                    )
                                }
                            }
                        },
                        selected = folder.id == folderId,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                if (folder.id != folderId) onOpenFolder(folder.id)
                            }
                        },
                        modifier = Modifier.testTag("drawer_folder_${folder.id}")
                    )
                }
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.nova_pasta)) },
                    selected = false,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        scope.launch { drawerState.close() }
                        folderNameInput = ""
                        createFolderError = false
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier.testTag("drawer_new_folder")
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.lembretes)) },
                    selected = false,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onOpenReminders()
                        }
                    },
                    modifier = Modifier.testTag("drawer_reminders")
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.lixeira)) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onOpenTrash()
                        }
                    },
                    modifier = Modifier.testTag("drawer_trash")
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.configuracoes)) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onOpenSettings()
                        }
                    },
                    modifier = Modifier.testTag("drawer_settings")
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when {
                                favoritesOnly -> stringResource(R.string.favoritos)
                                folderId != null -> folderName ?: stringResource(R.string.pasta)
                                else -> stringResource(R.string.app_name)
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.abrir_menu)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("settings_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.configuracoes)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { onNewNote(folderId) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.nova_nota)
                        )
                    },
                    text = { Text(stringResource(R.string.nova_nota)) },
                    modifier = Modifier.testTag("fab_new_note")
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(stringResource(R.string.buscar_notas)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.limpar_busca)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_field")
                )
                if (notes.isEmpty()) {
                    EmptyState(
                        favoritesOnly = favoritesOnly,
                        folderOnly = folderId != null,
                        searchActive = searchQuery.isNotBlank(),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                reminderAt = reminderAtByNote[note.id],
                                onClick = { onNoteClick(note.id) },
                                onToggleFavorite = onToggleFavorite,
                                onTogglePin = onTogglePin
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                createFolderError = false
            },
            title = { Text(stringResource(R.string.nova_pasta)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = {
                            folderNameInput = it
                            createFolderError = false
                        },
                        label = { Text(stringResource(R.string.nome_da_pasta)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (createFolderError) {
                        Text(
                            text = stringResource(R.string.pasta_ja_existe),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderNameInput.isBlank()) {
                            createFolderError = true
                        } else {
                            scope.launch {
                                if (onCreateFolder(folderNameInput)) {
                                    showCreateFolderDialog = false
                                    folderNameInput = ""
                                    createFolderError = false
                                } else {
                                    createFolderError = true
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.criar_pasta))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    val folderBeingDeleted = folderToDelete
    if (folderBeingDeleted != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(stringResource(R.string.excluir_pasta_titulo)) },
            text = {
                Text(
                    stringResource(
                        R.string.excluir_pasta_mensagem,
                        folderBeingDeleted.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            onDeleteFolder(folderBeingDeleted.id)
                            folderToDelete = null
                            if (folderBeingDeleted.id == folderId) onOpenMyNotes()
                        }
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
private fun NoteCard(
    note: Note,
    reminderAt: Long?,
    onClick: () -> Unit,
    onToggleFavorite: (Note) -> Unit,
    onTogglePin: (Note) -> Unit
) {
    val noteColor = NoteColors.colorOf(note.color)
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_item"),
        shape = RoundedCornerShape(16.dp),
        colors = if (noteColor != null) {
            CardDefaults.elevatedCardColors(
                containerColor = noteColor.copy(alpha = 0.07f)
            )
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            if (noteColor != null) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp))
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onTogglePin(note) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("pin_${note.id}")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin),
                            contentDescription = stringResource(
                                if (note.pinned) R.string.desafixar_nota
                                else R.string.fixar_nota
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = if (note.pinned) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(note) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("favorite_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(
                                if (note.favorite) R.string.desmarcar_como_favorita
                                else R.string.marcar_como_favorita
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = if (note.favorite) {
                                Color(0xFFFFC107)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
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
                        text = stringResource(R.string.atualizado_em, formatDate(note.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (reminderAt != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.tem_lembrete),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(reminderAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    favoritesOnly: Boolean = false,
    folderOnly: Boolean = false,
    searchActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    searchActive -> Icons.Filled.Search
                    favoritesOnly -> Icons.Filled.Star
                    else -> Icons.Filled.Edit
                },
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(
                when {
                    searchActive -> R.string.nenhuma_nota_encontrada
                    favoritesOnly -> R.string.sem_notas_favoritas
                    folderOnly -> R.string.pasta_vazia
                    else -> R.string.lista_vazia
                }
            ),
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    SecureNotesTheme {
        HomeScreen(notes = emptyList(), onNoteClick = {}, onNewNote = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithNotesPreview() {
    SecureNotesTheme {
        HomeScreen(
            notes = listOf(
                Note(
                    id = 1,
                    title = "ReuniÃ£o de segunda-feira",
                    content = "Preparar a apresentaÃ§Ã£o do projeto e revisar o cronograma com a equipe.",
                    createdAt = 0L,
                    updatedAt = 1755100000000L
                ),
                Note(
                    id = 2,
                    title = "Lista de compras",
                    content = "PÃ£o, cafÃ©, frutas e legumes.",
                    createdAt = 0L,
                    updatedAt = 1755000000000L
                )
            ),
            onNoteClick = {},
            onNewNote = {},
            onOpenSettings = {}
        )
    }
}

