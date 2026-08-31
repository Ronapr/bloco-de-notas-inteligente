package com.securenotes.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.securenotes.app.database.dao.FolderDao
import com.securenotes.app.database.entity.FolderEntity
import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepositoryImpl(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeFolders().map { folders ->
            folders.map { folder ->
                Folder(
                    id = folder.id,
                    name = folder.name,
                    noteCount = folder.noteCount
                )
            }
        }

    override suspend fun createFolder(name: String): Long? {
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || folderDao.findByName(normalizedName) != null) {
            return null
        }
        return try {
            folderDao.insert(FolderEntity(name = normalizedName))
        } catch (_: SQLiteConstraintException) {
            null
        }
    }

    override suspend fun deleteFolder(id: Long) {
        folderDao.deleteFolder(id)
    }
}
