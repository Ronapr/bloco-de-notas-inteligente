package com.securenotes.app.data.repository

import com.securenotes.app.database.dao.NoteDao
import com.securenotes.app.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeNoteDao : NoteDao {

    private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())
    private var nextId = 1L

    override fun observeNotes(): Flow<List<NoteEntity>> =
        notesFlow.map { notes ->
            notes.filterNot { it.deleted }
                .sortedWith(
                    compareByDescending<NoteEntity> { it.pinned }
                        .thenByDescending { it.updatedAt }
                )
        }

    override fun observeFavoriteNotes(): Flow<List<NoteEntity>> =
        notesFlow.map { notes ->
            notes.filterNot { it.deleted }
                .filter { it.favorite }
                .sortedByDescending { it.updatedAt }
        }

    override fun observeFolderNotes(folderId: Long): Flow<List<NoteEntity>> =
        notesFlow.map { notes ->
            notes.filterNot { it.deleted }
                .filter { it.folderId == folderId }
                .sortedByDescending { it.updatedAt }
        }

    override fun observeDeletedNotes(): Flow<List<NoteEntity>> =
        notesFlow.map { notes ->
            notes.filter { it.deleted }.sortedByDescending { it.deletedAt ?: it.updatedAt }
        }

    override fun observeNote(id: Long): Flow<NoteEntity?> =
        notesFlow.map { notes -> notes.find { it.id == id } }

    override suspend fun moveToTrash(id: Long, deletedAt: Long) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == id) current.copy(deleted = true, deletedAt = deletedAt) else current
            }
        }
    }

    override suspend fun restore(id: Long) {
        notesFlow.update { notes ->
            notes.map { current ->
                if (current.id == id) current.copy(deleted = false, deletedAt = null) else current
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

    override suspend fun emptyTrash() {
        notesFlow.update { notes -> notes.filterNot { it.deleted } }
    }

    override suspend fun insert(note: NoteEntity): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        notesFlow.update { notes -> notes + note.copy(id = id) }
        return id
    }

    override suspend fun update(note: NoteEntity) {
        notesFlow.update { notes ->
            notes.map { current -> if (current.id == note.id) note else current }
        }
    }

    override suspend fun delete(note: NoteEntity) {
        notesFlow.update { notes -> notes.filterNot { it.id == note.id } }
    }
}
