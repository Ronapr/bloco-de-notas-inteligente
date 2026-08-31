package com.securenotes.app.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.securenotes.app.NotesApplication
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val notes: List<Note> = emptyList()
)

class TrashViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = noteRepository.observeTrash()
        .map { notes -> TrashUiState(notes = notes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrashUiState()
        )

    fun restore(note: Note) {
        viewModelScope.launch {
            noteRepository.restoreNote(note)
        }
    }

    fun permanentlyDelete(note: Note) {
        viewModelScope.launch {
            noteRepository.permanentlyDeleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NotesApplication
                TrashViewModel(application.container.noteRepository)
            }
        }
    }
}
