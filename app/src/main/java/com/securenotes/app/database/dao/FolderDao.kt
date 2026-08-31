package com.securenotes.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.securenotes.app.database.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query(
        "SELECT folders.id AS id, folders.name AS name, COUNT(notes.id) AS noteCount " +
            "FROM folders LEFT JOIN notes " +
            "ON notes.folderId = folders.id AND notes.deleted = 0 " +
            "GROUP BY folders.id, folders.name ORDER BY folders.id ASC"
    )
    fun observeFolders(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearNotesFromFolder(folderId: Long)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteById(folderId: Long)

    @Transaction
    suspend fun deleteFolder(folderId: Long) {
        clearNotesFromFolder(folderId)
        deleteById(folderId)
    }
}
