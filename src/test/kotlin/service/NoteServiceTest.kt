package service

import org.education.exceptions.BlankNoteNameException
import org.education.exceptions.HeaderTooLongException
import org.education.model.NOTE_TITLE_MAX_LENGTH
import org.education.service.NoteService
import org.education.ui.UIText.EMPTY_NOTE_TITLE
import org.education.ui.UIText.START_HEADER_TEXT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NoteServiceTest {
    val noteRepository = NoteRepositoryMock()
    val noteService = NoteService(noteRepository)

    @Test
    fun `getNoteName with header set`() {
        val header = "Title"
        val noteText = "Text"

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "Title"

        assertEquals(noteName, expected)
    }

    @Test
    fun `getNoteName with default header`() {
        val header = START_HEADER_TEXT
        val noteText = "Text"

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "Text"

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName with empty header`() {
        val header = ""
        val noteText = "Text"

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "Text"

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName with empty note`() {
        val header = START_HEADER_TEXT
        val noteText = ""

        val noteName = noteService.getNoteName(header, noteText)
        val expected = EMPTY_NOTE_TITLE

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName truncates long note name from first word (header is empty)`() {
        val header = ""
        val noteText = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "A".repeat(NOTE_TITLE_MAX_LENGTH)

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName truncates long note name from first word (header is default)`() {
        val header = START_HEADER_TEXT
        val noteText = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "A".repeat(NOTE_TITLE_MAX_LENGTH)

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName with long header`() {
        val header = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)
        val noteText = "Text"

        assertThrows<HeaderTooLongException> { noteService.getNoteName(header, noteText) }
    }

    @Test
    fun `create note`() {
        val noteId = null
        val noteName = "Title"
        val noteText = "Text"

        val repoSizeBefore = noteRepository.size()
        val savedNoteId = noteService.saveNote(noteId, noteName, noteText)
        val repoSizeAfter = noteRepository.size()

        // could also make noteRepository.notes public, retrieve the whole note and also compare title and "removed"
        // here we assume that noteRepository methods are correct
        val savedNoteText = noteRepository.selectNote(savedNoteId) // only returns text of the note
        val savedNote = noteRepository.loadAllNotes().find { it.id == savedNoteId } // doesn't contain note text
            ?: error("Note with id=$savedNoteId not found")

        assertEquals(repoSizeBefore + 1, repoSizeAfter)
        assertEquals(noteText, savedNoteText)
        assertEquals(noteName, savedNote.title)
    }

    @Test
    fun `create note with long name`() {
        val noteId = null
        val noteName = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)
        val noteText = "Text"
        val repoSizeBefore = noteRepository.size()

        assertThrows<HeaderTooLongException> { noteService.saveNote(noteId, noteName, noteText) }

        val repoSizeAfter = noteRepository.size()
        assertEquals(repoSizeBefore, repoSizeAfter)
    }

    @Test
    fun `create note with empty name`() {
        val noteId = null
        val noteName = ""
        val noteText = "Text"

        val repoSizeBefore = noteRepository.size()
        assertThrows<BlankNoteNameException> { noteService.saveNote(noteId, noteName, noteText) }
        val repoSizeAfter = noteRepository.size()
        assertEquals(repoSizeBefore, repoSizeAfter)
    }

    @Test
    fun `create note with whitespace name`() {
        val noteId = null
        val noteName = " "
        val noteText = "Text"

        val repoSizeBefore = noteRepository.size()
        assertThrows<BlankNoteNameException> { noteService.saveNote(noteId, noteName, noteText) }
        val repoSizeAfter = noteRepository.size()
        assertEquals(repoSizeBefore, repoSizeAfter)
    }

    @Test
    fun `update note`() {
        val noteToUpdateId = noteRepository.insertNote("Old title", "Old text")

        val repoSizeBeforeUpdate = noteRepository.size()
        val updatedNoteId = noteService.saveNote(noteToUpdateId, "New title", "New text")

        val repoSizeAfterUpdate = noteRepository.size()
        val updatedNoteText = noteRepository.selectNote(updatedNoteId)
        val updatedNote = noteRepository.loadAllNotes().find { it.id == updatedNoteId }
            ?: error("Note with id=$updatedNoteId not found")

        assertEquals(repoSizeBeforeUpdate, repoSizeAfterUpdate)
        assertEquals(noteToUpdateId, updatedNoteId)
        assertEquals("New text", updatedNoteText)
        assertEquals("New title", updatedNote.title)
    }

    @Test
    fun `update note with long name`() {
        val noteToUpdateId = noteRepository.insertNote("Old title", "Old text")
        val noteName = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)
        val noteText = "Text"

        assertThrows<HeaderTooLongException> { noteService.saveNote(noteToUpdateId, noteName, noteText) }
    }

    @Test
    fun `update note with empty name`() {
        val noteToUpdateId = noteRepository.insertNote("Old title", "Old text")
        val noteName = ""
        val noteText = "Text"

        assertThrows<BlankNoteNameException> { noteService.saveNote(noteToUpdateId, noteName, noteText) }
    }

    @Test
    fun `update note with whitespace name`() {
        val noteToUpdateId = noteRepository.insertNote("Old title", "Old text")
        val noteName = " "
        val noteText = "Text"

        assertThrows<BlankNoteNameException> { noteService.saveNote(noteToUpdateId, noteName, noteText) }
    }
}