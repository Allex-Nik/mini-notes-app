package org.education.ui

import org.education.exceptions.HeaderTooLongException
import org.education.exceptions.NoteNotFoundException
import org.education.model.NoteListItem
import org.education.service.NoteService
import org.education.service.START_HEADER_TEXT
import org.education.ui.UIText.CONFIRM_DELETE_MESSAGE
import org.education.ui.UIText.CONFIRM_DELETE_NOTE_TITLE
import org.education.ui.UIText.CONFIRM_EXIT_MESSAGE
import org.education.ui.UIText.CONFIRM_SAVE_MESSAGE
import org.education.ui.UIText.CONFIRM_SAVE_NOTE_TITLE
import org.education.ui.UIText.EXIT_TITLE
import java.awt.BorderLayout
import java.awt.Image
import java.awt.Taskbar
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.AbstractDocument
import kotlin.system.exitProcess

// TODO: rewrite to Compose in a separate branch, do not extract controller
internal class MainWindow(private val noteService: NoteService) : JFrame() {
    // buttons and checkbox
    private val saveButton = JButton(UIText.SAVE_BUTTON_TITLE)
    private val loadButton = JButton(UIText.LOAD_BUTTON_TITLE)
    private val removeButton = JButton(UIText.REMOVE_BUTTON_TITLE)
    private val autosaveCheckbox = JCheckBox(UIText.AUTOSAVE_CHECKBOX_TITLE, false)

    // panels and pane
    private val leftPanel = JPanel()
    private val centralPanel = JPanel().apply { layout = BorderLayout() }
    private val notesPanel = JPanel()
    private lateinit var savedNotesScrollPane: JScrollPane

    // labels
    private val savedNotesLabel = JLabel(UIText.SAVED_NOTES_LABEL, SwingConstants.CENTER)
        .apply { font = Theme.savedNotesLabelFont }
    private val statusLabel = JLabel(UIText.READY_LABEL)
        .apply { border = Theme.statusPadding }

