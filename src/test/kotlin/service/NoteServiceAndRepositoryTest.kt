package service

import org.education.service.NoteService
import org.education.repository.NoteRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // reuse the class for every test method
abstract class NoteServiceAndRepositoryTest {
    abstract val noteRepository: NoteRepository
    abstract val noteService: NoteService

    @BeforeEach
    abstract fun emptyNotesTable()

    @AfterAll
    abstract fun closeConnection()

    // added to discuss and compare implementations
    @Test
    fun `createNote alternative`() {
        val id = noteService.saveNote(null, "Test note", "Test text")
        val savedNoteText = noteRepository.selectNote(id)
        val savedNoteTitle = noteRepository.loadAllNotes().find { it.id == id }?.title
            ?: error("Note with id=$id not found")

        assertEquals("Test text", savedNoteText)
        assertEquals("Test note", savedNoteTitle)
    }

    @Test
    fun updateNote() {
        val createdNoteId = noteRepository.insertNote("Old title", "Old text")
        val updatedNoteId = noteService.saveNote(createdNoteId, "Updated title", "Updated text")
        val updatedNoteText = noteRepository.selectNote(updatedNoteId)
        val updatedNoteTitle = noteRepository.loadAllNotes().find { it.id == updatedNoteId }?.title
            ?: error("Note with id=$updatedNoteId not found")

        assertEquals("Updated text", updatedNoteText)
        assertEquals("Updated title", updatedNoteTitle)
    }

    // good to check the "removed" flag, but NoteRepositoryJdbcImpl doesn't provide API for that
    @Test
    fun deleteNote() {
        val noteId = noteRepository.insertNote("Title", "Text")
        val deletedNotesNumber = noteService.deleteNote(noteId)

        assertEquals(1, deletedNotesNumber)
    }

    @Test
    fun selectNote() {
        val noteId = noteRepository.insertNote("Title", "Text")
        val selectedNoteText = noteService.selectNote(noteId)

        assertEquals("Text", selectedNoteText)
    }

    @Test
    fun loadAllNotes() {
        val emptyNotes = noteService.loadAllNotes()
        val firstNoteId = noteRepository.insertNote("Title1", "Text1")
        val secondNoteId = noteRepository.insertNote("Title2", "Text2")
        val notes = noteService.loadAllNotes()

        assertTrue(emptyNotes.isEmpty())
        assertEquals(2, notes.size)
        assertEquals(firstNoteId, notes[0].id)
        assertEquals(secondNoteId, notes[1].id)
        assertEquals("Title1", notes[0].title)
        assertEquals("Title2", notes[1].title)
    }
}