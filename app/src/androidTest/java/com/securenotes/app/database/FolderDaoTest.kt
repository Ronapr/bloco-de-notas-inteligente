package com.securenotes.app.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.securenotes.app.database.entity.FolderEntity
import com.securenotes.app.database.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderDaoTest {

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
    fun folderListsOnlyItsNotesAndCount() = runTest {
        val folderId = database.folderDao().insert(FolderEntity(name = "Pessoal"))
        val noteId = database.noteDao().insert(
            NoteEntity(
                title = "Nota",
                content = "Conteúdo",
                color = "roxo",
                favorite = true,
                folderId = folderId,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        assertEquals(noteId, database.noteDao().observeFolderNotes(folderId).first().single().id)
        assertEquals(1, database.folderDao().observeFolders().first().single().noteCount)
    }

    @Test
    fun deletingFolderUnlinksNotesAndPreservesTheirData() = runTest {
        val folderId = database.folderDao().insert(FolderEntity(name = "Pessoal"))
        val noteId = database.noteDao().insert(
            NoteEntity(
                title = "Nota",
                content = "Conteúdo",
                color = "azul",
                favorite = true,
                folderId = folderId,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        database.folderDao().deleteFolder(folderId)

        val note = database.noteDao().observeNote(noteId).first()
        assertTrue(database.folderDao().observeFolders().first().isEmpty())
        assertEquals("Nota", note?.title)
        assertEquals("Conteúdo", note?.content)
        assertEquals("azul", note?.color)
        assertEquals(true, note?.favorite)
        assertNull(note?.folderId)
    }
}
