package com.securenotes.app.data.repository

import com.securenotes.app.domain.model.Note
import com.securenotes.app.domain.repository.ReminderRepository
import com.securenotes.app.test.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteRepositoryImplTest {

    private lateinit var dao: FakeNoteDao
    private lateinit var reminderDao: FakeReminderDao
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeNoteDao()
        reminderDao = FakeReminderDao()
        reminderScheduler = FakeReminderScheduler()
        reminderRepository = ReminderRepositoryImpl(reminderDao, reminderScheduler)
        repository = NoteRepositoryImpl(dao, reminderRepository)
    }

    @Test
    fun `createNote persists note and returns generated id`() = runBlocking {
        val id = repository.createNote(
            Note(title = "Título", content = "Conteúdo", createdAt = 1L, updatedAt = 1L)
        )

        val notes = repository.observeNotes().first()

        assertEquals(1L, id)
        assertEquals(1, notes.size)
        assertEquals("Título", notes.first().title)
        assertEquals("Conteúdo", notes.first().content)
    }

    @Test
    fun `observeNote returns the note with the requested id`() = runBlocking {
        val firstId = repository.createNote(Note(title = "A", createdAt = 1L, updatedAt = 1L))
        repository.createNote(Note(title = "B", createdAt = 2L, updatedAt = 2L))

        val note = repository.observeNote(firstId).first()

        assertEquals(firstId, note?.id)
        assertEquals("A", note?.title)
    }

    @Test
    fun `observeNote returns null when note does not exist`() = runBlocking {
        assertNull(repository.observeNote(999L).first())
    }

    @Test
    fun `updateNote changes stored data`() = runBlocking {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )

        repository.updateNote(
            Note(id = id, title = "B", content = "new", color = "laranja", createdAt = 1L, updatedAt = 2L)
        )

        val updated = repository.observeNote(id).first()
        assertEquals("B", updated?.title)
        assertEquals("new", updated?.content)
        assertEquals("laranja", updated?.color)
        assertEquals(2L, updated?.updatedAt)
    }

    @Test
    fun `createNote persists note color`() = runBlocking {
        val id = repository.createNote(
            Note(title = "A", content = "x", color = "vermelho", createdAt = 1L, updatedAt = 1L)
        )

        val note = repository.observeNote(id).first()
        assertEquals("vermelho", note?.color)
    }

    @Test
    fun `createNote persists favorite when marked as favorite`() = runBlocking {
        val id = repository.createNote(
            Note(title = "A", favorite = true, createdAt = 1L, updatedAt = 1L)
        )

        val note = repository.observeNote(id).first()
        assertEquals(true, note?.favorite)
    }

    @Test
    fun `notes default to not favorited`() = runBlocking {
        val id = repository.createNote(
            Note(title = "A", content = "x", createdAt = 1L, updatedAt = 1L)
        )

        val note = repository.observeNote(id).first()
        assertEquals(false, note?.favorite)
        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    @Test
    fun `setFavorite adds note to favorites and observeFavorites returns only favorites`() = runBlocking {
        val id = repository.createNote(
            Note(title = "A", createdAt = 1L, updatedAt = 1L)
        )
        repository.createNote(Note(title = "B", createdAt = 2L, updatedAt = 2L))

        repository.setFavorite(id, true)

        val favorites = repository.observeFavorites().first()
        assertEquals(listOf("A"), favorites.map { it.title })
        assertEquals(2, repository.observeNotes().first().size)
    }

    @Test
    fun `unfavoriting removes note from favorites`() = runBlocking {
        val id = repository.createNote(Note(title = "A", createdAt = 1L, updatedAt = 1L))
        repository.setFavorite(id, true)
        assertEquals(1, repository.observeFavorites().first().size)

        repository.setFavorite(id, false)

        assertTrue(repository.observeFavorites().first().isEmpty())
        assertEquals(false, repository.observeNote(id).first()?.favorite)
    }

    @Test
    fun `setFolder changes only the folder assignment`() = runBlocking {
        val id = repository.createNote(
            Note(
                title = "A",
                content = "texto",
                color = "azul",
                favorite = true,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        repository.setFolder(id, 42L)
        val assigned = repository.observeNote(id).first()

        assertEquals(42L, assigned?.folderId)
        assertEquals("texto", assigned?.content)
        assertEquals("azul", assigned?.color)
        assertEquals(true, assigned?.favorite)

        repository.setFolder(id, null)
        assertEquals(null, repository.observeNote(id).first()?.folderId)
    }

    @Test
    fun `setPinned persists pinned state`() = runBlocking {
        val id = repository.createNote(Note(title = "A", createdAt = 1L, updatedAt = 1L))
        assertEquals(false, repository.observeNote(id).first()?.pinned)

        repository.setPinned(id, true)
        assertEquals(true, repository.observeNote(id).first()?.pinned)

        repository.setPinned(id, false)
        assertEquals(false, repository.observeNote(id).first()?.pinned)
    }

    @Test
    fun `pinned notes come first in observeNotes`() = runBlocking {
        val newer = repository.createNote(Note(title = "Nova", createdAt = 1L, updatedAt = 3L))
        val older = repository.createNote(Note(title = "Antiga", createdAt = 1L, updatedAt = 1L))
        repository.setPinned(older, true)

        val notes = repository.observeNotes().first()

        assertEquals(listOf("Antiga", "Nova"), notes.map { it.title })
        assertEquals(older, notes.first().id)
        assertTrue(newer < older || newer == notes[1].id)
    }

    @Test
    fun `pinned does not change favorites ordering`() = runBlocking {
        val a = repository.createNote(Note(title = "A", updatedAt = 3L))
        val b = repository.createNote(Note(title = "B", updatedAt = 1L))
        repository.setFavorite(a, true)
        repository.setFavorite(b, true)
        repository.setPinned(b, true)

        val favorites = repository.observeFavorites().first()

        assertEquals(listOf("A", "B"), favorites.map { it.title })
    }

    @Test
    fun `deleteNote moves note to trash and preserves its data`() = runBlocking {
        val id = repository.createNote(
            Note(
                title = "A",
                content = "texto",
                color = "roxo",
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val note = repository.observeNote(id).first()

        repository.deleteNote(note!!)

        assertTrue(repository.observeNotes().first().isEmpty())
        val trashed = repository.observeTrash().first().single()
        assertEquals("A", trashed.title)
        assertEquals("texto", trashed.content)
        assertEquals("roxo", trashed.color)
        assertTrue(trashed.deleted)
        assertTrue((trashed.deletedAt ?: 0L) > 0L)
    }

    @Test
    fun `deleteNote cancels reminders of the note`() = runBlocking {
        val noteId = repository.createNote(Note(title = "A", createdAt = 1L, updatedAt = 1L))
        reminderDao.seedNoteTitle(noteId, "A")
        reminderRepository.createReminder(noteId, 1000L)
        val reminderId = reminderRepository.observeReminders().first().single().id

        repository.deleteNote(repository.observeNote(noteId).first()!!)

        assertTrue(reminderRepository.observeReminders().first().isEmpty())
        assertEquals(listOf(reminderId), reminderScheduler.cancelled)
    }

    @Test
    fun `restoreNote returns note to active list`() = runBlocking {
        val id = repository.createNote(Note(title = "A", createdAt = 1L, updatedAt = 1L))
        repository.deleteNote(repository.observeNote(id).first()!!)

        repository.restoreNote(repository.observeTrash().first().single())

        assertEquals("A", repository.observeNotes().first().single().title)
        assertTrue(repository.observeTrash().first().isEmpty())
        assertTrue(repository.observeNote(id).first()?.deleted == false)
    }

    @Test
    fun `permanentlyDeleteNote removes trashed note`() = runBlocking {
        val id = repository.createNote(Note(title = "A", createdAt = 1L, updatedAt = 1L))
        repository.deleteNote(repository.observeNote(id).first()!!)

        repository.permanentlyDeleteNote(repository.observeTrash().first().single())

        assertTrue(repository.observeTrash().first().isEmpty())
        assertTrue(repository.observeNote(id).first() == null)
    }

    @Test
    fun `emptyTrash permanently removes only trashed notes`() = runBlocking {
        val trashedId = repository.createNote(Note(title = "Lixeira", createdAt = 1L, updatedAt = 1L))
        repository.createNote(Note(title = "Ativa", createdAt = 2L, updatedAt = 2L))
        repository.deleteNote(repository.observeNote(trashedId).first()!!)

        repository.emptyTrash()

        assertTrue(repository.observeTrash().first().isEmpty())
        assertEquals("Ativa", repository.observeNotes().first().single().title)
    }
}
