package repository

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
        val insertedNoteText = noteRepository.selectNote(noteId)
        val insertedNoteTitle = noteRepository.loadAllNotes().find { it.id == noteId }?.title
            ?: error("Note with id=$noteId not found")

        assertEquals(text, insertedNoteText)
        assertEquals(title, insertedNoteTitle)
    }

    @Test
    abstract fun `insert note with too long title`()

    @Test
    fun updateNote() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        noteRepository.updateNote(noteId, "New Title", "New Text")
        val updatedNoteText = noteRepository.selectNote(noteId)
        val updatedNoteTitle = noteRepository.loadAllNotes().find { it.id == noteId }?.title
            ?: error("Note with id=$noteId not found")

        assertEquals("New Text", updatedNoteText)
        assertEquals("New Title", updatedNoteTitle)
    }

    @Test
    abstract fun `updateNote with too long title`()

    @Test
    fun `updateNote with deleted note`() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        noteRepository.deleteNote(noteId)
        assertThrows<IllegalArgumentException> {
            noteRepository.updateNote(noteId, "New Title", "New Text")
        }
    }

    @Test
    fun selectNote() {
        val noteId = noteRepository.insertNote("Title", "Text to select")
        val selectedNoteText = noteRepository.selectNote(noteId)

        assertEquals("Text to select", selectedNoteText)
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
        assertThrows<IllegalArgumentException> { noteRepository.deleteNote(0) }
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