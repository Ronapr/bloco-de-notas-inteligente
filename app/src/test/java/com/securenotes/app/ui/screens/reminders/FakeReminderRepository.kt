package com.securenotes.app.ui.screens.reminders

import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReminderRepository : ReminderRepository {

    private val remindersFlow = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1L
    var reconcileCount = 0

    override fun observeReminders(): Flow<List<Reminder>> =
        remindersFlow.map { list ->
            list.sortedWith(compareBy({ it.shown }, { it.reminderAt }))
        }

    override fun observeRemindersForNote(noteId: Long): Flow<List<Reminder>> =
        remindersFlow.map { list ->
            list.filter { it.noteId == noteId }.sortedBy { it.reminderAt }
        }

    override fun observeNextReminderByNote(): Flow<Map<Long, Long>> =
        remindersFlow.map { list ->
            list.filter { !it.shown }
                .groupBy { it.noteId }
                .mapValues { (_, reminders) -> reminders.minOf { it.reminderAt } }
        }

    override suspend fun createReminder(noteId: Long, reminderAt: Long): Long {
        val id = nextId++
        remindersFlow.update {
            it + Reminder(id = id, noteId = noteId, reminderAt = reminderAt)
        }
        return id
    }

    override suspend fun updateReminderTime(reminderId: Long, reminderAt: Long) {
        remindersFlow.update { list ->
            list.map {
                if (it.id == reminderId) it.copy(reminderAt = reminderAt) else it
            }
        }
    }

    override suspend fun deleteReminder(reminderId: Long) {
        remindersFlow.update { list -> list.filterNot { it.id == reminderId } }
    }

    override suspend fun cancelRemindersForNote(noteId: Long) {
        remindersFlow.update { list -> list.filterNot { it.noteId == noteId } }
    }

    override suspend fun onReminderFired(reminderId: Long) {
        remindersFlow.update { list ->
            list.map { if (it.id == reminderId) it.copy(shown = true) else it }
        }
    }

    override suspend fun reconcileOverdue() {
        reconcileCount++
        remindersFlow.update { list ->
            list.map {
                if (!it.shown && it.reminderAt <= System.currentTimeMillis()) {
                    it.copy(shown = true)
                } else {
                    it
                }
            }
        }
    }
}
