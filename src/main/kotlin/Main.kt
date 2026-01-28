package org.education

import com.mysql.cj.jdbc.MysqlDataSource
import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import kotlin.system.exitProcess

class MainWindow(val noteRepository: NoteRepository) : JFrame() { // BorderLayout by default
    private val saveButton = JButton(UIText.SAVE_BUTTON_TITLE)
    private val loadButton = JButton(UIText.LOAD_BUTTON_TITLE)
    private val removeButton = JButton(UIText.REMOVE_BUTTON_TITLE)
    private val leftPanel = JPanel() // FlowLayout by default
    private val centralPanel = JPanel().apply { layout = BorderLayout() }
    private val header = JTextField(UIText.START_HEADER_TEXT)
    private val textArea = JTextArea()
    private val savedNotes = mutableListOf<Note>()
    private val savedNotesPanel = JPanel() // FlowLayout by default
    private var savedNotesList = JList(savedNotes.toTypedArray())
    private val savedNotesLabel = JLabel(UIText.SAVED_NOTES_LABEL, SwingConstants.CENTER)
        .apply { font = Theme.savedNotesLabelFont }
    private lateinit var savedNotesScrollPane: JScrollPane
    private val autosaveCheckbox = JCheckBox(UIText.AUTOSAVE_CHECKBOX_TITLE, false)
    private val saveShortcut: KeyStroke? = KeyStroke.getKeyStroke(UIText.SAVE_SHORTCUT)
    private val newNoteShortcut: KeyStroke? = KeyStroke.getKeyStroke(UIText.NEW_NOTE_SHORTCUT)
    private val statusLabel = JLabel(UIText.READY_LABEL)
        .apply { border = Theme.statusPadding }

    init {
//        noteRepository.dropNotesTable() // left for development
        noteRepository.createNotesTable()
        configureFrame()
        configureLeftPanel()
        configureTextArea()
        configureMenuBar()
        addListeners()
    }

    private fun configureFrame() {
        this.title = UIText.FRAME_TITLE
        this.setSize(Theme.FRAME_WIDTH, Theme.FRAME_HEIGHT)
        this.defaultCloseOperation = EXIT_ON_CLOSE
        this.layout = BorderLayout()
        this.add(statusLabel, BorderLayout.SOUTH)
        setAppIcon()
    }

