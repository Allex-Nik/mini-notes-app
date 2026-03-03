package service

import org.education.service.NoteService
import org.education.repository.NoteRepositoryHibernateImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach

private class NoteServiceAndHibernateRepositoryTest : NoteServiceAndRepositoryTest() {
    override val noteRepository = NoteRepositoryHibernateImpl("hibernate/hibernate_test.cfg.xml")
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