package com.securenotes.app.ui.screens.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.securenotes.app.NotesApplication
import com.securenotes.app.domain.model.Folder
import com.securenotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class FolderUiState(
    val folders: List<Folder> = emptyList()
)

class FolderViewModel(
    private val folderRepository: FolderRepository
) : ViewModel() {

    val uiState: StateFlow<FolderUiState> = folderRepository.observeFolders()
        .map { folders -> FolderUiState(folders = folders) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FolderUiState()
        )

    suspend fun createFolder(name: String): Boolean =
        folderRepository.createFolder(name) != null

    suspend fun deleteFolder(id: Long) {
        folderRepository.deleteFolder(id)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NotesApplication
                FolderViewModel(application.container.folderRepository)
            }
        }
    }
}
