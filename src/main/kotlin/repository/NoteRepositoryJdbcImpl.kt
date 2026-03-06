package org.education.repository

import org.education.model.NOTE_TITLE_MAX_LENGTH
import org.education.model.NoteListItem
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

internal class NoteRepositoryJdbcImpl(ds: DataSource) : NoteRepository {
    val conn: Connection =
        ds.connection // DataSource is preferred over DriverManager: https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
    val stmt: Statement = conn.createStatement()

    fun dropNotesTable() = stmt.execute("DROP TABLE IF EXISTS notes;")

    override fun createNotesTableIfNotExists() {
        val tableNotesSql =
            "CREATE TABLE IF NOT EXISTS notes (id SERIAL PRIMARY KEY, creationDateTime DATETIME(6), lastEditedDateTime DATETIME(6), title VARCHAR($NOTE_TITLE_MAX_LENGTH), text TEXT, removed BIT(1) DEFAULT 0);"
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
            val noteId = stmt.generatedKeys.use { keys ->
                if (keys.next()) keys.getLong(1) else null
            }
            checkNotNull(noteId) { "Expected to insert a note and get its id, but got null" }
        }

    override fun updateNote(id: Long, title: String, text: String) {
        conn.prepareStatement("UPDATE notes SET lastEditedDateTime = ?, title = ?, text = ? WHERE id = ? AND removed = false;")
            .use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.setString(2, title)
                stmt.setString(3, text)
                stmt.setLong(4, id)
                val affectedInstances = stmt.executeUpdate()
                require(affectedInstances == 1) {
                    "Expected to update exactly 1 note with id=$id, but tried to update $affectedInstances notes"
                }
            }
    }

    override fun selectNote(id: Long): String =
        conn.prepareStatement("SELECT text FROM notes WHERE id = ? AND removed = false").use { stmt ->
            stmt.setLong(1, id)
            return stmt.executeQuery().use { res ->
                val results = mutableListOf<String>()
                while (res.next()) {
                    results.add(res.getString("text"))
                }
                require(results.size == 1) {
                    "Expected to find exactly 1 note with id=$id, but found ${results.size} notes"
                }
                results.first()
            }
        }

    override fun deleteNote(id: Long): Unit =
        conn.prepareStatement("UPDATE notes SET removed = true WHERE id = ? AND removed = false").use { stmt ->
            stmt.setLong(1, id)
            val affectedInstances = stmt.executeUpdate()
            require(affectedInstances == 1) {
                "Expected to delete exactly 1 note with id=$id, but tried to delete $affectedInstances notes"
            }
        }

    override fun loadAllNotes(): List<NoteListItem> =
        stmt.executeQuery("SELECT id, creationDateTime, lastEditedDateTime, title FROM notes WHERE removed = false ORDER BY lastEditedDateTime DESC, id DESC;")
            .use { res ->
                val noteListItems = mutableListOf<NoteListItem>()
                while (res.next()) {
                    noteListItems.add(
                        NoteListItem(
                            res.getLong("id"),
                            res.getString("title")
                        )
                    )
                }
                return noteListItems
            }
}