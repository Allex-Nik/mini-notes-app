package org.education

import com.mysql.cj.jdbc.MysqlDataSource
import org.education.repository.NoteRepositoryHibernateImpl
import org.education.service.NoteService
import org.education.ui.MainWindow
import javax.swing.SwingUtilities

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
    val noteRepository = NoteRepositoryHibernateImpl("hibernate/hibernate.cfg.xml")
    val noteService = NoteService(noteRepository)

    SwingUtilities.invokeLater {
        val frame = MainWindow(noteService)
        frame.isVisible = true
    }
}
