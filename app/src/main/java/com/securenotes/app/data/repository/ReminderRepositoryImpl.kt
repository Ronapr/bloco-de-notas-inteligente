package com.securenotes.app.data.repository

import com.securenotes.app.database.dao.NoteReminderAt
import com.securenotes.app.database.dao.ReminderDao
import com.securenotes.app.database.dao.ReminderWithNoteEntity
import com.securenotes.app.database.entity.ReminderEntity
import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.domain.repository.ReminderRepository
import com.securenotes.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        reminderDao.observeReminders().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeRemindersForNote(noteId: Long): Flow<List<Reminder>> =
        reminderDao.observeRemindersForNote(noteId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeNextReminderByNote(): Flow<Map<Long, Long>> =
        reminderDao.observeNextReminderByNote().map { list ->
            list.associate { item -> item.noteId to item.reminderAt }
        }

    override suspend fun createReminder(noteId: Long, reminderAt: Long): Long {
        val id = reminderDao.insert(
            ReminderEntity(
                noteId = noteId,
                reminderAt = reminderAt,
                createdAt = System.currentTimeMillis()
            )
        )
        scheduler.schedule(id, reminderAt)
        return id
    }

    override suspend fun updateReminderTime(reminderId: Long, reminderAt: Long) {
        reminderDao.updateTime(reminderId, reminderAt)
        scheduler.cancel(reminderId)
        scheduler.schedule(reminderId, reminderAt)
    }

    override suspend fun deleteReminder(reminderId: Long) {
        reminderDao.delete(ReminderEntity(id = reminderId))
        scheduler.cancel(reminderId)
    }

    override suspend fun cancelRemindersForNote(noteId: Long) {
        val ids = reminderDao.idsByNote(noteId)
        reminderDao.deleteByNoteId(noteId)
        ids.forEach { scheduler.cancel(it) }
    }

    override suspend fun onReminderFired(reminderId: Long) {
        val reminder = reminderDao.findById(reminderId) ?: return
        if (!reminder.shown) {
            reminderDao.markShown(reminderId)
            scheduler.postNotification(reminder.toDomain())
        }
    }

    override suspend fun reconcileOverdue() {
        val overdue = reminderDao.findOverdueUnshown(System.currentTimeMillis())
        overdue.forEach { reminder ->
            reminderDao.markShown(reminder.id)
            scheduler.postNotification(reminder.toDomain())
        }
    }

    override suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        reminderDao.findUnshown().forEach { reminder ->
            if (reminder.reminderAt <= now) {
                reminderDao.markShown(reminder.id)
                scheduler.postNotification(reminder.toDomain())
            } else {
                scheduler.schedule(reminder.id, reminder.reminderAt)
            }
        }
    }

    private fun ReminderWithNoteEntity.toDomain(): Reminder = Reminder(
        id = id,
        noteId = noteId,
        noteTitle = noteTitle,
        reminderAt = reminderAt,
        shown = shown,
        createdAt = createdAt
    )
}
