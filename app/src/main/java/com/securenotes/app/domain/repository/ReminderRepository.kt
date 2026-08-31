package com.securenotes.app.domain.repository

import com.securenotes.app.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    fun observeReminders(): Flow<List<Reminder>>

    fun observeRemindersForNote(noteId: Long): Flow<List<Reminder>>

    fun observeNextReminderByNote(): Flow<Map<Long, Long>>

    suspend fun createReminder(noteId: Long, reminderAt: Long): Long

    suspend fun updateReminderTime(reminderId: Long, reminderAt: Long)

    suspend fun deleteReminder(reminderId: Long)

    /** Deletes reminders of the note and cancels their alarms (note moved to trash). */
    suspend fun cancelRemindersForNote(noteId: Long)

    /** Marks the reminder as shown and posts its notification (alarm fired). */
    suspend fun onReminderFired(reminderId: Long)

    /** Marks overdue unshown reminders as shown and notifies them (device off / force stop). */
    suspend fun reconcileOverdue()

    /**
     * Re-registra os alarmes de todos os lembretes futuros não exibidos e
     * notifica os atrasados. Deve ser chamado ao iniciar o app e após reboot,
     * pois atualizações do app e reinícios do aparelho apagam os alarmes.
     */
    suspend fun rescheduleAll()
}
