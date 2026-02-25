package org.education.repository

import org.education.model.Note
import org.education.model.NoteListItem
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import java.time.Instant


/**
 * REVIEW:
 *
 * 1. Do not use error(...) for business invariants.
 *    Explanation:
 *      error() throws IllegalStateException and is typically used
 *      for programming errors, not domain constraints.
 *
 *      Here you check that exactly one row is affected.
 *      Use require(affectedInstances == 1) with a clear message instead.
 *      It makes intent explicit and more idiomatic.
 *
 *  ```
 *  require(affectedInstances == 1) {
 *     "Expected to affect exactly 1 row, but affected $affectedInstances"
 *  }
 * ```
 *
 * 2. Avoid hardcoded HQL strings inside methods.
 *    Explanation:
 *      The constructor query:
 *      "SELECT new org.education.model.NoteListItem(...)"
 *      contains a fully qualified class name as a string.
 *
 *      If the package name changes, this breaks at runtime.
 *      Extract the query into a private const val at the top of the class.
 *
 *
 * 3. loadAllNotes() has no ORDER BY.
 *    Explanation:
 *      UI (MainWindow.readNotes) displays results as-is.
 *      Without ORDER BY, database result order is undefined.
 *
 *      Always define explicit ordering in repository queries.
 *
 * 4. Align soft-delete behavior across methods.
 *    Explanation:
 *      loadAllNotes() filters removed = false.
 *      Other methods (selectNote, updateNote) must respect the same rule.
 *
 *      Otherwise, a "deleted" note may still be selectable or updatable.
 *
 *
 * 5. Avoid exposing unnecessary public functions.
 *    Explanation:
 *      buildSessionFactory(...) is public by default (top-level function).
 *      If used only inside this module, mark it internal.
 *
 *
 * 6. Keep exception behavior consistent with JDBC implementation.
 *    Explanation:
 *      Hibernate throws runtime exceptions.
 *      Make sure no exceptions are swallowed and that both implementations
 *      behave the same way in error cases.
 *
 *    Especially, related to the number of affected rows.
 *
 *
 * 7. Extract magic strings and repeated values.
 *    Explanation:
 *      Query strings and configuration file names should be constants.
 *      This improves maintainability and reduces copy-paste errors.
 */
class NoteRepositoryHibernateImpl(configurationFile: String) : NoteRepository {
    val sessionFactory = buildSessionFactory(configurationFile)

    // done by Hibernate automatically
    override fun createNotesTableIfNotExists() {}

    // https://docs.hibernate.org/orm/7.2/introduction/html_single/#managing-transactions
    override fun insertNote(title: String, text: String): Long =
        sessionFactory.fromTransaction { session ->
            val note = Note(
                creationDateTime = Instant.now(),
                lastEditedDateTime = Instant.now(),
                title = title,
                text = text,
                removed = false
            )
            session.persist(note)
            note.id ?: error("id was not generated")
        }

    override fun updateNote(id: Long, title: String, text: String) =
        sessionFactory.inTransaction { session ->
            session.createMutationQuery(
                "UPDATE Note SET lastEditedDateTime = :now, title = :title, text = :text WHERE id = :id"
            )
                .setParameter("now", Instant.now())
                .setParameter("title", title)
                .setParameter("text", text)
                .setParameter("id", id)
                .executeUpdate()
        }

    override fun selectNote(id: Long): String =
        sessionFactory.fromTransaction { session ->
            session.createQuery(
                "SELECT n.text FROM Note n WHERE n.id = :id",
                String::class.java
            )
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