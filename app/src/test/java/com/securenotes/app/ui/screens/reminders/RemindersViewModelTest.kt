package com.securenotes.app.ui.screens.reminders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class RemindersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeReminderRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeReminderRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reconciles overdue reminders on init and lists them`() = runTest {
        repository.createReminder(
            noteId = 1L,
            reminderAt = System.currentTimeMillis() - 60_000L
        )
        repository.createReminder(
            noteId = 2L,
            reminderAt = System.currentTimeMillis() + 60_000L
        )

        val viewModel = RemindersViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertEquals(1, repository.reconcileCount)
        assertEquals(2, viewModel.uiState.value.reminders.size)
    }

    @Test
    fun `deleteReminder removes the reminder from the list`() = runTest {
        val id = repository.createReminder(noteId = 1L, reminderAt = 1000L)

        val viewModel = RemindersViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.deleteReminder(id)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.reminders.isEmpty())
    }
}
