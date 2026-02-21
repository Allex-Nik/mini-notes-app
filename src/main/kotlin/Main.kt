package org.education

import com.mysql.cj.jdbc.MysqlDataSource
import org.education.repository.NoteRepositoryHibernateImpl
import org.education.service.NoteService
import org.education.ui.MainWindow
import javax.swing.*

fun main() {
    val ds = MysqlDataSource().apply {
        serverName = "localhost" // find out about Unix socket and TCP
        databaseName = "notesapp"
        user = System.getenv("MYSQL_USER")
        password = System.getenv("MYSQL_PASSWORD")
        description = "Notes App Database"
    }
    val noteRepository = NoteRepositoryHibernateImpl("hibernate/hibernate.cfg.xml") // NoteRepositoryJdbcImpl(ds)
    val noteService = NoteService(noteRepository)

    SwingUtilities.invokeLater {
        val frame = MainWindow(noteService) // dependency injection (DI)
        frame.isVisible = true
    }
}
