package org.education.repository

import org.education.exceptions.*
import org.education.model.Note
import org.education.model.NoteData
import org.education.model.NoteListItem
import org.hibernate.NonUniqueResultException
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.time.Instant

internal class NoteRepositoryHibernateImpl(configurationFile: String) : NoteRepository {
    companion object {
        private const val UPDATE_NOTE_QUERY = """
            UPDATE Note n SET n.lastEditedDateTime = :now, n.title = :title, n.text = :text 
            WHERE n.id = :id AND n.removed = false
            """

        private const val SELECT_NOTE_QUERY = """
            SELECT new org.education.model.NoteData(n.title, n.text) FROM Note n WHERE n.id = :id AND n.removed = false
            """

        private const val DELETE_NOTE_QUERY =
            "UPDATE Note n SET n.removed = true WHERE n.id = :id AND n.removed = false"

        private const val LOAD_ALL_NOTES_QUERY = """
            SELECT new org.education.model.NoteListItem(n.id, n.title) 
            FROM Note n 
            WHERE n.removed = false 
            ORDER BY n.lastEditedDateTime DESC, n.id DESC
            """
    }

    val sessionFactory = buildSessionFactory(configurationFile)

    // https://docs.hibernate.org/orm/7.2/introduction/html_single/#managing-transactions
    override fun insertNote(title: String, text: String): Long =
        sessionFactory.fromTransaction { session ->
            val now = Instant.now()
            val note = Note().apply {
                creationDateTime = now
                lastEditedDateTime = now
                this.title = title
                this.text = text
                removed = false
            }
            session.persist(note)
            note.id ?: throw NoteNotInsertedException()
        }

    override fun updateNote(id: Long, title: String, text: String) =
        sessionFactory.inTransaction { session ->
            val affectedInstances = session.createMutationQuery(
                UPDATE_NOTE_QUERY
            )
                .setParameter("now", Instant.now())
                .setParameter("title", title)
                .setParameter("text", text)
                .setParameter("id", id)
                .executeUpdate()
            if (affectedInstances == 0) throw NoteNotFoundException()
            if (affectedInstances > 1) throw MultipleRowsAffectedException()
        }

    override fun selectNote(id: Long): NoteData =
        sessionFactory.fromTransaction { session ->
            try {
                session.createQuery(
                    SELECT_NOTE_QUERY,
                    NoteData::class.java
                )
                    .setParameter("id", id)
                    .uniqueResultOptional() // throws NonUniqueResultException if > 1 row is returned
                    .orElseThrow { NoteNotFoundException() }
            } catch (_: NonUniqueResultException) {
                throw NonUniqueNoteException()
            }
        }

    // If an exception is triggered inside a transaction, the transaction is rolled back
    override fun deleteNote(id: Long): Unit =
        sessionFactory.inTransaction { session ->
            val affectedInstances = session
                .createMutationQuery(DELETE_NOTE_QUERY)
                .setParameter("id", id)
                .executeUpdate()
            if (affectedInstances == 0) throw NoteNotDeletedException()
            if (affectedInstances > 1) throw MultipleRowsAffectedException()
        }

    override fun loadAllNotes(): List<NoteListItem> =
        sessionFactory.fromTransaction { session ->
            session.createQuery(
                LOAD_ALL_NOTES_QUERY,
                NoteListItem::class.java
            )
                .resultList
        }
}

fun buildSessionFactory(configurationFile: String): SessionFactory =
    Configuration().configure(configurationFile).buildSessionFactory()