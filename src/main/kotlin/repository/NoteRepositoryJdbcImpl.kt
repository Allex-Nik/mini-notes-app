package org.education.repository

import org.education.ui.MAX_CHARACTERS
import org.education.model.NoteListItem
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

class NoteRepositoryJdbcImpl(ds: DataSource) : NoteRepository { // in some repos the name contains "DAO" - data access object
    val conn: Connection =
        ds.connection // DataSource is preferred over DriverManager: https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
    val stmt: Statement = conn.createStatement() // TODO: what if connection is closed?

    fun dropNotesTable() = stmt.execute("DROP TABLE IF EXISTS notes;")

    fun createNotesTableIfNotExists(): Boolean {
        val tableNotesSql =
            "CREATE TABLE IF NOT EXISTS notes (id SERIAL PRIMARY KEY, creationDateTime DATETIME, lastEditedDateTime DATETIME, title VARCHAR($MAX_CHARACTERS), text TEXT, removed BIT(1) DEFAULT 0);"
        return stmt.execute(tableNotesSql)
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

/**
 * To validate the length of the `title` column:
 * 1. Check if there are already existing violations: SELECT id, title FROM notes WHERE CHAR_LENGTH(title) > 60
 * If there are, decide what to do with them.
 * Shorten? UPDATE notes SET title = LEFT(title, 60) WHERE CHAR_LENGTH(title) > 60;
 *
 * 2. Change the type of the column: ALTER TABLE notes MODIFY title VARCHAR(60)
 * If we do this when there are violations in the table already, the operation will fail
 * (and the constraint will not be enforced for new entries)
 * OR
 * Add a constraint: ALTER TABLE notes ADD CONSTRAINT CHECK (CHAR_LENGTH(title) <= 60)
 * The same goes for this option.
 *
 * Then why does this work with Hibernate? When I do @field:Length(max = MAX_CHARACTERS) in Hibernate,
 * it enforces the constraint even if there were violations before adding the constraint.
 * This annotation doesn't change the schema by itself, the `update` option does. `update` tries to enforce
 * this requirement made by the validator. If there are violations already, `update` fails in this part,
 * but @field:Length(max = MAX_CHARACTERS) still works for the new rows because it is checked by the ORM
 * and not by the DB.
 *
 * So the only way to restrict the field on the DB level is to get rid of the existing violations first,
 * and then to change the schema.
 */