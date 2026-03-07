package service

import org.education.service.NoteService
import org.education.repository.NoteRepositoryHibernateImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import repository.HIBERNATE_CONFIGURATION_FILE_TEST

private class NoteServiceAndHibernateRepositoryTest : NoteServiceAndRepositoryTest() {
    override val noteRepository = NoteRepositoryHibernateImpl(HIBERNATE_CONFIGURATION_FILE_TEST)
    override val noteService = NoteService(noteRepository)

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
}