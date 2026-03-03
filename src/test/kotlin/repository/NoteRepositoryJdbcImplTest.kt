package repository

import com.mysql.cj.jdbc.MysqlDataSource
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation
import org.education.model.NOTE_TITLE_MAX_LENGTH
import org.education.repository.NoteRepositoryJdbcImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class NoteRepositoryJdbcImplTest : NoteRepositoryTest() {
    private val dataSource = MysqlDataSource()
        .apply {
            serverName = "localhost"
            databaseName = "test"
            user = System.getenv("MYSQL_TEST_USER")
            password = System.getenv("MYSQL_TEST_PASSWORD")
            description = "Testing Database"
        }
    override val noteRepository = NoteRepositoryJdbcImpl(dataSource)

    @BeforeEach
    override fun emptyNotesTable() {
        noteRepository.dropNotesTable()
        noteRepository.createNotesTableIfNotExists()
    }

    @AfterAll
    override fun closeConnection() {
        noteRepository.conn.close()
    }

    @Test
    override fun `insert note with too long title`() {
        val title = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)
        val text = "Text"

        assertThrows<MysqlDataTruncation> { noteRepository.insertNote(title, text) }
    }

    @Test
    override fun `updateNote with too long title`() {
        val noteId = noteRepository.insertNote("Old title", "Old text")
        val newTitle = "A".repeat(NOTE_TITLE_MAX_LENGTH + 5)

        assertThrows<MysqlDataTruncation> {
            noteRepository.updateNote(noteId, newTitle, "New Text")
        }
    }
}