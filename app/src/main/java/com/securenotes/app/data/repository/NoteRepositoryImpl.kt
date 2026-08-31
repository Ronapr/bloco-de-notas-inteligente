package com.securenotes.app.data.repository

import com.securenotes.app.database.dao.NoteDao
import com.securenotes.app.database.entity.NoteEntity
import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.repository.NoteRepository
import com.securenotes.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val reminderRepository: ReminderRepository
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeNotes().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override fun observeFavorites(): Flow<List<Note>> =
        noteDao.observeFavoriteNotes().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override fun observeFolderNotes(folderId: Long): Flow<List<Note>> =
        noteDao.observeFolderNotes(folderId).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override fun observeTrash(): Flow<List<Note>> =
        noteDao.observeDeletedNotes().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }

    override fun observeNote(id: Long): Flow<Note?> =
        noteDao.observeNote(id).map { entity -> entity?.toDomain() }

    override suspend fun createNote(note: Note): Long =
        noteDao.insert(note.toEntity())

    override suspend fun updateNote(note: Note) {
        noteDao.update(note.toEntity())
    }

    override suspend fun deleteNote(note: Note) {
        noteDao.moveToTrash(note.id, System.currentTimeMillis())
        reminderRepository.cancelRemindersForNote(note.id)
    }

    override suspend fun restoreNote(note: Note) {
        noteDao.restore(note.id)
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        noteDao.setFavorite(id, favorite)
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        noteDao.setPinned(id, pinned)
    }

    override suspend fun setFolder(id: Long, folderId: Long?) {
        noteDao.setFolder(id, folderId)
    }

    override suspend fun permanentlyDeleteNote(note: Note) {
        noteDao.delete(note.toEntity())
        reminderRepository.cancelRemindersForNote(note.id)
    }

    override suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    private fun NoteEntity.toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        color = color,
        deleted = deleted,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        favorite = favorite,
        folderId = folderId,
        pinned = pinned
    )

    private fun Note.toEntity(): NoteEntity = NoteEntity(
        id = id,
        title = title,
        content = content,
        color = color,
        deleted = deleted,
        deletedAt = deletedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        favorite = favorite,
        folderId = folderId,
        pinned = pinned
    )
}
