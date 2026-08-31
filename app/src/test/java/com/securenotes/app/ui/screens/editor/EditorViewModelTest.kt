package com.securenotes.app.ui.screens.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.securenotes.app.domain.model.Note
import com.securenotes.app.ui.screens.folder.FakeFolderRepository
import com.securenotes.app.ui.screens.reminders.FakeReminderRepository
import com.securenotes.app.spellcheck.OfflineSpellCheckEngine
import com.securenotes.app.spellcheck.SpellChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeNoteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save creates new note with timestamps`() = runTest {
        val viewModel = EditorViewModel(repository, noteId = 0L)
        var completed = false

        viewModel.onTitleChange("Título")
        viewModel.onContentChange("Conteúdo")
        viewModel.save { completed = true }
        advanceUntilIdle()

        assertTrue(completed)
        val notes = repository.observeNotes().first()
        assertEquals(1, notes.size)
        assertEquals("Título", notes.first().title)
        assertEquals("Conteúdo", notes.first().content)
        assertEquals("", notes.first().color)
        assertTrue(notes.first().createdAt > 0L)
        assertEquals(notes.first().createdAt, notes.first().updatedAt)
    }

    @Test
    fun `save persists chosen color`() = runTest {
        val viewModel = EditorViewModel(repository, noteId = 0L)

        viewModel.onTitleChange("Colorida")
        viewModel.onColorChange("roxo")
        viewModel.save { }
        advanceUntilIdle()

        val note = repository.observeNotes().first().first()
        assertEquals("roxo", note.color)
        assertEquals("roxo", viewModel.uiState.value.color)
    }

    @Test
    fun `existing note favorite state is loaded into editor state`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", favorite = true, createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isNew)
        assertEquals(true, viewModel.uiState.value.favorite)
    }

    @Test
    fun `save persists favorite for new note`() = runTest {
        val viewModel = EditorViewModel(repository, noteId = 0L)

        viewModel.onTitleChange("Favorita")
        viewModel.onFavoriteToggle()
        viewModel.save { }
        advanceUntilIdle()

        val note = repository.observeNotes().first().first()
        assertEquals(true, note.favorite)
        assertEquals(true, viewModel.uiState.value.favorite)
    }

    @Test
    fun `save updates favorite when toggled on existing note`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()
        viewModel.onFavoriteToggle()
        viewModel.save { }
        advanceUntilIdle()

        assertEquals(true, repository.observeNote(id).first()?.favorite)
    }

    @Test
    fun `existing note color is loaded into editor state`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", color = "verde", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isNew)
        assertEquals("verde", viewModel.uiState.value.color)
    }

    @Test
    fun `new note saves the selected folder`() = runTest {
        val folderRepository = FakeFolderRepository()
        val folderId = folderRepository.createFolder("Pessoal")!!
        val viewModel = EditorViewModel(
            repository,
            noteId = 0L,
            folderRepository = folderRepository
        )

        viewModel.onTitleChange("Na pasta")
        viewModel.onFolderChange(folderId)
        viewModel.save { }
        advanceUntilIdle()

        assertEquals(folderId, repository.observeNotes().first().single().folderId)
    }

    @Test
    fun `save updates existing note color`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", color = "azul", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()
        viewModel.onColorChange("amarelo")
        viewModel.save { }
        advanceUntilIdle()

        assertEquals("amarelo", repository.observeNote(id).first()?.color)
    }

    @Test
    fun `save skips empty note`() = runTest {
        val viewModel = EditorViewModel(repository, noteId = 0L)
        var completed = false

        viewModel.save { completed = true }
        advanceUntilIdle()

        assertTrue(completed)
        assertTrue(repository.observeNotes().first().isEmpty())
    }

    @Test
    fun `existing note is loaded into editor state`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isNew)
        assertEquals("A", state.title)
        assertEquals("old", state.content)
    }

    @Test
    fun `save updates existing note and refreshes updatedAt`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()
        viewModel.onContentChange("new")
        viewModel.save { }
        advanceUntilIdle()

        val updated = repository.observeNote(id).first()
        assertEquals("new", updated?.content)
        assertEquals(1L, updated?.createdAt)
        assertTrue((updated?.updatedAt ?: 0L) > 1L)
    }

    @Test
    fun `delete moves note to trash`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "x", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()
        viewModel.delete { }
        advanceUntilIdle()

        assertTrue(repository.observeNotes().first().isEmpty())
        assertEquals("A", repository.observeTrash().first().single().title)
        assertEquals("x", repository.observeTrash().first().single().content)
    }

    private fun spellEngine(): SpellChecker = OfflineSpellCheckEngine.fromWords(
        listOf("casa", "casas", "computador", "também", "teste")
    )

    @Test
    fun `spellcheck flags misspelled words after debounce`() = runTest {
        val viewModel = EditorViewModel(repository, 0L, spellEngine(), dispatcher)

        viewModel.onContentChange("casa cassas")
        advanceUntilIdle()

        val issues = viewModel.uiState.value.spellingIssues
        assertEquals(1, issues.size)
        assertEquals("cassas", issues.first().word)
        assertTrue(issues.first().suggestions.contains("casas"))
    }

    @Test
    fun `acceptSuggestion replaces misspelled word`() = runTest {
        val viewModel = EditorViewModel(repository, 0L, spellEngine(), dispatcher)

        viewModel.onContentChange("casa cassas")
        advanceUntilIdle()
        viewModel.nextIssue()
        advanceUntilIdle()
        assertEquals("cassas", viewModel.uiState.value.currentIssue?.word)

        viewModel.acceptSuggestion("cassas", "casas")
        advanceUntilIdle()

        assertEquals("casa casas", viewModel.uiState.value.content)
        assertTrue(viewModel.uiState.value.spellingIssues.isEmpty())
    }

    @Test
    fun `ignoreWord removes underline and keeps persistent behaviour`() = runTest {
        val viewModel = EditorViewModel(repository, 0L, spellEngine(), dispatcher)

        viewModel.onContentChange("casa cassas")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.spellingIssues.size)

        viewModel.ignoreWord("cassas")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.spellingIssues.isEmpty())
    }

    @Test
    fun `selection inside underlined word surfaces currentIssue`() = runTest {
        val viewModel = EditorViewModel(repository, 0L, spellEngine(), dispatcher)

        viewModel.onContentChange("casa cassas")
        advanceUntilIdle()

        viewModel.onContentFieldChange(TextFieldValue("casa cassas", TextRange(7, 7)))
        assertEquals("cassas", viewModel.uiState.value.currentIssue?.word)
    }

    @Test
    fun `moving selection outside words hides currentIssue`() = runTest {
        val viewModel = EditorViewModel(repository, 0L, spellEngine(), dispatcher)

        viewModel.onContentChange("casa cassas")
        advanceUntilIdle()

        viewModel.onContentFieldChange(TextFieldValue("casa cassas", TextRange(0, 0)))
        assertTrue(viewModel.uiState.value.currentIssue == null)
    }

    @Test
    fun `typing clears issues and rescans after debounce`() = runTest {
        val viewModel = EditorViewModel(repository, 0L, spellEngine(), dispatcher)

        viewModel.onContentFieldChange(TextFieldValue("casa cassas", TextRange(11, 11)))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.spellingIssues.size)

        viewModel.onContentFieldChange(TextFieldValue("casa cassas teste", TextRange(17, 17)))
        assertTrue(viewModel.uiState.value.spellingIssues.isEmpty())
        assertEquals("casa cassas teste", viewModel.uiState.value.content)

        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.spellingIssues.size)
        assertEquals("cassas", viewModel.uiState.value.spellingIssues.first().word)
    }

    @Test
    fun `existing note content is spellchecked after load`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "casa cassas", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, id, spellEngine(), dispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.spellCheckReady)
        assertEquals("casa cassas", viewModel.contentField.value.text)
        val issues = viewModel.uiState.value.spellingIssues
        assertEquals(1, issues.size)
        assertEquals("cassas", issues.first().word)
    }

    @Test
    fun `existing note pinned state is loaded into editor state`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", pinned = true, createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isNew)
        assertEquals(true, viewModel.uiState.value.pinned)
    }

    @Test
    fun `save persists pinned toggle`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )

        val viewModel = EditorViewModel(repository, noteId = id)
        advanceUntilIdle()
        viewModel.onPinToggle()
        viewModel.save { }
        advanceUntilIdle()

        assertEquals(true, repository.observeNote(id).first()?.pinned)
    }

    @Test
    fun `reminders of existing note are loaded into editor state`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )
        val reminderRepository = FakeReminderRepository()
        reminderRepository.createReminder(id, 1000L)

        val viewModel = EditorViewModel(
            repository,
            id,
            reminderRepository = reminderRepository
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.reminders.size)
        assertEquals(1000L, viewModel.uiState.value.reminders.single().reminderAt)
    }

    @Test
    fun `addReminder creates reminder for the note`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )
        val reminderRepository = FakeReminderRepository()

        val viewModel = EditorViewModel(
            repository,
            id,
            reminderRepository = reminderRepository
        )
        advanceUntilIdle()

        viewModel.addReminder(2000L)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.reminders.size)
        assertEquals(2000L, viewModel.uiState.value.reminders.single().reminderAt)
    }

    @Test
    fun `deleteReminder removes reminder from editor state`() = runTest {
        val id = repository.createNote(
            Note(title = "A", content = "old", createdAt = 1L, updatedAt = 1L)
        )
        val reminderRepository = FakeReminderRepository()
        val reminderId = reminderRepository.createReminder(id, 1000L)

        val viewModel = EditorViewModel(
            repository,
            id,
            reminderRepository = reminderRepository
        )
        advanceUntilIdle()

        viewModel.deleteReminder(reminderId)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminders.isEmpty())
    }
}
