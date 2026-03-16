package org.education

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.mysql.cj.jdbc.MysqlDataSource
import org.education.mini_notes_app.generated.resources.Res
import org.education.mini_notes_app.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource
import org.education.repository.NoteRepositoryHibernateImpl
import org.education.service.NoteService
import org.education.ui.App
import org.education.ui.Theme
import org.education.ui.UIText

private const val HIBERNATE_CONFIGURATION_FILE_PROD = "hibernate/hibernate.cfg.xml"

fun main() {
    // `ds` is needed when switching to the JDBC repository
    val ds = MysqlDataSource().apply {
        serverName = "localhost"
        databaseName = "notesapp"
        user = System.getenv("MYSQL_USER")
        password = System.getenv("MYSQL_PASSWORD")
        description = "Notes App Database"
    }
    // replace with NoteRepositoryJdbcImpl(ds) if the JDBC repository is needed
    val noteRepository = NoteRepositoryHibernateImpl(HIBERNATE_CONFIGURATION_FILE_PROD)
    val noteService = NoteService(noteRepository)

    // uncomment the line below if the JDBC repository is needed
    // SchemaInitializerJdbc(ds).createNotesTableIfNotExists()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = UIText.FRAME_TITLE,
            icon = painterResource(Res.drawable.app_icon),
            state = WindowState(size = DpSize(Theme.FRAME_WIDTH.dp, Theme.FRAME_HEIGHT.dp))
        ) {
            App(noteService = noteService, onExit = ::exitApplication)
        }
    }
}
