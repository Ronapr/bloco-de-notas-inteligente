package com.securenotes.app.domain.repository

import com.securenotes.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    fun observeNotes(): Flow<List<Note>>

    fun observeFavorites(): Flow<List<Note>>

    fun observeFolderNotes(folderId: Long): Flow<List<Note>>

    fun observeTrash(): Flow<List<Note>>

    fun observeNote(id: Long): Flow<Note?>

    suspend fun createNote(note: Note): Long

    suspend fun updateNote(note: Note)

    /** Moves the note to the trash without losing its data. */
    suspend fun deleteNote(note: Note)

    suspend fun restoreNote(note: Note)

    suspend fun setFavorite(id: Long, favorite: Boolean)

    suspend fun setPinned(id: Long, pinned: Boolean)

    suspend fun setFolder(id: Long, folderId: Long?)

    suspend fun permanentlyDeleteNote(note: Note)

    suspend fun emptyTrash()
}
