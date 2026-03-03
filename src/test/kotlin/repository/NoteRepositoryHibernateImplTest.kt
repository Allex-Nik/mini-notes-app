package repository

import jakarta.validation.ConstraintViolationException
import org.education.model.NOTE_TITLE_MAX_LENGTH
import org.education.repository.NoteRepositoryHibernateImpl
import org.hibernate.exception.DataException
import org.junit.jupiter.api.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
private class NoteRepositoryHibernateImplTest : NoteRepositoryTest() {
    override val noteRepository = NoteRepositoryHibernateImpl("hibernate/hibernate_test.cfg.xml")

    @BeforeEach
    override fun emptyNotesTable() {
        noteRepository.sessionFactory.inTransaction { session ->
            session.createMutationQuery("DELETE FROM Note").executeUpdate()
        }
    }

    @AfterAll
    override fun closeConnection() {
        noteRepository.sessionFactory.close()
    }

    @Test
    override fun `insert note with too long title`() {
        val title = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)
        val text = "Text"

        assertThrows<ConstraintViolationException> { noteRepository.insertNote(title, text) }
    }

    @Test
    override fun `updateNote with too long title`() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        val newTitle = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)

        assertThrows<DataException> {
            noteRepository.updateNote(noteId, newTitle, "New Text")
        }
    }
}