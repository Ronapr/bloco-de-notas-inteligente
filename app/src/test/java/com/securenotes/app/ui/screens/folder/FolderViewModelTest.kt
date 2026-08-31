package com.securenotes.app.ui.screens.folder

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
class FolderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `create and delete folder updates state`() = runTest {
        val repository = FakeFolderRepository()
        val viewModel = FolderViewModel(repository)
        backgroundScope.launch { viewModel.uiState.collect { } }
        advanceUntilIdle()

        assertTrue(viewModel.createFolder("Pessoal"))
        advanceUntilIdle()
        assertEquals("Pessoal", viewModel.uiState.value.folders.single().name)

        val folderId = viewModel.uiState.value.folders.single().id
        viewModel.deleteFolder(folderId)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.folders.isEmpty())
    }
}
