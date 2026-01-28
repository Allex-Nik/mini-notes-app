package org.education

import java.sql.Connection
import java.sql.Statement
import javax.sql.DataSource

class NoteRepository(ds: DataSource) { // in some repos the name contains "DAO" - data access object
    val conn: Connection = ds.connection // DataSource is preferred over DriverManager: https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
    val stmt: Statement = conn.createStatement() // TODO: what if connection is closed?

    fun dropNotesTable() = stmt.execute("DROP TABLE IF EXISTS notes;") // TODO: Exceptions?

    fun createNotesTable(): Boolean {
        val tableNotesSql =
            "CREATE TABLE IF NOT EXISTS notes (id SERIAL PRIMARY KEY, title VARCHAR(255), text TEXT);" // MEDIUMTEXT, LONGTEXT
        return stmt.execute(tableNotesSql)
    }

    fun insertNote(title: String, text: String) =
        conn.prepareStatement("INSERT INTO notes (title, text) VALUES (?, ?);").use { stmt ->
            stmt.setString(1, title)
            stmt.setString(2, text)
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

    fun loadAllNotes(): List<Note> = stmt.executeQuery("SELECT id, title FROM notes;").use { res ->
        val notes = mutableListOf<Note>()
        while (res.next()) {
            notes.add(Note(res.getLong("id"), res.getString("title")))
        }
        return notes
    }
}