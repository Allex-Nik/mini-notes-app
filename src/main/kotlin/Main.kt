package org.education

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
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

class MainWindow : JFrame() { // BorderLayout by default
    private val saveButton = JButton("Save")
    private val loadButton = JButton("Load")
    private val leftPanel = JPanel() // FlowLayout by default
    private val textArea = JTextArea()
    private val savedNotes = mutableListOf<String>()
    private val savedNotesPanel = JPanel() // FlowLayout by default
    private var savedNotesList = JList(savedNotes.toTypedArray())
    // check available fonts: val end = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
    private val savedNotesLabel = JLabel("        Saved Notes     ").apply {
        font = Font("Arial", Font.PLAIN, 18)
    }
    private lateinit var savedNotesScrollPane: JScrollPane

    init {
        configureFrame()
        configureLeftPanel()
        configureTextArea()
        addListeners()
    }

    private fun configureFrame() {
        this.title = FRAME_TITLE
        this.setSize(1280, 820)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.layout = BorderLayout()
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

    private fun configureLeftPanel() {
        leftPanel.layout = BorderLayout()
        leftPanel.preferredSize = Dimension(200, 800)

        createButtonsPanel()
        configureSavedNotesPanel()

        this.add(leftPanel, BorderLayout.WEST)
    }

    private fun createButtonsPanel() {
        val buttonsPanel = JPanel().apply {
            layout = BorderLayout()
            border = BorderFactory.createEmptyBorder(40, 10, 20, 10)
            background = Color(244, 231, 207)
        }

        saveButton.apply {
            preferredSize = Dimension(90, 50)
            toolTipText = """Saves the current note to the folder "$NOTES_DIR" on your computer"""
            font = Font("Arial", Font.PLAIN, 18)
        }

        loadButton.apply {
            preferredSize = Dimension(90, 50)
            toolTipText = "Refreshes the list of saved notes"
            font = Font("Arial", Font.PLAIN, 18)
        }

        buttonsPanel.add(saveButton, BorderLayout.NORTH)
        // make buttons further from each other to avoid misclicks
        buttonsPanel.add(Box.createRigidArea(Dimension(0, 10)), BorderLayout.CENTER)
        buttonsPanel.add(loadButton, BorderLayout.SOUTH)

        leftPanel.add(buttonsPanel, BorderLayout.NORTH)
    }

    private fun configureSavedNotesPanel() {
        savedNotesPanel.apply {
            layout = BorderLayout()
            background = Color(244, 231, 207)
        }

        savedNotesList.apply {
            background = Color(244, 231, 207)
            selectionBackground = Color.WHITE
            selectionForeground = Color.BLACK
            toolTipText = "Click on a note to view it"
            font = Font("Arial", Font.PLAIN, 14)
        }

        readNotes(File(NOTES_DIR))
        updateSavedNotesList()

        leftPanel.add(savedNotesPanel, BorderLayout.CENTER)
    }

    private fun configureTextArea() {
        textArea.background = Color(255, 247, 232)
        val textScrollPane = JScrollPane(textArea)
        this.add(textScrollPane, BorderLayout.CENTER)
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
        // which leads to savedNotesList.selectedValue == null and thus an exception
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
        savedNotesPanel.removeAll()
        savedNotesPanel.add(savedNotesLabel, BorderLayout.NORTH)
        savedNotesList.setListData(savedNotes.toTypedArray())
        savedNotesPanel.add(Box.createRigidArea(Dimension(0, 20)), BorderLayout.CENTER)
        savedNotesPanel.add(savedNotesList, BorderLayout.CENTER)
        savedNotesScrollPane = JScrollPane(savedNotesList)
        savedNotesPanel.add(savedNotesScrollPane)
        savedNotesPanel.revalidate()
        savedNotesPanel.repaint()
    }
}

fun main() {
    SwingUtilities.invokeLater {
        val frame = MainWindow()
        frame.isVisible = true
    }
}
