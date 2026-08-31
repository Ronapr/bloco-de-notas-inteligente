package com.securenotes.app.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securenotes.app.database.entity.NoteEntity
import com.securenotes.app.database.entity.ReminderEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveReminderWithNoteTitle() = runTest {
        val noteId = database.noteDao().insert(
            NoteEntity(title = "Minha nota", content = "x", createdAt = 1L, updatedAt = 1L)
        )
        database.reminderDao().insert(
            ReminderEntity(noteId = noteId, reminderAt = 1000L, createdAt = 1L)
        )

        val reminder = database.reminderDao().observeReminders().first().single()

        assertEquals(noteId, reminder.noteId)
        assertEquals("Minha nota", reminder.noteTitle)
        assertEquals(1000L, reminder.reminderAt)
        assertEquals(false, reminder.shown)
    }

    @Test
    fun markShownAndUpdateTimePersist() = runTest {
        val noteId = database.noteDao().insert(
            NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L)
        )
        val id = database.reminderDao().insert(
            ReminderEntity(noteId = noteId, reminderAt = 1000L, createdAt = 1L)
        )

        database.reminderDao().updateTime(id, 2000L)
        database.reminderDao().markShown(id)

        val reminder = database.reminderDao().findById(id)
        assertEquals(2000L, reminder?.reminderAt)
        assertEquals(true, reminder?.shown)
    }

    @Test
    fun deleteByNoteIdRemovesOnlyThatNoteReminders() = runTest {
        val noteA = database.noteDao().insert(
            NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L)
        )
        val noteB = database.noteDao().insert(
            NoteEntity(title = "B", createdAt = 1L, updatedAt = 1L)
        )
        database.reminderDao().insert(
            ReminderEntity(noteId = noteA, reminderAt = 1000L, createdAt = 1L)
        )
        database.reminderDao().insert(
            ReminderEntity(noteId = noteB, reminderAt = 1000L, createdAt = 1L)
        )

        database.reminderDao().deleteByNoteId(noteA)

        val remaining = database.reminderDao().observeReminders().first()
        assertEquals(1, remaining.size)
        assertEquals(noteB, remaining.single().noteId)
    }

    @Test
    fun findOverdueUnshownReturnsOnlyOverduePending() = runTest {
        val noteId = database.noteDao().insert(
            NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L)
        )
        database.reminderDao().insert(
            ReminderEntity(noteId = noteId, reminderAt = 1000L, createdAt = 1L)
        )
        database.reminderDao().insert(
            ReminderEntity(noteId = noteId, reminderAt = 99999L, createdAt = 1L)
        )

        val overdue = database.reminderDao().findOverdueUnshown(5000L)

        assertEquals(1, overdue.size)
        assertEquals(1000L, overdue.single().reminderAt)
        assertEquals(
            2,
            database.reminderDao().findOverdueUnshown(99999L).size
        )
        assertTrue(database.reminderDao().observeNextReminderByNote().first().single().reminderAt == 1000L)
    }
}
