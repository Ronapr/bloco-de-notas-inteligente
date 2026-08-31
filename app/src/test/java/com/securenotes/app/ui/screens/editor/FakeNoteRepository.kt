package com.securenotes.app.ui.screens.editor

import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNoteRepository : NoteRepository {

    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1L

    override fun observeNotes(): Flow<List<Note>> =
        notesFlow.map { notes ->
            notes.filterNot { it.deleted }
                .sortedWith(
                    compareByDescending<Note> { it.pinned }
                        .thenByDescending { it.updatedAt }
                )
        }

    override fun observeFavorites(): Flow<List<Note>> =
        notesFlow.map { notes ->
            notes.filterNot { it.deleted }
                .filter { it.favorite }
                .sortedByDescending { it.updatedAt }
        }

    override fun observeFolderNotes(folderId: Long): Flow<List<Note>> =
        notesFlow.map { notes ->
            notes.filterNot { it.deleted }
                .filter { it.folderId == folderId }
                .sortedByDescending { it.updatedAt }
        }

    override fun observeTrash(): Flow<List<Note>> =
        notesFlow.map { notes ->
            notes.filter { it.deleted }.sortedByDescending { it.deletedAt ?: it.updatedAt }
        }

    override fun observeNote(id: Long): Flow<Note?> =
        notesFlow.map { notes -> notes.find { it.id == id } }

    override suspend fun createNote(note: Note): Long {
        val id = nextId++
        notesFlow.update { notes -> notes + note.copy(id = id) }
        return id
    }

    override suspend fun updateNote(note: Note) {
        notesFlow.update { notes ->
            notes.map { current -> if (current.id == note.id) note else current }
        }
    }

    override suspend fun deleteNote(note: Note) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == note.id) {
                    current.copy(deleted = true, deletedAt = System.currentTimeMillis())
                } else {
                    current
                }
            }
        }
    }

    override suspend fun restoreNote(note: Note) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == note.id) current.copy(deleted = false, deletedAt = null) else current
            }
        }
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == id) current.copy(favorite = favorite) else current
            }
        }
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == id) current.copy(pinned = pinned) else current
            }
        }
    }

    override suspend fun setFolder(id: Long, folderId: Long?) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == id) current.copy(folderId = folderId) else current
            }
        }
    }

    override suspend fun permanentlyDeleteNote(note: Note) {
        notesFlow.update { notes -> notes.filterNot { it.id == note.id } }
    }

    override suspend fun emptyTrash() {
        notesFlow.update { notes -> notes.filterNot { it.deleted } }
    }
}
