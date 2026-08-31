package com.securenotes.app.data.repository

import com.securenotes.app.database.entity.FolderEntity
import com.securenotes.app.database.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FolderRepositoryImplTest {

    private lateinit var dao: FakeFolderDao
    private lateinit var repository: FolderRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeFolderDao()
        repository = FolderRepositoryImpl(dao)
    }

    @Test
    fun `createFolder trims name and persists it`() = runBlocking {
        val id = repository.createFolder("  Pessoal  ")

        assertEquals(1L, id)
        assertEquals("Pessoal", repository.observeFolders().first().single().name)
    }

    @Test
    fun `createFolder rejects blank and duplicate names`() = runBlocking {
        assertNull(repository.createFolder("   "))
        assertEquals(1L, repository.createFolder("Pessoal"))

        assertNull(repository.createFolder("pEsSoAl"))
        assertEquals(1, repository.observeFolders().first().size)
    }

    @Test
    fun `deleteFolder unlinks notes without deleting their data`() = runBlocking {
        val folderId = repository.createFolder("Pessoal")!!
        dao.seedNote(
            NoteEntity(
                id = 10L,
                title = "Nota",
                content = "Conteúdo",
                color = "roxo",
                favorite = true,
                folderId = folderId,
                createdAt = 1L,
                updatedAt = 2L
            )
        )

        repository.deleteFolder(folderId)

        val note = dao.note(10L)
        assertTrue(repository.observeFolders().first().isEmpty())
        assertEquals("Nota", note?.title)
        assertEquals("Conteúdo", note?.content)
        assertEquals("roxo", note?.color)
        assertEquals(true, note?.favorite)
        assertNull(note?.folderId)
    }
}
