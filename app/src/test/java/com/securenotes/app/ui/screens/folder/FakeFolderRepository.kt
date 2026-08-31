package com.securenotes.app.ui.screens.folder

import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeFolderRepository : FolderRepository {

    private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())
    private var nextId = 1L

    override fun observeFolders(): Flow<List<Folder>> = foldersFlow

    override suspend fun createFolder(name: String): Long? {
        val normalizedName = name.trim()
        if (normalizedName.isBlank() || foldersFlow.value.any {
                it.name.equals(normalizedName, ignoreCase = true)
            }
        ) {
            return null
        }
        val id = nextId++
        foldersFlow.update { folders -> folders + Folder(id, normalizedName) }
        return id
    }

    override suspend fun deleteFolder(id: Long) {
        foldersFlow.update { folders -> folders.filterNot { it.id == id } }
    }
}
