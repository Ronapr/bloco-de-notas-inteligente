package com.securenotes.app.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.securenotes.app.NotesApplication
import com.securenotes.app.ai.AiResult
import com.securenotes.app.ai.TokiaAiService
import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.domain.repository.FolderRepository
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.ReminderRepository
import com.securenotes.app.spellcheck.NoOpSpellChecker
import com.securenotes.app.spellcheck.SpellChecker
import com.securenotes.app.spellcheck.SpellingIssue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SpellingStyle = SpanStyle(
    color = Color(0xFFD32F2F),
    textDecoration = TextDecoration.Underline
)

data class EditorUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val color: String = "",
    val favorite: Boolean = false,
    val pinned: Boolean = false,
    val folderId: Long? = null,
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val spellCheckReady: Boolean = false,
    val spellingIssues: List<SpellingIssue> = emptyList(),
    val currentIssue: SpellingIssue? = null,
    val reminders: List<Reminder> = emptyList(),
    val aiLoading: Boolean = false,
    val aiResult: String? = null,
    val aiError: String? = null
)

class EditorViewModel(
    private val noteRepository: NoteRepository,
    noteId: Long,
    private val spellChecker: SpellChecker = NoOpSpellChecker,
    private val scanDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val folderRepository: FolderRepository? = null,
    initialFolderId: Long? = null,
    private val reminderRepository: ReminderRepository? = null,
    private val aiService: TokiaAiService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditorUiState(
            noteId = noteId,
            folderId = initialFolderId,
            isNew = noteId == 0L,
            isLoading = noteId != 0L
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _contentField = MutableStateFlow(TextFieldValue(""))
    val contentField: StateFlow<TextFieldValue> = _contentField.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private var createdAt: Long = 0L
    private var spellCheckJob: Job? = null
    private var dirty = false

    private var originalTitle: String = ""
    private var originalContent: String = ""

    fun hasChanges(): Boolean {
        val state = _uiState.value
        return state.title != originalTitle || state.content != originalContent
    }

    fun restoreOriginal() {
        dirty = false
        _contentField.value = TextFieldValue(AnnotatedString(originalContent))
        _uiState.update { it.copy(title = originalTitle, content = originalContent) }
    }

    // Escopo independente para salvar mesmo após onCleared() (saída da tela)
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        folderRepository?.let { repository ->
            viewModelScope.launch {
                repository.observeFolders().collect { folders ->
                    _folders.value = folders
                    _uiState.update { state ->
                        if (state.folderId != null && folders.none { it.id == state.folderId }) {
                            state.copy(folderId = null)
                        } else {
                            state
                        }
                    }
                }
            }
        }
        if (noteId != 0L) {
            viewModelScope.launch {
                val note = noteRepository.observeNote(noteId).first()
                if (note != null) {
                    createdAt = note.createdAt
                    originalTitle = note.title
                    originalContent = note.content
                    _contentField.value = TextFieldValue(AnnotatedString(note.content))
                    _uiState.update {
                        it.copy(
                            noteId = note.id,
                            title = note.title,
                            content = note.content,
                            color = note.color,
                            favorite = note.favorite,
                            pinned = note.pinned,
                            folderId = note.folderId,
                            isNew = false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = EditorUiState(noteId = 0L, isNew = true, isLoading = false)
                }
                initSpellCheck()
            }
            reminderRepository?.let { repository ->
                viewModelScope.launch {
                    repository.observeRemindersForNote(noteId).collect { reminders ->
                        _uiState.update { it.copy(reminders = reminders) }
                    }
                }
            }
        } else {
            initSpellCheck()
        }
    }

    fun onTitleChange(title: String) {
        dirty = true
        _uiState.update { it.copy(title = title) }
    }

    fun onContentChange(content: String) {
        dirty = true
        applyText(content)
    }

    fun onColorChange(color: String) {
        dirty = true
        _uiState.update { it.copy(color = color) }
    }

    fun onFavoriteToggle() {
        dirty = true
        _uiState.update { it.copy(favorite = !it.favorite) }
    }

    fun onPinToggle() {
        dirty = true
        _uiState.update { it.copy(pinned = !it.pinned) }
    }

    fun onFolderChange(folderId: Long?) {
        dirty = true
        _uiState.update { it.copy(folderId = folderId) }
    }

    fun addReminder(reminderAt: Long) {
        val noteId = _uiState.value.noteId
        if (noteId == 0L) return
        viewModelScope.launch {
            reminderRepository?.createReminder(noteId, reminderAt)
        }
    }

    fun updateReminder(reminderId: Long, reminderAt: Long) {
        viewModelScope.launch {
            reminderRepository?.updateReminderTime(reminderId, reminderAt)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository?.deleteReminder(reminderId)
        }
    }

    private var lastKnownCursorPos = 0

    fun onContentFieldChange(value: TextFieldValue) {
        val previous = _contentField.value
        if (value.text == previous.text) {
            lastKnownCursorPos = value.selection.start
            _contentField.value = value
        } else {
            dirty = true
            lastKnownCursorPos = value.selection.start
            _contentField.value = value
            _uiState.update { state ->
                state.copy(content = value.text)
            }
            scheduleSpellCheck()
        }
    }

    fun acceptSuggestion(word: String, suggestion: String) {
        val issue = _uiState.value.currentIssue ?: return
        if (!issue.word.equals(word, ignoreCase = true)) return
        val text = _uiState.value.content
        if (issue.offset > text.length || issue.offset + issue.length > text.length) return
        val newText = text.replaceRange(issue.offset, issue.offset + issue.length, suggestion)
        val newSelection = TextRange(issue.offset + suggestion.length)
        _contentField.value = _contentField.value.copy(
            annotatedString = AnnotatedString(text = newText),
            selection = newSelection,
            composition = null
        )
        _uiState.update {
            it.copy(content = newText, spellingIssues = emptyList(), currentIssue = null)
        }
        scheduleSpellCheck(immediate = true)
    }

    fun ignoreWord(word: String) {
        addWordToDictionary(word)
    }

    fun addToDictionary(word: String) {
        addWordToDictionary(word)
    }

    fun dismissCurrentIssue() {
        _uiState.update { it.copy(currentIssue = null) }
    }

    fun nextIssue() {
        val issues = _uiState.value.spellingIssues
        if (issues.isEmpty()) return
        val current = _uiState.value.currentIssue
        val index = if (current == null) {
            0
        } else {
            val found = issues.indexOfFirst {
                it.offset == current.offset && it.length == current.length
            }
            if (found < 0) 0 else (found + 1) % issues.size
        }
        val next = issues[index]
        _contentField.update { field ->
            field.copy(
                selection = TextRange(next.offset, next.offset + next.length),
                composition = null
            )
        }
        _uiState.update { it.copy(currentIssue = next) }
    }

    /**
     * Salva a nota atual. Idempotente: após criar uma nota nova, o estado
     * passa a apontar para o id criado, então salvamentos seguintes atualizam
     * a mesma nota (nunca duplicam).
     */
    suspend fun saveNote() {
        val state = _uiState.value
        if (!dirty) return
        if (state.title.isBlank() && state.content.isBlank()) return
        val now = System.currentTimeMillis()
        if (state.isNew) {
            val newId = noteRepository.createNote(
                Note(
                    title = state.title.trim(),
                    content = state.content,
                    color = state.color,
                    favorite = state.favorite,
                    pinned = state.pinned,
                    folderId = state.folderId,
                    createdAt = now,
                    updatedAt = now
                )
            )
            createdAt = now
            _uiState.update { it.copy(noteId = newId, isNew = false) }
        } else {
            noteRepository.updateNote(
                Note(
                    id = state.noteId,
                    title = state.title.trim(),
                    content = state.content,
                    color = state.color,
                    favorite = state.favorite,
                    pinned = state.pinned,
                    folderId = state.folderId,
                    createdAt = createdAt,
                    updatedAt = now
                )
            )
        }
        dirty = false
        originalTitle = _uiState.value.title
        originalContent = _uiState.value.content
    }

    fun save(onComplete: () -> Unit) {
        viewModelScope.launch {
            saveNote()
            onComplete()
        }
    }

    /** Salvamento em segundo plano (usado no ON_STOP e ao sair da tela). */
    fun saveAsync() {
        viewModelScope.launch {
            saveNote()
        }
    }

    override fun onCleared() {
        // Garante o salvamento automático ao sair do editor por qualquer via
        // (seta de voltar, gesto de sistema, navegação). viewModelScope já foi
        // cancelado aqui, por isso usamos o saveScope independente.
        if (dirty) {
            val state = _uiState.value
            if (state.title.isNotBlank() || state.content.isNotBlank()) {
                saveScope.launch {
                    try {
                        saveNote()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        super.onCleared()
    }

    fun delete(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isNew) {
                noteRepository.deleteNote(Note(id = state.noteId))
            }
            onComplete()
        }
    }

    private fun initSpellCheck() {
        viewModelScope.launch {
            val ready = spellChecker.awaitReady()
            _uiState.update { it.copy(spellCheckReady = ready) }
            if (ready) {
                scheduleSpellCheck(immediate = true)
            }
        }
    }

    private fun addWordToDictionary(word: String) {
        spellChecker.addToDictionary(word)
        _uiState.update {
            it.copy(
                spellingIssues = it.spellingIssues.filterNot { issue ->
                    issue.word.equals(word, ignoreCase = true)
                },
                currentIssue = null
            )
        }
        scheduleSpellCheck(immediate = true)
    }

    private fun scheduleSpellCheck(immediate: Boolean = false) {
        if (!_uiState.value.spellCheckReady) return
        spellCheckJob?.cancel()
        spellCheckJob = viewModelScope.launch {
            if (!immediate) delay(SPELL_DEBOUNCE_MS)
            val text = _uiState.value.content
            if (text.isEmpty()) {
                _uiState.update {
                    it.copy(spellingIssues = emptyList(), currentIssue = null)
                }
                return@launch
            }
            val issues = withContext(scanDispatcher) { spellChecker.scan(text) }
            val cursorPos = lastKnownCursorPos.coerceIn(0, text.length)
            _uiState.update {
                it.copy(
                    spellingIssues = issues,
                    currentIssue = currentIssueFor(TextRange(cursorPos), issues)
                )
            }
        }
    }

    private fun applyText(text: String) {
        val selection = _contentField.value.selection
        val clamped = TextRange(
            start = selection.start.coerceIn(0, text.length),
            end = selection.end.coerceIn(0, text.length)
        )
        _contentField.value = _contentField.value.copy(
            annotatedString = AnnotatedString(text = text),
            selection = clamped,
            composition = null
        )
        _uiState.update { state ->
            state.copy(content = text, currentIssue = currentIssueFor(clamped, state.spellingIssues))
        }
        scheduleSpellCheck(immediate = true)
    }

    private fun fieldValue(
        text: String,
        selection: TextRange = TextRange.Zero,
        composition: TextRange? = null,
        issues: List<SpellingIssue> = emptyList()
    ): TextFieldValue = TextFieldValue(
        annotatedString = AnnotatedString(text = text, spanStyles = styleRanges(issues)),
        selection = selection,
        composition = composition
    )

    private fun styleRanges(issues: List<SpellingIssue>): List<AnnotatedString.Range<SpanStyle>> =
        issues.map { AnnotatedString.Range(SpellingStyle, it.offset, it.offset + it.length) }

    private fun currentIssueFor(selection: TextRange, issues: List<SpellingIssue>): SpellingIssue? {
        if (issues.isEmpty()) return null
        val cursor = if (selection.collapsed) selection.start else minOf(selection.start, selection.end)
        return issues.firstOrNull { it.contains(cursor) }
    }

    fun improveText() {
        val text = _uiState.value.content
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(aiLoading = true, aiError = null, aiResult = null) }
            val result = aiService?.completeText(text)
            when (result) {
                is AiResult.Success -> {
                    _uiState.update { it.copy(aiLoading = false, aiResult = result.content) }
                }
                is AiResult.Error -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = result.message) }
                }
                null -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = "Serviço de IA não disponível") }
                }
            }
        }
    }

    fun summarizeText() {
        val text = _uiState.value.content
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(aiLoading = true, aiError = null, aiResult = null) }
            val result = aiService?.summarize(text)
            when (result) {
                is AiResult.Success -> {
                    _uiState.update { it.copy(aiLoading = false, aiResult = result.content) }
                }
                is AiResult.Error -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = result.message) }
                }
                null -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = "Serviço de IA não disponível") }
                }
            }
        }
    }

    fun correctText() {
        val text = _uiState.value.content
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(aiLoading = true, aiError = null, aiResult = null) }
            val result = aiService?.correctSpelling(text)
            when (result) {
                is AiResult.Success -> {
                    _uiState.update { it.copy(aiLoading = false, aiResult = result.content) }
                }
                is AiResult.Error -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = result.message) }
                }
                null -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = "Serviço de IA não disponível") }
                }
            }
        }
    }

    fun generateTitleFromContent() {
        val text = _uiState.value.content
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(aiLoading = true, aiError = null, aiResult = null) }
            val result = aiService?.generateTitle(text)
            when (result) {
                is AiResult.Success -> {
                    val newTitle = result.content.trim().removeSurrounding("\"")
                    onTitleChange(newTitle)
                    _uiState.update { it.copy(aiLoading = false, aiResult = null) }
                }
                is AiResult.Error -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = result.message) }
                }
                null -> {
                    _uiState.update { it.copy(aiLoading = false, aiError = "Serviço de IA não disponível") }
                }
            }
        }
    }

    fun applyAiResult() {
        val result = _uiState.value.aiResult ?: return
        applyText(result)
        _uiState.update { it.copy(aiResult = null) }
    }

    fun dismissAiResult() {
        _uiState.update { it.copy(aiResult = null, aiError = null) }
    }

    companion object {
        private const val SPELL_DEBOUNCE_MS = 300L

        fun factory(noteId: Long, initialFolderId: Long? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NotesApplication
                EditorViewModel(
                    noteRepository = application.container.noteRepository,
                    noteId = noteId,
                    initialFolderId = initialFolderId,
                    spellChecker = application.container.spellCheckManager,
                    folderRepository = application.container.folderRepository,
                    reminderRepository = application.container.reminderRepository,
                    aiService = application.container.aiService
                )
            }
        }
    }
}
