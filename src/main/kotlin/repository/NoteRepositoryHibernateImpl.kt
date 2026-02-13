package org.education.repository

import org.education.Note
import org.education.NoteListItem
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.time.Instant

// TODO: Use @CheckHQL and @NamedQuery for compile time query validation:
//  see 1.6 in https://docs.hibernate.org/orm/7.2/introduction/html_single/#organizing-persistence
// TODO: Create and inject a Queries repository (same link as above)
class NoteRepositoryHibernateImpl : NoteRepository {
    val sessionFactory = buildSessionFactory()

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

    override fun deleteNote(id: Long): Int =
        sessionFactory.fromTransaction { session ->
            session.createMutationQuery("UPDATE Note n SET n.removed = true WHERE n.id = :id") // TODO: provoke the problem: #rows != 1
                .setParameter("id", id)
                .executeUpdate() // TODO: If #rows != 1, throw exception
        }

    // TODO: Find annotation to replace recurring code with sessions and transactions
    override fun loadAllNotes(): List<NoteListItem> =
        sessionFactory.fromTransaction { session ->
            session.createQuery(
                "SELECT new org.education.NoteListItem(n.id, n.creationDateTime, n.lastEditedDateTime, n.title) FROM Note n WHERE n.removed = false",
                NoteListItem::class.java
            )
                .resultList
        }
}

fun buildSessionFactory(): SessionFactory {
    return Configuration().configure("hibernate/hibernate.cfg.xml").buildSessionFactory()
    /**
     * Programmatic setup:
     * val sessionFactory = HibernatePersistenceConfiguration("notesapp")
     *         .managedClass(Note::class.java)
     *         .jdbcUrl("jdbc:mysql://localhost:3306/notesapp")
     *         .jdbcCredentials("root", "password")
     *         .showSql(true, true, true)
     *         .createEntityManagerFactory()
     */
}