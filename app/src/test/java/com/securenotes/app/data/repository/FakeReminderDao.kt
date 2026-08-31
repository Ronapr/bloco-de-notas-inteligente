package com.securenotes.app.data.repository

import com.securenotes.app.database.dao.NoteReminderAt
import com.securenotes.app.database.dao.ReminderDao
import com.securenotes.app.database.dao.ReminderWithNoteEntity
import com.securenotes.app.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReminderDao : ReminderDao {

    private val remindersFlow = MutableStateFlow<List<ReminderEntity>>(emptyList())
    private val noteTitles = MutableStateFlow<Map<Long, String>>(emptyMap())
    private var nextId = 1L

    fun seedNoteTitle(noteId: Long, title: String) {
        noteTitles.update { it + (noteId to title) }
    }

    private fun ReminderEntity.withTitle(): ReminderWithNoteEntity = ReminderWithNoteEntity(
        id = id,
        noteId = noteId,
        noteTitle = noteTitles.value[noteId] ?: "",
        reminderAt = reminderAt,
        shown = shown,
        createdAt = createdAt
    )

    override fun observeReminders(): Flow<List<ReminderWithNoteEntity>> =
        remindersFlow.map { list ->
            list.map { it.withTitle() }
                .sortedWith(compareBy({ it.shown }, { it.reminderAt }))
        }

    override fun observeRemindersForNote(noteId: Long): Flow<List<ReminderWithNoteEntity>> =
        remindersFlow.map { list ->
            list.filter { it.noteId == noteId }
                .map { it.withTitle() }
                .sortedBy { it.reminderAt }
        }

    override fun observeNextReminderByNote(): Flow<List<NoteReminderAt>> =
        remindersFlow.map { list ->
            list.filter { !it.shown }
                .groupBy { it.noteId }
                .map { (noteId, reminders) ->
                    NoteReminderAt(noteId, reminders.minOf { it.reminderAt })
                }
        }

    override suspend fun findById(id: Long): ReminderWithNoteEntity? =
        remindersFlow.value.firstOrNull { it.id == id }?.withTitle()

    override suspend fun findOverdueUnshown(now: Long): List<ReminderWithNoteEntity> =
        remindersFlow.value.filter { !it.shown && it.reminderAt <= now }
            .map { it.withTitle() }

    override suspend fun markShown(id: Long) {
        remindersFlow.update { list ->
            list.map { if (it.id == id) it.copy(shown = true) else it }
        }
    }

    override suspend fun updateTime(id: Long, reminderAt: Long) {
        remindersFlow.update { list ->
            list.map { if (it.id == id) it.copy(reminderAt = reminderAt) else it }
        }
    }

    override suspend fun idsByNote(noteId: Long): List<Long> =
        remindersFlow.value.filter { it.noteId == noteId }.map { it.id }

    override suspend fun deleteByNoteId(noteId: Long) {
        remindersFlow.update { list -> list.filterNot { it.noteId == noteId } }
    }

    override suspend fun insert(reminder: ReminderEntity): Long {
        val id = if (reminder.id == 0L) nextId++ else reminder.id
        remindersFlow.update { list -> list + reminder.copy(id = id) }
        return id
    }

    override suspend fun delete(reminder: ReminderEntity) {
        remindersFlow.update { list -> list.filterNot { it.id == reminder.id } }
    }
}
