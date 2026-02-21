package org.education.ui

import org.education.exceptions.HeaderTooLongException
import org.education.model.NoteListItem
import org.education.service.NoteService
import java.awt.BorderLayout
import java.awt.Image
import java.awt.Taskbar
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.text.AbstractDocument
import kotlin.system.exitProcess

class MainWindow(val noteService: NoteService) : JFrame() { // BorderLayout by default
    private val saveButton = JButton(UIText.SAVE_BUTTON_TITLE)
    private val loadButton = JButton(UIText.LOAD_BUTTON_TITLE)
    private val removeButton = JButton(UIText.REMOVE_BUTTON_TITLE)
    private val leftPanel = JPanel() // FlowLayout by default
    private val centralPanel = JPanel().apply { layout = BorderLayout() }
    private val header = JTextField(UIText.START_HEADER_TEXT)
    private val textArea = JTextArea()
    private val noteListItems = mutableListOf<NoteListItem>()
    private val notesPanel = JPanel() // FlowLayout by default
    private var notesJList =
        JList(noteListItems.toTypedArray()) // TODO: Try DefaultListModel to avoid using setListData
    private val savedNotesLabel = JLabel(UIText.SAVED_NOTES_LABEL, SwingConstants.CENTER)
        .apply { font = Theme.savedNotesLabelFont }
    private lateinit var savedNotesScrollPane: JScrollPane
    private val autosaveCheckbox = JCheckBox(UIText.AUTOSAVE_CHECKBOX_TITLE, false)
    private val saveShortcut: KeyStroke? = KeyStroke.getKeyStroke(UIText.SAVE_SHORTCUT)
    private val newNoteShortcut: KeyStroke? = KeyStroke.getKeyStroke(UIText.NEW_NOTE_SHORTCUT)
    private val statusLabel = JLabel(UIText.READY_LABEL)
        .apply { border = Theme.statusPadding }
    private var currentNoteId: Long? = null

    init {
//        noteRepository.dropNotesTable() // left for development
//        noteRepository.createNotesTableIfNotExists()
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

        readNotes()
        updateSavedNotesList()

        leftPanel.add(notesPanel, BorderLayout.CENTER)
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
                currentNoteId = null
                header.text = UIText.START_HEADER_TEXT
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

    private fun saveNote() {
        val noteText = textArea.text
        val noteHeader = header.text

        try {
            val noteName = noteService.getNoteName(noteHeader, noteText)
            currentNoteId = noteService.saveNote(currentNoteId, noteName, noteText)
            statusLabel.text = UIText.noteSaved(noteName)

            readNotes()
            updateSavedNotesList()
            restoreSelection()
        } catch (ex: HeaderTooLongException) {
            notifyAboutLongHeader(ex.message)
        }
    }

    private fun restoreSelection() {
        val id = currentNoteId ?: return
        val noteIndexToSelect = noteListItems.indexOfFirst { it.id == id }
        if (noteIndexToSelect >= 0) {
            notesJList.selectedIndex = noteIndexToSelect
            notesJList.ensureIndexIsVisible(noteIndexToSelect)
        } else { // do we need this branch?
            currentNoteId = null
            notesJList.clearSelection()
        }
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

    private fun notifyAboutLongHeader(message: String) {
        JOptionPane.showMessageDialog(
            this,
            message,
            UIText.HEADER_ERROR_TITLE,
            JOptionPane.ERROR_MESSAGE
        )
    }

    private fun addLoadButtonListener() = loadButton.addActionListener {
        readNotes()
        updateSavedNotesList()
        // if currentNoteId != null, selection restored. if it is null, this step is skipped.
        // TODO: Keep an eye on synchronization between currentNoteId and savedNotesList.selectedValue.id.
        //  Consider currentNoteId = savedNotesList.selectedValue?.id
        restoreSelection()
        statusLabel.text = UIText.NOTES_UPDATED_MESSAGE
    }

    private fun addRemoveButtonListener() = removeButton.addActionListener {
        if (notesJList.isSelectionEmpty) return@addActionListener
        if (confirmedDeleteNote()) removeNote()
    }

    private fun removeNote() {
        val selectedNote = notesJList.selectedValue
        noteService.deleteNote(selectedNote.id)
        currentNoteId = null
        statusLabel.text = UIText.noteDeleted(selectedNote.title)
        textArea.text = ""
        header.text = UIText.START_HEADER_TEXT
        readNotes() // heavy operation, better to avoid
        updateSavedNotesList()
    }

    // TODO: When add some text and then select another note, autosave if the checkbox is checked, ask about saving otherwise
    private fun addNotesSelectionListener() = notesJList.addListSelectionListener {
        // setListData in updateSavedNotesList removes selection and triggers this listener
        // which leads to savedNotesList.selectedValue == null and thus an exception
        if (notesJList.isSelectionEmpty) return@addListSelectionListener
        val selectedNote = notesJList.selectedValue
        currentNoteId = selectedNote.id
        header.text = selectedNote.title

        textArea.text = noteService.selectNote(selectedNote.id)
    }

    private fun readNotes() {
        noteListItems.clear()
        val loadedNotes = noteService.loadAllNotes()
        noteListItems.addAll(loadedNotes)
    }

    private fun updateSavedNotesList() {
        notesPanel.removeAll() // change only notes we need
        notesPanel.add(savedNotesLabel, BorderLayout.NORTH)
        notesJList.setListData(noteListItems.toTypedArray())
        savedNotesScrollPane = JScrollPane(notesJList)
        notesPanel.add(savedNotesScrollPane, BorderLayout.CENTER)
        notesPanel.revalidate()
        notesPanel.repaint()
    }
}