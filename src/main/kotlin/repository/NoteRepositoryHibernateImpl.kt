package org.education.repository

import org.education.model.Note
import org.education.model.NoteListItem
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.time.Instant

internal class NoteRepositoryHibernateImpl(configurationFile: String) : NoteRepository {
    val sessionFactory = buildSessionFactory(configurationFile)

    // done by Hibernate automatically
    override fun createNotesTableIfNotExists() {}

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
            // not a business invariant and probably not a programming error, but some problem in the database
            // is that check correct?
            checkNotNull(note.id) { "Expected to insert a note and get its id, but got null" }
        }

    override fun updateNote(id: Long, title: String, text: String) =
        sessionFactory.inTransaction { session ->
            val affectedInstances = session.createMutationQuery(
                "UPDATE Note n SET n.lastEditedDateTime = :now, n.title = :title, n.text = :text WHERE n.id = :id AND n.removed = false"
            )
                .setParameter("now", Instant.now())
                .setParameter("title", title)
                .setParameter("text", text)
                .setParameter("id", id)
                .executeUpdate()
            // require throws IllegalArgumentException. Does it suit here? Maybe use a custom exception like in fb91a488?
            require(affectedInstances == 1) {
                "Expected to update exactly 1 note with id=$id, but tried to update $affectedInstances notes"
            }
        }

    override fun selectNote(id: Long): String =
        sessionFactory.fromTransaction { session ->
            val selectedInstances = session.createQuery(
                "SELECT n.text FROM Note n WHERE n.id = :id AND n.removed = false",
                String::class.java
            ).setParameter("id", id).resultList
            require(selectedInstances.size == 1) {
                "Expected to find exactly 1 note with id=$id, but found ${selectedInstances.size} notes"
            }
            selectedInstances.first()
        }

    // If an exception is triggered inside a transaction, the transaction is rolled back
    override fun deleteNote(id: Long): Unit =
        sessionFactory.inTransaction { session ->
            val affectedInstances = session
                .createMutationQuery("UPDATE Note n SET n.removed = true WHERE n.id = :id AND n.removed = false")
                .setParameter("id", id)
                .executeUpdate()
            require(affectedInstances == 1) {
                "Expected to delete exactly 1 note with id=$id, but tried to delete $affectedInstances notes"
            }
        }

    override fun loadAllNotes(): List<NoteListItem> =
        sessionFactory.fromTransaction { session ->
            session.createQuery(
                "SELECT new org.education.model.NoteListItem(n.id, n.title) FROM Note n WHERE n.removed = false ORDER BY n.lastEditedDateTime DESC, n.id DESC",
                NoteListItem::class.java
            )
                .resultList
        }
}

fun buildSessionFactory(configurationFile: String): SessionFactory =
    Configuration().configure(configurationFile).buildSessionFactory()