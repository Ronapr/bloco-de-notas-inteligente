package com.securenotes.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.securenotes.app.NotesApplication
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val reminderAtByNote: Map<Long, Long> = emptyMap(),
    val searchQuery: String = ""
)

class HomeViewModel(
    private val noteRepository: NoteRepository,
    private val favoritesOnly: Boolean = false,
    private val folderId: Long? = null,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<HomeUiState> = combine(
        when {
            folderId != null -> noteRepository.observeFolderNotes(folderId)
            favoritesOnly -> noteRepository.observeFavorites()
            else -> noteRepository.observeNotes()
        },
        reminderRepository.observeNextReminderByNote(),
        searchQuery
    ) { notes, reminderAtByNote, query ->
        HomeUiState(
            notes = if (query.isBlank()) {
                notes
            } else {
                val normalizedQuery = normalize(query)
                notes.filter { note ->
                    normalize(note.title).contains(normalizedQuery) ||
                        normalize(note.content).contains(normalizedQuery)
                }
            },
            reminderAtByNote = reminderAtByNote,
            searchQuery = query
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            noteRepository.setFavorite(note.id, !note.favorite)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            noteRepository.setPinned(note.id, !note.pinned)
        }
    }

    private fun normalize(text: String): String =
        java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")

    companion object {
        fun factory(
            favoritesOnly: Boolean = false,
            folderId: Long? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NotesApplication
                HomeViewModel(
                    application.container.noteRepository,
                    favoritesOnly,
                    folderId,
                    application.container.reminderRepository
                )
            }
        }
    }
}
