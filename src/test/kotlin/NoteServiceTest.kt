import org.education.MAX_CHARACTERS
import org.education.NoteService
import org.education.UIText.EMPTY_NOTE_TITLE
import org.education.UIText.START_HEADER_TEXT
import org.education.UIText.TXT_EXTENSION
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoteServiceTest {
    val noteRepository = NoteRepositoryMock()
    val noteService = NoteService(noteRepository)

    @Test
    fun `getNoteName with header set`() {
        val header = "Title"
        val noteText = "Text"

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "Title$TXT_EXTENSION"

        assertEquals(noteName, expected)
    }

    @Test
    fun `getNoteName with default header`() {
        val header = START_HEADER_TEXT
        val noteText = "Text"

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "Text$TXT_EXTENSION"

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName with empty header`() {
        val header = ""
        val noteText = "Text"

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "Text$TXT_EXTENSION"

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName with empty note`() {
        val header = START_HEADER_TEXT
        val noteText = ""

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "$EMPTY_NOTE_TITLE$TXT_EXTENSION"

        assertEquals(expected, noteName)
    }

    @Test
    fun `getNoteName with long header`() {
        val header = ""
        val noteText = "A".repeat(MAX_CHARACTERS + 5)

        val noteName = noteService.getNoteName(header, noteText)
        val expected = "${"A".repeat(MAX_CHARACTERS)}$TXT_EXTENSION"

        assertEquals(expected, noteName)
    }

    @Test
    fun `saveNote creating note`() {
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
    fun `saveNote updating note`() {
        val noteToUpdateId = noteRepository.insertNote("Old title", "Old text")

        val updatedNoteId = noteService.saveNote(noteToUpdateId, "New title", "New text")
        val updatedNoteText = noteRepository.selectNote(updatedNoteId)
        val updatedNote = noteRepository.loadAllNotes().find { it.id == updatedNoteId }
            ?: error("Note with id=$updatedNoteId not found")

        assertEquals(noteToUpdateId, updatedNoteId)
        assertEquals("New text", updatedNoteText)
        assertEquals("New title", updatedNote.title)
    }
}