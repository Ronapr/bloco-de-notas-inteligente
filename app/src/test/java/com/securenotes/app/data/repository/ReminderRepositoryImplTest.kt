package com.securenotes.app.data.repository

import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.repository.ReminderRepository
import com.securenotes.app.test.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderRepositoryImplTest {

    private lateinit var dao: FakeReminderDao
    private lateinit var scheduler: FakeReminderScheduler
    private lateinit var repository: ReminderRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeReminderDao()
        scheduler = FakeReminderScheduler()
        repository = ReminderRepositoryImpl(dao, scheduler)
    }

    @Test
    fun `createReminder persists and schedules`() = runBlocking {
        dao.seedNoteTitle(1L, "Nota")

        val id = repository.createReminder(noteId = 1L, reminderAt = 1000L)

        assertEquals(1L, id)
        assertEquals(1L to 1000L, scheduler.scheduled.single())
        assertEquals(1L, repository.observeReminders().first().single().noteId)
        assertEquals("Nota", repository.observeReminders().first().single().noteTitle)
    }

    @Test
    fun `updateReminderTime reschedules`() = runBlocking {
        val id = repository.createReminder(noteId = 1L, reminderAt = 1000L)

        repository.updateReminderTime(id, 2000L)

        assertEquals(listOf(1L), scheduler.cancelled)
        assertEquals(2000L, repository.observeReminders().first().single().reminderAt)
        assertEquals(1L to 2000L, scheduler.scheduled.last())
    }

    @Test
    fun `deleteReminder removes and cancels`() = runBlocking {
        val id = repository.createReminder(noteId = 1L, reminderAt = 1000L)

        repository.deleteReminder(id)

        assertTrue(repository.observeReminders().first().isEmpty())
        assertEquals(listOf(1L), scheduler.cancelled)
    }

    @Test
    fun `cancelRemindersForNote deletes and cancels all reminders of the note`() = runBlocking {
        repository.createReminder(noteId = 1L, reminderAt = 1000L)
        repository.createReminder(noteId = 1L, reminderAt = 2000L)
        repository.createReminder(noteId = 2L, reminderAt = 3000L)

        repository.cancelRemindersForNote(1L)

        assertEquals(listOf(1L, 2L), scheduler.cancelled)
        assertEquals(
            listOf(2L),
            repository.observeReminders().first().map { it.noteId }
        )
    }

    @Test
    fun `onReminderFired marks shown and posts once`() = runBlocking {
        val id = repository.createReminder(noteId = 1L, reminderAt = 1000L)

        repository.onReminderFired(id)
        repository.onReminderFired(id)

        assertEquals(1, scheduler.posted.size)
        assertEquals(true, repository.observeReminders().first().single().shown)
    }

    @Test
    fun `reconcileOverdue marks and posts only overdue unshown`() = runBlocking {
        repository.createReminder(noteId = 1L, reminderAt = System.currentTimeMillis() - 1000L)
        repository.createReminder(noteId = 2L, reminderAt = System.currentTimeMillis() + 60_000L)

        repository.reconcileOverdue()

        assertEquals(1, scheduler.posted.size)
        val reminders = repository.observeReminders().first()
        assertEquals(true, reminders.first { it.noteId == 1L }.shown)
        assertEquals(false, reminders.first { it.noteId == 2L }.shown)
    }

    @Test
    fun `observeNextReminderByNote returns earliest pending per note`() = runBlocking {
        repository.createReminder(noteId = 1L, reminderAt = 5000L)
        repository.createReminder(noteId = 1L, reminderAt = 1000L)
        repository.createReminder(noteId = 2L, reminderAt = 3000L)
        repository.deleteReminder(1L)

        val map = repository.observeNextReminderByNote().first()

        assertEquals(1000L, map[1L])
        assertEquals(3000L, map[2L])
    }
}
