package com.securenotes.app.data.repository

import com.securenotes.app.database.dao.FolderDao
import com.securenotes.app.database.dao.FolderWithCount
import com.securenotes.app.database.entity.FolderEntity
import com.securenotes.app.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeFolderDao : FolderDao {

    private val foldersFlow = MutableStateFlow<List<FolderEntity>>(emptyList())
    private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())
    private var nextId = 1L

    override fun observeFolders(): Flow<List<FolderWithCount>> =
        foldersFlow.map { folders ->
            folders.map { folder ->
                FolderWithCount(
                    id = folder.id,
                    name = folder.name,
                    noteCount = notesFlow.value.count {
                        !it.deleted && it.folderId == folder.id
                    }
                )
            }
        }

    override suspend fun findByName(name: String): FolderEntity? =
        foldersFlow.value.firstOrNull { it.name.equals(name, ignoreCase = true) }

    override suspend fun insert(folder: FolderEntity): Long {
        val id = if (folder.id == 0L) nextId++ else folder.id
        foldersFlow.update { folders -> folders + folder.copy(id = id) }
        return id
    }

    override suspend fun clearNotesFromFolder(folderId: Long) {
        notesFlow.update { notes ->
            notes.map { note ->
                if (note.folderId == folderId) note.copy(folderId = null) else note
            }
        }
    }

    override suspend fun deleteById(folderId: Long) {
        foldersFlow.update { folders -> folders.filterNot { it.id == folderId } }
    }

    override suspend fun deleteFolder(folderId: Long) {
        clearNotesFromFolder(folderId)
        deleteById(folderId)
    }

    suspend fun seedNote(note: NoteEntity) {
        notesFlow.update { notes -> notes + note }
    }

    fun note(id: Long): NoteEntity? = notesFlow.value.firstOrNull { it.id == id }
}
