package org.education.repository

import org.education.exceptions.*
import org.education.model.NoteListItem
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

internal class NoteRepositoryJdbcImpl(private val ds: DataSource) : NoteRepository {
    companion object {
        private const val INSERT_NOTE_QUERY = """
            INSERT INTO notes (creationDateTime, lastEditedDateTime, title, text, removed) VALUES (?, ?, ?, ?, ?);
            """

        private const val UPDATE_NOTE_QUERY = """
            UPDATE notes SET lastEditedDateTime = ?, title = ?, text = ? WHERE id = ? AND removed = false;
            """

        private const val SELECT_NOTE_QUERY = "SELECT text FROM notes WHERE id = ? AND removed = false"

        private const val DELETE_NOTE_QUERY = "UPDATE notes SET removed = true WHERE id = ? AND removed = false"

        private const val LOAD_ALL_NOTES_QUERY = """
            SELECT id, creationDateTime, lastEditedDateTime, title 
            FROM notes 
            WHERE removed = false 
            ORDER BY lastEditedDateTime DESC, id DESC;
            """
    }

    override fun insertNote(title: String, text: String) =
        ds.connection.use { conn ->
            conn.prepareStatement(
                INSERT_NOTE_QUERY,
                Statement.RETURN_GENERATED_KEYS
            ).use { stmt ->
                val now = Timestamp.from(Instant.now())
                stmt.setTimestamp(1, now)
                stmt.setTimestamp(2, now)
                stmt.setString(3, title)
                stmt.setString(4, text)
                stmt.setBoolean(5, false)
                stmt.executeUpdate()
                stmt.generatedKeys.use { keys ->
                    if (keys.next()) keys.getLong(1) else throw NoteNotInsertedException()
                }
            }
        }

    override fun updateNote(id: Long, title: String, text: String) {
        ds.connection.use { conn ->
            conn.prepareStatement(UPDATE_NOTE_QUERY).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.setString(2, title)
                stmt.setString(3, text)
                stmt.setLong(4, id)
                val affectedInstances = stmt.executeUpdate()
                if (affectedInstances == 0) throw NoteNotFoundException()
                if (affectedInstances > 1) throw MultipleRowsAffectedException()
            }
        }
    }

    override fun selectNote(id: Long): String =
        ds.connection.use { conn ->
            conn.prepareStatement(SELECT_NOTE_QUERY).use { stmt ->
                stmt.setLong(1, id)
                return stmt.executeQuery().use { res ->
                    if (!res.next()) throw NoteNotFoundException()
                    val noteText = res.getString("text")
                    if (res.next()) throw NonUniqueNoteException()
                    noteText
                }
            }
        }

    override fun deleteNote(id: Long): Unit =
        ds.connection.use { conn ->
            conn.prepareStatement(DELETE_NOTE_QUERY).use { stmt ->
                stmt.setLong(1, id)
                val affectedInstances = stmt.executeUpdate()
                if (affectedInstances == 0) throw NoteNotDeletedException()
                if (affectedInstances > 1) throw MultipleRowsAffectedException()
            }
        }

    override fun loadAllNotes(): List<NoteListItem> =
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery(LOAD_ALL_NOTES_QUERY).use { res ->
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
        }
}