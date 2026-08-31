package com.securenotes.app.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.securenotes.app.NotesApplication
import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList()
)

class RemindersViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val uiState: StateFlow<RemindersUiState> = reminderRepository.observeReminders()
        .map { reminders -> RemindersUiState(reminders = reminders) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RemindersUiState()
        )

    init {
        viewModelScope.launch {
            reminderRepository.reconcileOverdue()
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminderId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as NotesApplication
                RemindersViewModel(application.container.reminderRepository)
            }
        }
    }
}
