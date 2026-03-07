package service

import com.mysql.cj.jdbc.MysqlDataSource
import org.education.model.Note
import org.education.repository.NoteRepositoryJdbcImpl
import org.education.schema.SchemaInitializerJdbc
import org.education.service.NoteService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class NoteServiceAndJdbcRepositoryTest : NoteServiceAndRepositoryTest() {
    private val dataSource = MysqlDataSource()
        .apply {
            serverName = "localhost"
            databaseName = "test"
            user = System.getenv("MYSQL_TEST_USER")
            password = System.getenv("MYSQL_TEST_PASSWORD")
            description = "Testing Database"
        }
    override val noteRepository = NoteRepositoryJdbcImpl(dataSource)
    override val noteService = NoteService(noteRepository)
    private val schemaInitializer = SchemaInitializerJdbc(dataSource)

    @BeforeEach
    override fun emptyNotesTable() {
        schemaInitializer.dropNotesTableIfExists()
        schemaInitializer.createNotesTableIfNotExists()
    }

    // it is closed in every method where it is open
    @AfterAll
    override fun closeConnection() {
    }

    @Test
    fun createNote() {
        val id = noteService.saveNote(null, "Test note", "Test text")
        val retrieveNoteSql = "SELECT * FROM notes WHERE id = ?"
        val savedNote = dataSource.connection.use { conn ->
            conn.prepareStatement(retrieveNoteSql).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeQuery()
                    .use { result ->
                        result.next()
                        Note().apply {
                            this.id = result.getLong("id")
                            this.creationDateTime = result.getTimestamp("creationDateTime").toInstant()
                            this.lastEditedDateTime = result.getTimestamp("lastEditedDateTime").toInstant()
                            this.title = result.getString("title")
                            this.text = result.getString("text")
                            this.removed = result.getBoolean("removed")
                        }
                    }
            }
        }
        assertEquals("Test text", savedNote.text)
        assertEquals("Test note", savedNote.title)
        assertEquals(false, savedNote.removed)
    }
}