    // shortcuts
    private val saveShortcut: KeyStroke? = KeyStroke.getKeyStroke(UIText.SAVE_SHORTCUT)
    private val newNoteShortcut: KeyStroke? = KeyStroke.getKeyStroke(UIText.NEW_NOTE_SHORTCUT)
    private val exitShortcut: KeyStroke? = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)

    // list of notes
    private val listModel = DefaultListModel<NoteListItem>()
    private var notesJList = JList(listModel)

    // current note
    private val header = JTextField(START_HEADER_TEXT)
    private val textArea = JTextArea()
    private var currentNoteId: Long? = null
    private var isAdjustingSelection = false

    init {
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
        notesPanel.apply {
            layout = BorderLayout()
            background = Theme.savedNotesPanelColor
        }

        notesJList.apply {
            background = Theme.savedNotesListColor
            selectionBackground = Theme.selectBackground
            selectionForeground = Theme.selectForeground
            toolTipText = UIText.SAVED_NOTES_LIST_TOOLTIP
            font = Theme.savedNotesListFont
        }

        savedNotesScrollPane = JScrollPane(notesJList)
        notesPanel.add(savedNotesLabel, BorderLayout.NORTH)
        notesPanel.add(savedNotesScrollPane, BorderLayout.CENTER)

        readNotes()

        leftPanel.add(notesPanel, BorderLayout.CENTER)
    }

    private fun configureTextArea() {
        textArea.apply {
            background = Theme.textAreaColor
            // ctrl N - in JTextArea denotes moving to the next line
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
                // save/ask only if there are changes in the note
                if (isNoteChanged()) {
                    // if the checkbox is selected, the confirmedAction() is not evaluated and the dialog is not shown
                    if (autosaveCheckbox.isSelected || confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)) {
                        saveNote()
                    }
                }
                currentNoteId = null
                header.text = START_HEADER_TEXT
                textArea.text = ""
                notesJList.clearSelection()
                statusLabel.text = UIText.NOTE_CREATED_MESSAGE
            }
        }

        // save note menu item
        val saveNotePic = ImageIcon(this.javaClass.getResource(Icons.SAVE_NOTE_ICON))
            .image.getScaledInstance(Theme.MENU_ICON_WIDTH, Theme.MENU_ICON_HEIGHT, Image.SCALE_SMOOTH)
        val saveNoteItem = JMenuItem(UIText.SAVE_TITLE, ImageIcon(saveNotePic))
        saveNoteItem.accelerator = saveShortcut
        saveNoteItem.addActionListener {
            if (confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)) {
                saveNote()
                restoreSelection()
            }
        }

        // exit menu item
        val exitPic = ImageIcon(this.javaClass.getResource(Icons.EXIT_ICON))
            .image.getScaledInstance(Theme.MENU_ICON_WIDTH, Theme.MENU_ICON_HEIGHT, Image.SCALE_SMOOTH)
        val exitItem = JMenuItem(EXIT_TITLE, ImageIcon(exitPic))
        exitItem.accelerator = exitShortcut
        exitItem.addActionListener {
            if (!confirmedAction(CONFIRM_EXIT_MESSAGE, EXIT_TITLE)) return@addActionListener
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

    private fun addSaveButtonListener() = saveButton.addActionListener {
        if (confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)) {
            saveNote()
            restoreSelection()
        }
    }

    private fun saveNote(): Boolean {
        val noteText = textArea.text
        val noteHeader = header.text

        try {
            val noteName = noteService.getNoteName(noteHeader, noteText)
            currentNoteId = noteService.saveNote(currentNoteId, noteName, noteText)
            statusLabel.text = UIText.noteSaved(noteName)
            readNotes()
        } catch (ex: HeaderTooLongException) {
            notifyAboutError(ex.message ?: "Header is too long", UIText.HEADER_ERROR_TITLE)
            return false
        } catch (ex: NoteNotFoundException) {
            handleNoteNotFound(ex.message ?: "Note not found")
            return false
        }
        return true
    }

    private fun restoreSelection() {
        val id = currentNoteId ?: return
        val noteIndexToSelect = (0 until listModel.size).indexOfFirst { listModel[it].id == id }
        if (noteIndexToSelect >= 0) {
            isAdjustingSelection = true
            notesJList.selectedIndex = noteIndexToSelect
            isAdjustingSelection = false
            notesJList.ensureIndexIsVisible(noteIndexToSelect)
        } else {
            currentNoteId = null
            notesJList.clearSelection()
        }
    }

    private fun confirmedAction(message: String, title: String): Boolean {
        val choice = JOptionPane.showConfirmDialog(
            this,
            message,
            title,
            JOptionPane.YES_NO_OPTION
        )
        return choice == JOptionPane.YES_OPTION
    }

    private fun notifyAboutError(message: String, notificationTitle: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            notificationTitle,
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun addLoadButtonListener() = loadButton.addActionListener {
        readNotes()
        // if currentNoteId != null, selection restored. If it is null, this step is skipped.
        restoreSelection()
        statusLabel.text = UIText.NOTES_UPDATED_MESSAGE
    }

    private fun addRemoveButtonListener() = removeButton.addActionListener {
        if (notesJList.isSelectionEmpty) return@addActionListener
        if (confirmedAction(CONFIRM_DELETE_MESSAGE, CONFIRM_DELETE_NOTE_TITLE)) removeNote()
    }

    private fun removeNote() {
        val selectedNote = notesJList.selectedValue
        noteService.deleteNote(selectedNote.id)
        currentNoteId = null
        statusLabel.text = UIText.noteDeleted(selectedNote.title)
        textArea.text = ""
        header.text = START_HEADER_TEXT
        readNotes() // heavy operation, better to avoid
    }

    // `isAdjustingSelection` flag is used to prevent calling this selection listener
    // when selection is changed due to automatic updates, and not when the user intentionally selects a different note
    private fun addNotesSelectionListener() = notesJList.addListSelectionListener {
        if (isAdjustingSelection || notesJList.isSelectionEmpty) return@addListSelectionListener
        val selectedNote = notesJList.selectedValue
        if (isNoteChanged()) {
            if (autosaveCheckbox.isSelected || confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)) {
                // if the note is expected to be saved, but it wasn't, keep selection in order not to lose the text
                val noteSaved = saveNote()
                if (!noteSaved) return@addListSelectionListener
            }
        }
        currentNoteId = selectedNote.id
        header.text = selectedNote.title
        try {
            textArea.text = noteService.selectNote(selectedNote.id)
        } catch (ex: NoteNotFoundException) {
            handleNoteNotFound(ex.message ?: "Note not found")
            header.text = START_HEADER_TEXT
            textArea.text = ""
        }
        restoreSelection()
    }

    private fun handleNoteNotFound(message: String) {
        notifyAboutError(message, UIText.NOTE_NOT_FOUND_TITLE)
        currentNoteId = null
        statusLabel.text = UIText.NOTE_NOT_FOUND_LABEL
        readNotes()
    }

    private fun readNotes() {
        isAdjustingSelection = true
        listModel.clear()
        isAdjustingSelection = false
        val loadedNotes = noteService.loadAllNotes()
        loadedNotes.forEach { listModel.addElement(it) }
    }

    private fun isNoteChanged(): Boolean {
        val currentText = textArea.text
        // new note case
        if (currentNoteId == null) {
            return currentText.isNotEmpty() || (header.text != START_HEADER_TEXT && header.text.isNotEmpty())
        }
        // existing note case
        val savedText = try {
            noteService.selectNote(currentNoteId!!)
        } catch (ex: NoteNotFoundException) {
            handleNoteNotFound(ex.message ?: "Note not found")
        }
        return currentText != savedText || header.text != noteService.loadAllNotes()
            .find { it.id == currentNoteId }?.title // potentially heavy operation
    }
}