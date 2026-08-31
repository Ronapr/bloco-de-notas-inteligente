package com.securenotes.app.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securenotes.app.database.dao.NoteDao
import com.securenotes.app.database.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseInitializesAndNotesTableStartsEmpty() = runTest {
        assertTrue(dao.observeNotes().first().isEmpty())
    }

    @Test
    fun insertNoteAndReadItBack() = runTest {
        val id = dao.insert(
            NoteEntity(title = "Compra", content = "Pão", createdAt = 10L, updatedAt = 10L)
        )

        val notes = dao.observeNotes().first()

        assertEquals(1, notes.size)
        assertEquals(id, notes.first().id)
        assertEquals("Compra", notes.first().title)
        assertEquals("Pão", notes.first().content)
    }

    @Test
    fun observeNoteReturnsTheMatchingNote() = runTest {
        val firstId = dao.insert(NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L))
        dao.insert(NoteEntity(title = "B", createdAt = 2L, updatedAt = 2L))

        val note = dao.observeNote(firstId).first()

        assertEquals(firstId, note?.id)
        assertEquals("A", note?.title)
    }

    @Test
    fun updateNotePersistsChanges() = runTest {
        val id = dao.insert(
            NoteEntity(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )

        dao.update(
            NoteEntity(id = id, title = "A2", content = "new", createdAt = 1L, updatedAt = 2L)
        )

        val updated = dao.observeNote(id).first()
        assertEquals("A2", updated?.title)
        assertEquals("new", updated?.content)
        assertEquals(2L, updated?.updatedAt)
    }

    @Test
    fun deleteNoteRemovesItFromDatabase() = runTest {
        val id = dao.insert(NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L))
        val note = dao.observeNote(id).first()

        dao.delete(note!!)

        assertTrue(dao.observeNotes().first().isEmpty())
    }

    @Test
    fun insertedNotesDefaultToNotFavorited() = runTest {
        val id = dao.insert(NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L))

        val note = dao.observeNote(id).first()

        assertEquals(false, note?.favorite)
        assertTrue(dao.observeFavoriteNotes().first().isEmpty())
    }

    @Test
    fun setFavoriteAddsAndRemovesNoteFromFavorites() = runTest {
        val id = dao.insert(NoteEntity(title = "A", createdAt = 1L, updatedAt = 1L))

        dao.setFavorite(id, true)

        val favorites = dao.observeFavoriteNotes().first()
        assertEquals(1, favorites.size)
        assertEquals(id, favorites.first().id)
        assertEquals(true, favorites.first().favorite)

        dao.setFavorite(id, false)

        assertTrue(dao.observeFavoriteNotes().first().isEmpty())
        assertEquals(false, dao.observeNote(id).first()?.favorite)
    }

    @Test
    fun setPinnedPersistsAndOrdersPinnedFirst() = runTest {
        val newer = dao.insert(NoteEntity(title = "Nova", createdAt = 1L, updatedAt = 3L))
        val older = dao.insert(NoteEntity(title = "Antiga", createdAt = 1L, updatedAt = 1L))

        dao.setPinned(older, true)

        val notes = dao.observeNotes().first()
        assertEquals(listOf("Antiga", "Nova"), notes.map { it.title })
        assertEquals(true, dao.observeNote(older).first()?.pinned)

        dao.setPinned(older, false)
        assertEquals(false, dao.observeNote(older).first()?.pinned)
        assertEquals(listOf("Nova", "Antiga"), dao.observeNotes().first().map { it.title })
        assertTrue(newer != older)
    }
}
