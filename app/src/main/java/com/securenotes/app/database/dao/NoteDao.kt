package com.securenotes.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.securenotes.app.database.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 0 AND favorite = 1 ORDER BY updatedAt DESC")
    fun observeFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 0 AND folderId = :folderId ORDER BY updatedAt DESC")
    fun observeFolderNotes(folderId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleted = 1 ORDER BY deletedAt DESC, updatedAt DESC")
    fun observeDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNote(id: Long): Flow<NoteEntity?>

    @Query("UPDATE notes SET deleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE notes SET deleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("UPDATE notes SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE notes SET folderId = :folderId WHERE id = :id")
    suspend fun setFolder(id: Long, folderId: Long?)

    @Query("DELETE FROM notes WHERE deleted = 1")
    suspend fun emptyTrash()

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)
}
