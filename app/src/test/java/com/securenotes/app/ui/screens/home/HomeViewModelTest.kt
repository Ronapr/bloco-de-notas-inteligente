package com.securenotes.app.ui.screens.home

import com.securenotes.app.domain.model.Note
import com.securenotes.app.ui.screens.editor.FakeNoteRepository
import com.securenotes.app.ui.screens.reminders.FakeReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNoteRepository
    private lateinit var reminderRepository: FakeReminderRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeNoteRepository()
        reminderRepository = FakeReminderRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun kotlinx.coroutines.test.TestScope.subscribe(viewModel: HomeViewModel) {
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()
    }

    @Test
    fun `home shows all active notes by default`() = runTest {
        repository.createNote(Note(title = "A", updatedAt = 2L))
        repository.createNote(Note(title = "B", updatedAt = 1L))

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        assertEquals(listOf("A", "B"), viewModel.uiState.value.notes.map { it.title })
    }

    @Test
    fun `favoritesOnly shows only favorite notes`() = runTest {
        val favoriteId = repository.createNote(Note(title = "Fav", updatedAt = 2L))
        repository.createNote(Note(title = "Normal", updatedAt = 1L))
        repository.setFavorite(favoriteId, true)

        val viewModel = HomeViewModel(repository, favoritesOnly = true, reminderRepository = reminderRepository)
        subscribe(viewModel)

        assertEquals(listOf("Fav"), viewModel.uiState.value.notes.map { it.title })
    }

    @Test
    fun `toggleFavorite marks and unmarks a note`() = runTest {
        val id = repository.createNote(Note(title = "A", updatedAt = 1L))
        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        val note = repository.observeNote(id).first()!!
        viewModel.toggleFavorite(note)
        advanceUntilIdle()

        assertTrue(repository.observeNote(id).first()!!.favorite)
        assertEquals(listOf("A"), viewModel.uiState.value.notes.map { it.title })

        viewModel.toggleFavorite(repository.observeNote(id).first()!!)
        advanceUntilIdle()

        assertEquals(false, repository.observeNote(id).first()!!.favorite)

        val favoritesViewModel = HomeViewModel(repository, favoritesOnly = true, reminderRepository = reminderRepository)
        subscribe(favoritesViewModel)
        assertTrue(favoritesViewModel.uiState.value.notes.isEmpty())
    }

    @Test
    fun `reminderAtByNote contains next reminder time per note`() = runTest {
        val noteId = repository.createNote(Note(title = "A", updatedAt = 1L))
        reminderRepository.createReminder(noteId, 5000L)
        reminderRepository.createReminder(noteId, 1000L)

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        assertEquals(1000L, viewModel.uiState.value.reminderAtByNote[noteId])
    }

    @Test
    fun `search filters notes by title`() = runTest {
        repository.createNote(Note(title = "Planejamento do mes", updatedAt = 2L))
        repository.createNote(Note(title = "Tarefa do dia", updatedAt = 1L))

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        viewModel.onSearchQueryChange("planej")
        advanceUntilIdle()

        assertEquals(listOf("Planejamento do mes"), viewModel.uiState.value.notes.map { it.title })
    }

    @Test
    fun `search filters notes by content`() = runTest {
        repository.createNote(Note(title = "A", content = "Comprar insumos da cozinha", updatedAt = 2L))
        repository.createNote(Note(title = "B", content = "Outro texto", updatedAt = 1L))

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        viewModel.onSearchQueryChange("insumos")
        advanceUntilIdle()

        assertEquals(listOf("A"), viewModel.uiState.value.notes.map { it.title })
    }

    @Test
    fun `search ignores accents and case`() = runTest {
        repository.createNote(Note(title = "Café da manhã", content = "Pão e manteiga", updatedAt = 2L))
        repository.createNote(Note(title = "Nota comum", updatedAt = 1L))

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        viewModel.onSearchQueryChange("CAFE")
        advanceUntilIdle()

        assertEquals(listOf("Café da manhã"), viewModel.uiState.value.notes.map { it.title })

        viewModel.onSearchQueryChange("manha")
        advanceUntilIdle()

        assertEquals(listOf("Café da manhã"), viewModel.uiState.value.notes.map { it.title })
    }

    @Test
    fun `search with no matches returns empty list`() = runTest {
        repository.createNote(Note(title = "Nota qualquer", updatedAt = 1L))

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        viewModel.onSearchQueryChange("inexistente")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notes.isEmpty())
        assertEquals("inexistente", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `clearing search restores full list`() = runTest {
        repository.createNote(Note(title = "Planejamento do mes", updatedAt = 2L))
        repository.createNote(Note(title = "Tarefa do dia", updatedAt = 1L))

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        viewModel.onSearchQueryChange("tarefa")
        advanceUntilIdle()
        assertEquals(listOf("Tarefa do dia"), viewModel.uiState.value.notes.map { it.title })

        viewModel.onSearchQueryChange("")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.notes.size)
    }

    @Test
    fun `search in favorites filters only favorite notes`() = runTest {
        val favoriteId = repository.createNote(Note(title = "Favorita planejada", updatedAt = 2L))
        repository.createNote(Note(title = "Comum planejada", updatedAt = 1L))
        repository.setFavorite(favoriteId, true)

        val viewModel = HomeViewModel(
            repository,
            favoritesOnly = true,
            reminderRepository = reminderRepository
        )
        subscribe(viewModel)

        viewModel.onSearchQueryChange("planejada")
        advanceUntilIdle()

        assertEquals(listOf("Favorita planejada"), viewModel.uiState.value.notes.map { it.title })
    }

    @Test
    fun `pinned note appears at top of home list`() = runTest {
        val newer = repository.createNote(Note(title = "Nova", updatedAt = 3L))
        val older = repository.createNote(Note(title = "Antiga", updatedAt = 1L))
        repository.setPinned(older, true)

        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        assertEquals(listOf("Antiga", "Nova"), viewModel.uiState.value.notes.map { it.title })
        assertEquals(older, viewModel.uiState.value.notes.first().id)
        assertTrue(newer < older || newer == viewModel.uiState.value.notes[1].id)
    }

    @Test
    fun `togglePin marks and unmarks a note`() = runTest {
        val id = repository.createNote(Note(title = "A", updatedAt = 1L))
        val viewModel = HomeViewModel(repository, reminderRepository = reminderRepository)
        subscribe(viewModel)

        val note = repository.observeNote(id).first()!!
        viewModel.togglePin(note)
        advanceUntilIdle()

        assertEquals(true, repository.observeNote(id).first()!!.pinned)

        viewModel.togglePin(repository.observeNote(id).first()!!)
        advanceUntilIdle()

        assertEquals(false, repository.observeNote(id).first()!!.pinned)
    }
}