package org.education.repository

import org.education.model.Note
import org.education.model.NoteListItem
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.time.Instant

class NoteRepositoryHibernateImpl(configurationFile: String) : NoteRepository {
    val sessionFactory = buildSessionFactory(configurationFile)

    // https://docs.hibernate.org/orm/7.2/introduction/html_single/#managing-transactions
    override fun insertNote(title: String, text: String): Long =
        sessionFactory.fromTransaction { session ->
            val note =
                Note(creationDateTime = Instant.now(), lastEditedDateTime = Instant.now(), title = title, text = text, removed = false)
            session.persist(note)
            note.id ?: error("id was not generated")
        }

    override fun updateNote(id: Long, title: String, text: String) =
        sessionFactory.inTransaction { session ->
            session.createMutationQuery("UPDATE Note SET lastEditedDateTime = :now, title = :title, text = :text WHERE id = :id")
                .setParameter("now", Instant.now())
                .setParameter("title", title)
                .setParameter("text", text)
                .setParameter("id", id)
                .executeUpdate()
        }

    override fun selectNote(id: Long): String =
        sessionFactory.fromTransaction { session ->
            session.createQuery("SELECT n.text FROM Note n WHERE n.id = :id", String::class.java)
                .setParameter("id", id)
                .uniqueResultOptional()
                .orElse("")
        }

    // If an exception is triggered inside a transaction, the transaction is rolled back
    override fun deleteNote(id: Long): Int =
        sessionFactory.fromTransaction { session ->
            val affectedInstances = session
                .createMutationQuery("UPDATE Note n SET n.removed = true WHERE n.id = :id")
                .setParameter("id", id)
                .executeUpdate()
            if (affectedInstances == 0) error("The note was not deleted")
            if (affectedInstances > 1) error("Attempt to delete multiple notes. Nothing was deleted.")
            affectedInstances
        }

    // TODO: Find annotation to replace recurring code with sessions and transactions
    override fun loadAllNotes(): List<NoteListItem> =
        sessionFactory.fromTransaction { session ->
            session.createQuery(
                "SELECT new org.education.model.NoteListItem(n.id, n.creationDateTime, n.lastEditedDateTime, n.title) FROM Note n WHERE n.removed = false",
                NoteListItem::class.java
            )
                .resultList
        }
}

fun buildSessionFactory(configurationFile: String): SessionFactory =
    Configuration().configure(configurationFile).buildSessionFactory()