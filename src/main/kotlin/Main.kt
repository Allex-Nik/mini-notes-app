package org.education

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Taskbar
import java.io.File
import java.nio.file.Files
import javax.swing.*
import kotlin.io.path.Path
import kotlin.io.path.exists

private const val NOTES_DIR = "notes"
private const val EMPTY_NOTE_NAME = "empty_note"
private const val TXT_EXTENSION = ".txt"
private const val FRAME_TITLE = "Notes"

class MainWindow(title: String) : JFrame() { // BorderLayout by default
    private val saveButton = JButton("Save")
    private val loadButton = JButton("Load")
    private val buttonPanel = JPanel() // FlowLayout by default
    private val textArea = JTextArea()
    private val savedNotes = mutableListOf<String>()
    private val savedNotesPanel = JPanel() // FlowLayout by default
    private var savedNotesList = JList(savedNotes.toTypedArray())
    private val savedNotesLabel = JLabel("     Saved Notes     ")

    init {
        createFrame(title)
        this.add(createToolPanel(), BorderLayout.WEST)
        this.add(textArea, BorderLayout.CENTER)
        addListeners()
    }

    private fun createFrame(title: String) {
        this.title = title
        this.setSize(1280, 820)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.layout = BorderLayout(10, 10)
        this.contentPane.background = Color(255, 255, 255)
        setIcons()
    }

    private fun setIcons() {
        val imageAddress = this.javaClass.getResource("/icon.png") ?: return
        val image = ImageIcon(imageAddress)
        this.iconImage = image.image

        if (Taskbar.isTaskbarSupported()) {
            Taskbar.getTaskbar().iconImage = image.image
        }
    }

    private fun createToolPanel(): JPanel {
        buttonPanel.background = Color(255, 229, 204)
        buttonPanel.preferredSize = Dimension(120, 100)

        // add buttons
        saveButton.preferredSize = Dimension(90, 50)
        loadButton.preferredSize = Dimension(90, 50)
        buttonPanel.add(saveButton)
        buttonPanel.add(loadButton)

        // add saved notes
        readNotes(File(NOTES_DIR))
        updateSavedNotesList()
        savedNotesPanel.add(savedNotesList)
        savedNotesPanel.preferredSize = Dimension(150, 500)
        savedNotesPanel.background = Color(255, 229, 204)
        buttonPanel.add(savedNotesPanel)

        return buttonPanel
    }

    private fun addListeners() {
        addSaveButtonListener()
        addLoadButtonListener()
        addNotesSelectionListener()
    }

    private fun addSaveButtonListener() = saveButton.addActionListener {
        val note = textArea.text

        // take the first word of the note, remove punctuation
        var fileName = note
            .substringBefore(' ')
            .trim { !it.isLetterOrDigit() }

        // limit the word to 15 characters
        if (fileName.length > 15) fileName = fileName.take(15)

        if (fileName.isEmpty()) fileName = EMPTY_NOTE_NAME

        // add an integer suffix if the file with this name already exists
        var end = 1
        while (Path("$NOTES_DIR/$fileName$TXT_EXTENSION").exists()) {
            if (fileName.endsWith("_${end - 1}")) {
                fileName = fileName.removeSuffix("_${end - 1}")
            }
            fileName += "_$end"
            end++
        }

        fileName += TXT_EXTENSION

        Files.createDirectories(Path(NOTES_DIR))
        File("$NOTES_DIR/$fileName").writeText(note)

        readNotes(File(NOTES_DIR))
        updateSavedNotesList()
    }

    private fun addLoadButtonListener() = loadButton.addActionListener {
        readNotes(File(NOTES_DIR))
        updateSavedNotesList()
    }

    private fun addNotesSelectionListener() = savedNotesList.addListSelectionListener {
        // setListData in updateSavedNotesList removes selection and triggers this listener
        // which leads to savedNotesList.selectedValue == null
        if (savedNotesList.isSelectionEmpty) return@addListSelectionListener
        val selectedNote = savedNotesList.selectedValue
        textArea.text = File("$NOTES_DIR/$selectedNote").readText()
    }

    private fun readNotes(dir: File) {
        savedNotes.clear()
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(TXT_EXTENSION)) {
                    savedNotes.add(file.name)
                }
            }
        }
    }

    private fun updateSavedNotesList() {
        savedNotesPanel.removeAll() // removes nested components
        savedNotesPanel.add(savedNotesLabel)
        savedNotesList.setListData(savedNotes.toTypedArray())
        savedNotesPanel.add(savedNotesList)
        savedNotesPanel.revalidate()
        savedNotesPanel.repaint()
    }
}

fun main() {
    SwingUtilities.invokeLater {
        val frame = MainWindow(FRAME_TITLE)
        frame.isVisible = true
    }
}
