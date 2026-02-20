import jakarta.validation.ConstraintViolationException
import org.education.MAX_CHARACTERS
import org.education.repository.NoteRepositoryHibernateImpl
import org.hibernate.exception.DataException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoteRepositoryHibernateImplTest {
    val noteRepository = NoteRepositoryHibernateImpl("hibernate/hibernate_test.cfg.xml")

    @BeforeEach
    fun emptyNotesTable() {
        noteRepository.sessionFactory.inTransaction { session ->
            session.createMutationQuery("DELETE FROM Note").executeUpdate()
        }
    }

    @AfterAll
    fun closeConnection() {
        noteRepository.sessionFactory.close()
    }

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
    fun `insert note with too long title`() {
        val title = "A".repeat(MAX_CHARACTERS + 5)
        val text = "Text"

        assertThrows<ConstraintViolationException> { noteRepository.insertNote(title, text) }
    }

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
    fun `updateNote with too long title`() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        val newTitle = "A".repeat(MAX_CHARACTERS + 5)

        assertThrows<DataException> {
            noteRepository.updateNote(noteId, newTitle, "New Text")
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
        val deletedNotesNumber = noteRepository.deleteNote(noteId)

        assertEquals(1, deletedNotesNumber)
        assertEquals(0, noteRepository.loadAllNotes().size)
    }

    @Test
    fun loadAllNotes() {
        val emptyNotes = noteRepository.loadAllNotes()
        val firstNoteId = noteRepository.insertNote("Title1", "Text1")
        val secondNoteId = noteRepository.insertNote("Title2", "Text2")
        val notes = noteRepository.loadAllNotes()

        assertTrue(emptyNotes.isEmpty())
        assertEquals(2, notes.size)
        assertEquals(firstNoteId, notes[0].id)
        assertEquals(secondNoteId, notes[1].id)
        assertEquals("Title1", notes[0].title)
        assertEquals("Title2", notes[1].title)
    }
}