    private fun setAppIcon() {
        val appImageAddress = this.javaClass.getResource(Icons.APP_ICON) ?: return
        val appImage = ImageIcon(appImageAddress)
        this.iconImage = appImage.image

        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.iconImage = appImage.image
            }
        }
    }

    private fun configureLeftPanel() {
        leftPanel.layout = BorderLayout()
        leftPanel.preferredSize = Theme.leftPanelSize

        createButtonsPanel()
        configureSavedNotesPanel()

        this.add(leftPanel, BorderLayout.WEST)
    }

    private fun createButtonsPanel() {
        val buttonsPanel = JPanel().apply {
            layout = BorderLayout()
            border = Theme.buttonsPadding
            background = Theme.buttonsPanelColor
        }

        saveButton.apply {
            preferredSize = Theme.saveButtonSize
            toolTipText = UIText.SAVE_BUTTON_TOOLTIP
            font = Theme.saveButtonFont
        }

        loadButton.apply {
            preferredSize = Theme.loadButtonSize
            toolTipText = UIText.LOAD_BUTTON_TOOLTIP
            font = Theme.loadButtonFont
        }

        removeButton.apply {
            preferredSize = Theme.removeButtonSize
            toolTipText = UIText.REMOVE_BUTTON_TOOLTIP
            font = Theme.removeButtonFont
        }

        buttonsPanel.add(saveButton, BorderLayout.NORTH)
        buttonsPanel.add(loadButton, BorderLayout.CENTER)
        buttonsPanel.add(removeButton, BorderLayout.SOUTH)

        leftPanel.add(buttonsPanel, BorderLayout.NORTH)
    }

    private fun configureSavedNotesPanel() {
        savedNotesPanel.apply {
            layout = BorderLayout()
            background = Theme.savedNotesPanelColor
        }

        savedNotesList.apply {
            background = Theme.savedNotesListColor
            selectionBackground = Theme.selectBackground
            selectionForeground = Theme.selectForeground
            toolTipText = UIText.SAVED_NOTES_LIST_TOOLTIP
            font = Theme.savedNotesListFont
        }

        readNotes()
        updateSavedNotesList()

        leftPanel.add(savedNotesPanel, BorderLayout.CENTER)
    }

    private fun configureTextArea() {
        textArea.apply {
            background = Theme.textAreaColor
            // ctrl N - in JTextArea is going to the next line
            // cancel the default behavior for JTextArea when it is in focus
            getInputMap(JComponent.WHEN_FOCUSED).put(newNoteShortcut, "none")
            lineWrap = true
            wrapStyleWord = true
        }

        header.apply {
            font = Theme.noteHeaderFont
            horizontalAlignment = JTextField.CENTER
            background = Theme.textAreaColor
        }
        val document = header.document as AbstractDocument
        document.documentFilter = HeaderLengthFilter()

        val textScrollPane = JScrollPane(textArea)
        centralPanel.apply {
            background = Theme.textAreaColor
            add(header, BorderLayout.NORTH)
            add(textScrollPane, BorderLayout.CENTER)
        }
        this.add(centralPanel, BorderLayout.CENTER)
    }

    private fun configureMenuBar() {
        val menuBar = JMenuBar()
        val menu = JMenu(UIText.MAIN_MENU_TITLE)
        menuBar.add(menu)

        // new note menu item
        val newNoteItem = JMenuItem(UIText.NEW_NOTE_TITLE).apply {
            accelerator = newNoteShortcut

            val newNoteImage = ImageIcon(this@MainWindow.javaClass.getResource(Icons.NEW_NOTE_ICON))
                .image
            val newNoteImageScaled = newNoteImage
                .getScaledInstance(Theme.MENU_ICON_WIDTH, Theme.MENU_ICON_HEIGHT, Image.SCALE_SMOOTH)
            icon = ImageIcon(newNoteImageScaled)

            addActionListener {
                // if the checkbox is selected, the confirmedSaveNote() is not evaluated and the dialog is not shown
                if (autosaveCheckbox.isSelected || confirmedSaveNote()) saveNote()
                header.text = UIText.START_HEADER_TEXT
                textArea.text = ""
                savedNotesList.clearSelection()
                statusLabel.text = UIText.NOTE_CREATED_MESSAGE
            }
        }

        // save note menu item
        val saveNotePic = ImageIcon(this.javaClass.getResource(Icons.SAVE_NOTE_ICON))
            .image.getScaledInstance(Theme.MENU_ICON_WIDTH, Theme.MENU_ICON_HEIGHT, Image.SCALE_SMOOTH)
        val saveNoteItem = JMenuItem(UIText.SAVE_TITLE, ImageIcon(saveNotePic))
        saveNoteItem.accelerator = saveShortcut
        saveNoteItem.addActionListener { if (confirmedSaveNote()) saveNote() }

        // exit menu item
        val exitPic = ImageIcon(this.javaClass.getResource(Icons.EXIT_ICON))
            .image.getScaledInstance(Theme.MENU_ICON_WIDTH, Theme.MENU_ICON_HEIGHT, Image.SCALE_SMOOTH)
        val exitItem = JMenuItem(UIText.EXIT_TITLE, ImageIcon(exitPic)) // TODO: Add a shortcut
        exitItem.addActionListener {
            if (!confirmedExit()) return@addActionListener
            if (autosaveCheckbox.isSelected) saveNote()
            exitProcess(0)
        }

        menu.add(newNoteItem)
        menu.add(saveNoteItem)
        menu.add(exitItem)

        val menuAutosave = JMenu(UIText.AUTOSAVE_MENU_TITLE)
        menuAutosave.add(autosaveCheckbox)
        menuBar.add(menuAutosave)

        this.jMenuBar = menuBar
    }

    private fun addListeners() {
        addSaveButtonListener()
        addLoadButtonListener()
        addRemoveButtonListener()
        addNotesSelectionListener()
    }

    private fun addSaveButtonListener() = saveButton.addActionListener { if (confirmedSaveNote()) saveNote() }

    // TODO: Implement editing existing notes without creating new ones
    private fun saveNote() {
        val note = textArea.text
        val fileName = getNoteName()

        noteRepository.insertNote(fileName, note) // TODO: Don't work with DB on EDT

        statusLabel.text = UIText.noteSaved(fileName)

        readNotes()
        updateSavedNotesList()
    }

    private fun getNoteName(): String {
        var fileName = if (header.text != UIText.START_HEADER_TEXT && header.text.isNotEmpty()) {
            header.text
        } else {
            // take the first word of the note, remove punctuation
            val note = textArea.text
            var fileName = note
                .substringBefore(' ')
                .trim { !it.isLetterOrDigit() }

            // limit the word to 15 characters
            if (fileName.length > 15) fileName = fileName.take(15)

            if (fileName.isEmpty()) fileName = UIText.EMPTY_NOTE_TITLE

            fileName
        }

        fileName += UIText.TXT_EXTENSION
        return fileName
    }

    // TODO: Unify the "confirm" methods
    private fun confirmedSaveNote(): Boolean {
        val choice = JOptionPane.showConfirmDialog(
            this,
            UIText.CONFIRM_SAVE_MESSAGE,
            UIText.CONFIRM_SAVE_NOTE_TITLE,
            JOptionPane.YES_NO_OPTION
        )
        return choice == JOptionPane.YES_OPTION
    }

    private fun confirmedDeleteNote(): Boolean {
        val choice = JOptionPane.showConfirmDialog(
            this,
            UIText.CONFIRM_DELETE_MESSAGE,
            UIText.CONFIRM_DELETE_NOTE_TITLE,
            JOptionPane.YES_NO_OPTION
        )
        return choice == JOptionPane.YES_OPTION
    }

    private fun confirmedExit(): Boolean {
        val choice = JOptionPane.showConfirmDialog(
            this,
            UIText.CONFIRM_EXIT_MESSAGE,
            UIText.EXIT_TITLE,
            JOptionPane.YES_NO_OPTION
        )
        return choice == JOptionPane.YES_OPTION || (choice == JOptionPane.CLOSED_OPTION)
    }

    private fun addLoadButtonListener() = loadButton.addActionListener {
        readNotes()
        updateSavedNotesList()
        statusLabel.text = UIText.NOTES_UPDATED_MESSAGE
    }

    private fun addRemoveButtonListener() = removeButton.addActionListener {
        if (savedNotesList.isSelectionEmpty) return@addActionListener
        if (confirmedDeleteNote()) removeNote()
    }

    private fun removeNote() {
        val selectedNote = savedNotesList.selectedValue
        noteRepository.deleteNote(selectedNote.id)
        statusLabel.text = UIText.noteDeleted(getNoteName())
        textArea.text = ""
        header.text = UIText.START_HEADER_TEXT
        readNotes()
        updateSavedNotesList()
    }

    // TODO: When add some text and then select another note, autosave if the checkbox is checked, ask about saving otherwise
    private fun addNotesSelectionListener() = savedNotesList.addListSelectionListener {
        // setListData in updateSavedNotesList removes selection and triggers this listener
        // which leads to savedNotesList.selectedValue == null and thus an exception
        if (savedNotesList.isSelectionEmpty) return@addListSelectionListener
        val selectedNote = savedNotesList.selectedValue
        header.text = selectedNote.title.removeSuffix(UIText.TXT_EXTENSION)

        textArea.text = noteRepository.selectNote(selectedNote.id)
    }

    private fun readNotes() {
        savedNotes.clear()
        val loadedNotes = noteRepository.loadAllNotes()
        savedNotes.addAll(loadedNotes)
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
    val ds = MysqlDataSource().apply {
        serverName = "localhost" // find out about Unix socket and TCP
        databaseName = "notesapp"
        user = System.getenv("MYSQL_USER")
        password = System.getenv("MYSQL_PASSWORD")
        description = "Notes App Database"
    }
    val noteRepository = NoteRepository(ds)

    SwingUtilities.invokeLater {
        val frame = MainWindow(noteRepository)
        frame.isVisible = true
    }
}
