package org.education.schema

import org.education.model.NOTE_TITLE_MAX_LENGTH
import javax.sql.DataSource

class SchemaInitializerJdbc(private val ds: DataSource) {
    private val createNotesTableQuery = """
        CREATE TABLE IF NOT EXISTS notes (
        id BIGINT AUTO_INCREMENT PRIMARY KEY, 
        creationDateTime DATETIME(6) NOT NULL, 
        lastEditedDateTime DATETIME(6) NOT NULL, 
        title VARCHAR($NOTE_TITLE_MAX_LENGTH) NOT NULL, 
        text TEXT NOT NULL, 
        removed BIT(1) NOT NULL DEFAULT 0
        );
        """.trimIndent()

    private val dropNotesTableQuery = "DROP TABLE IF EXISTS notes;"

    fun createNotesTableIfNotExists() =
        ds.connection.use { conn ->
            conn.prepareStatement(createNotesTableQuery).use { stmt ->
                stmt.execute()
            }
        }

    fun dropNotesTableIfExists() =
        ds.connection.use { conn ->
            conn.prepareStatement(dropNotesTableQuery).use { stmt ->
                stmt.execute()
            }
        }
}