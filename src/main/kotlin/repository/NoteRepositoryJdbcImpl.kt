package org.education.repository

import org.education.ui.MAX_CHARACTERS
import org.education.model.NoteListItem
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource


/**
 * REVIEW:
 *
 * 1. Connection and Statement are stored as fields and never closed.
 *    Explanation:
 *      val conn: Connection = ds.connection
 *      val stmt: Statement = conn.createStatement()
 *
 *      These are opened once and never closed.
 *      This is a real resource-leak problem.
 *
 *      Repository should not keep a long-lived Connection.
 *      Each method should obtain a connection via ds.connection.use { ... }.
 *
 *
 * 2. No transaction handling at all.
 *    Explanation:
 *      Unlike Hibernate implementation (which wraps everything in transactions),
 *      JDBC version does not explicitly manage transactions.
 *
 *      It relies on default autoCommit behavior.
 *      This creates behavioral inconsistency between implementations.
 *
 *      If autoCommit is disabled in DataSource configuration,
 *      this code will silently break.
 *
 *
 * 3. updateNote does not check affected row count.
 *    Explanation:
 *      stmt.executeUpdate() result is ignored.
 *
 *      If id does not exist, zero rows are updated and no error is thrown.
 *      Hibernate implementation enforces affectedInstances == 1.
 *
 *      This is a real behavioral inconsistency.
 *
 *
 * 4. deleteNote does not enforce single-row invariant.
 *    Explanation:
 *      stmt.executeUpdate() return value is returned,
 *      but neither repository nor service validates it.
 *
 *      Hibernate version enforces exactly one row affected.
 *      JDBC version does not.
 *
 *
 * 5. selectNote returns empty string when not found.
 *    Explanation:
 *      if (res.next()) res.getString("text") else ""
 *
 *      This silently hides missing records.
 *      Hibernate implementation likely throws if not found.
 *
 *      Returning "" makes absence indistinguishable from an empty note.
 *
 *
 * 6. Global Statement used in loadAllNotes().
 *    Explanation:
 *      stmt.executeQuery(...) uses shared Statement instance.
 *
 *      Statement is not thread-safe.
 *      Even in single-thread UI app, keeping global Statement is bad practice.
 *
 *      Always use prepareStatement(...).use { } instead.
 *
 *
 * 7. Hardcoded SQL strings (magic strings).
 *    Explanation:
 *      SQL queries are inline literals.
 *      They should be private const val at top of class.
 *
 *      This improves readability and maintainability.
 *
 *
 * 8. Missing ORDER BY in loadAllNotes().
 *    Explanation:
 *      SELECT ... WHERE removed = false;
 *
 *      No ORDER BY clause.
 *      Result order is undefined.
 *      UI depends on deterministic order.
 *
 *
 * 9. Throwing generic Exception in insertNote.
 *     Explanation:
 *       throw Exception("Failed to insert a new note")
 *
 *       Never throw raw Exception.
 *       Use IllegalStateException or a domain-specific exception.
 *
 *
 * 10. Class properties should be private.
 *     Explanation:
 *       conn and stmt are public by default.
 *       They must be private to avoid external misuse.
 *
 *
 * 11. No defensive handling of nullable timestamps.
 *     Explanation:
 *       res.getTimestamp(...)?.
 *
 *       If DB schema guarantees NOT NULL,
 *       nullability should not be silently propagated.
 */
class NoteRepositoryJdbcImpl(ds: DataSource) : NoteRepository { // in some repos the name contains "DAO" - data access object
    val conn: Connection =
        ds.connection // DataSource is preferred over DriverManager: https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
    val stmt: Statement = conn.createStatement()

    fun dropNotesTable() = stmt.execute("DROP TABLE IF EXISTS notes;")

    override fun createNotesTableIfNotExists() {
        val tableNotesSql =
            "CREATE TABLE IF NOT EXISTS notes (id SERIAL PRIMARY KEY, creationDateTime DATETIME, lastEditedDateTime DATETIME, title VARCHAR($MAX_CHARACTERS), text TEXT, removed BIT(1) DEFAULT 0);"
        stmt.execute(tableNotesSql)
    }

    override fun insertNote(title: String, text: String) =
        conn.prepareStatement(
            "INSERT INTO notes (creationDateTime, lastEditedDateTime, title, text, removed) VALUES (?, ?, ?, ?, ?);",
            Statement.RETURN_GENERATED_KEYS
        ).use { stmt ->
            val now = Timestamp.from(Instant.now())
            stmt.setTimestamp(1, now)
            stmt.setTimestamp(2, now)
            stmt.setString(3, title)
            stmt.setString(4, text)
            stmt.setBoolean(5, false)
            stmt.executeUpdate()
            stmt.generatedKeys.use { keys -> if (keys.next()) keys.getLong(1) else throw Exception("Failed to insert a new note") }
        }

    override fun updateNote(id: Long, title: String, text: String) {
        conn.prepareStatement("UPDATE notes SET lastEditedDateTime = ?, title = ?, text = ? WHERE id = ?").use { stmt ->
            stmt.setTimestamp(1, Timestamp.from(Instant.now()))
            stmt.setString(2, title)
            stmt.setString(3, text)
            stmt.setLong(4, id)
            stmt.executeUpdate()
        }
    }

    override fun selectNote(id: Long): String = conn.prepareStatement("SELECT text FROM notes WHERE id = ?").use { stmt ->
        stmt.setLong(1, id)
        return stmt.executeQuery().use { res ->
            if (res.next()) res.getString("text") else ""
        }
    }

    override fun deleteNote(id: Long) = conn.prepareStatement("UPDATE notes SET removed = true WHERE id = ?").use { stmt ->
        stmt.setLong(1, id)
        stmt.executeUpdate()
    }

    override fun loadAllNotes(): List<NoteListItem> = stmt.executeQuery("SELECT id, creationDateTime, lastEditedDateTime, title FROM notes WHERE removed = false;").use { res ->
        val noteListItems = mutableListOf<NoteListItem>()
        while (res.next()) {
            noteListItems.add(
                NoteListItem(
                    res.getLong("id"),
                    res.getTimestamp("creationDateTime")?.toInstant(),
                    res.getTimestamp("lastEditedDateTime")?.toInstant(),
                    res.getString("title")
                )
            )
        }
        return noteListItems
    }
}