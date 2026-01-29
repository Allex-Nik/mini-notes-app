package org.education

import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

class NoteRepository(ds: DataSource) { // in some repos the name contains "DAO" - data access object
    val conn: Connection =
        ds.connection // DataSource is preferred over DriverManager: https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
    val stmt: Statement = conn.createStatement() // TODO: what if connection is closed?

    fun dropNotesTable() = stmt.execute("DROP TABLE IF EXISTS notes;")

    fun createNotesTableIfNotExists(): Boolean {
        val tableNotesSql =
            "CREATE TABLE IF NOT EXISTS notes (id SERIAL PRIMARY KEY, creationDateTime DATETIME, title VARCHAR(255), text TEXT);" // MEDIUMTEXT, LONGTEXT
        return stmt.execute(tableNotesSql)
    }

    fun insertNote(title: String, text: String) =
        conn.prepareStatement("INSERT INTO notes (creationDateTime, title, text) VALUES (?, ?, ?);").use { stmt ->
            stmt.setTimestamp(1, Timestamp.from(Instant.now()))
            stmt.setString(2, title)
            stmt.setString(3, text)
            stmt.executeUpdate()
        }

    fun selectNote(id: Long): String = conn.prepareStatement("SELECT text FROM notes WHERE id = ?").use { stmt ->
        stmt.setLong(1, id)
        return stmt.executeQuery().use { res ->
            if (res.next()) res.getString("text") else ""
        }
    }

    fun deleteNote(id: Long) = conn.prepareStatement("DELETE FROM notes WHERE id = ?").use { stmt ->
        stmt.setLong(1, id)
        stmt.executeUpdate()
    }

    fun loadAllNotes(): List<Note> = stmt.executeQuery("SELECT id, creationDateTime, title FROM notes;").use { res ->
        val notes = mutableListOf<Note>()
        while (res.next()) {
            notes.add(
                Note(
                    res.getLong("id"),
                    res.getTimestamp("creationDateTime")?.toInstant(),
                    res.getString("title")
                )
            )
        }
        return notes
    }
}