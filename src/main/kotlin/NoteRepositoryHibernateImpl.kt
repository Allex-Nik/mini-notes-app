package org.education

import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.time.Instant
import kotlin.use

// TODO: Use @CheckHQL and @NamedQuery for compile time query validation:
//  see 1.6 in https://docs.hibernate.org/orm/7.2/introduction/html_single/#organizing-persistence
// TODO: Create and inject a Queries repository (same link as above)
class NoteRepositoryHibernateImpl {
    val sessionFactory = buildSessionFactory()

    fun insertNote(title: String, text: String): Long { // TODO: rewrite with inTransaction/fromTransaction or custom withSession/withTransaction
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val note = Note(creationDateTime = Instant.now(), lastEditedDateTime = Instant.now(), title = title, text = text)
            session.persist(note)
            session.transaction.commit()
            return note.id ?: error("id was not generated")
        }
    }

    fun updateNote(id: Long, title: String, text: String) {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createMutationQuery("UPDATE Note SET lastEditedDateTime = :now, title = :title, text = :text WHERE id = :id")
                .setParameter("now", Instant.now())
                .setParameter("title", title)
                .setParameter("text", text)
                .setParameter("id", id)
                .executeUpdate()
            session.transaction.commit()
        }
    }

    fun selectNote(id: Long): String {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val note = session.createQuery("SELECT n.text FROM Note n WHERE n.id = :id", String::class.java)
                .setParameter("id", id)
                .uniqueResultOptional()
                .orElse("")
            session.transaction.commit()
            return note
        }
    }

    fun deleteNote(id: Long) {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            session.createMutationQuery("DELETE FROM Note n WHERE n.id = :id")
                .setParameter("id", id)
                .executeUpdate()
            session.transaction.commit()
        }
    }

    fun loadAllNotes(): List<NoteListItem> {
        sessionFactory.openSession().use { session ->
            session.beginTransaction()
            val noteListItems = session.createQuery("SELECT new org.education.NoteListItem(n.id, n.creationDateTime, n.lastEditedDateTime, n.title) FROM Note n",
                NoteListItem::class.java)
                .resultList
            session.transaction.commit()
            return noteListItems
        }
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