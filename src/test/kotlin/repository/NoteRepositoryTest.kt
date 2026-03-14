package repository

import org.education.exceptions.NoteNotDeletedException
import org.education.exceptions.NoteNotFoundException
import org.education.repository.NoteRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal abstract class NoteRepositoryTest {
    abstract val noteRepository: NoteRepository

    @BeforeEach
    abstract fun emptyNotesTable()

    @AfterAll
    abstract fun closeConnection()

    @Test
    fun insertNote() {
        val title = "Title"
        val text = "Text"

        val noteId = noteRepository.insertNote(title, text)
        val insertedNote = noteRepository.selectNote(noteId)

        assertEquals(text, insertedNote.text)
        assertEquals(title, insertedNote.title)
    }

    @Test
    abstract fun `insert note with too long title`()

    @Test
    fun updateNote() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        noteRepository.updateNote(noteId, "New Title", "New Text")
        val updatedNote = noteRepository.selectNote(noteId)

        assertEquals("New Text", updatedNote.text)
        assertEquals("New Title", updatedNote.title)
    }

    @Test
    abstract fun `updateNote with too long title`()

    @Test
    fun `updateNote with deleted note`() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        noteRepository.deleteNote(noteId)
        assertThrows<NoteNotFoundException> { noteRepository.updateNote(noteId, "New Title", "New Text") }
    }

    @Test
    fun selectNote() {
        val noteId = noteRepository.insertNote("Title", "Text to select")
        val selectedNote = noteRepository.selectNote(noteId)

        assertEquals("Text to select", selectedNote.text)
        assertEquals("Title", selectedNote.title)
    }

    @Test
    fun deleteNote() {
        val noteId = noteRepository.insertNote(
            "Title of the note to delete",
            "Text of the note to delete"
        )
        assertEquals(1, noteRepository.loadAllNotes().size)
        noteRepository.deleteNote(noteId)
        assertEquals(0, noteRepository.loadAllNotes().size)
    }

    @Test
    fun `deleteNote with non-existing note`() {
        assertThrows<NoteNotDeletedException> { noteRepository.deleteNote(0) }
    }

    @Test
    fun loadAllNotes() {
        val emptyNotes = noteRepository.loadAllNotes()
        val firstNoteId = noteRepository.insertNote("Title1", "Text1")
        val secondNoteId = noteRepository.insertNote("Title2", "Text2")
        val notes = noteRepository.loadAllNotes()

        assertTrue(emptyNotes.isEmpty())
        assertEquals(2, notes.size)
        assert(notes.any { it.id == firstNoteId })
        assert(notes.any { it.id == secondNoteId })
        assert(notes.any { it.title == "Title1" })
        assert(notes.any { it.title == "Title2" })
    }

    @Test
    fun `loadAllNotes sorts notes by lastEditedDateTime in descending order`() {
        val firstNoteId = noteRepository.insertNote("Title1", "Text1")
        val secondNoteId = noteRepository.insertNote("Title2", "Text2")
        val thirdNoteId = noteRepository.insertNote("Title3", "Text3")
        noteRepository.updateNote(secondNoteId, "Title2 new", "Text2 new")
        val notes = noteRepository.loadAllNotes()

        assertEquals(secondNoteId, notes[0].id)
        assertEquals(thirdNoteId, notes[1].id)
        assertEquals(firstNoteId, notes[2].id)
    }
}