package com.securenotes.app.domain.repository

import com.securenotes.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {

    fun observeFolders(): Flow<List<Folder>>

    suspend fun createFolder(name: String): Long?

    suspend fun deleteFolder(id: Long)
}
