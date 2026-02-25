MainWindow (View)
        ↓
NotesController
        ↓
NoteService
        ↓
Repository (Hibernate / JDBC)

```kotlin
class NotesController(
private val service: NoteService
) {

    fun initialize() {
        service.createNotesTableIfNotExists()
    }

    fun loadNotes(): List<NoteListItem> =
        service.loadAllNotes()

    fun saveNote(id: Long?, header: String, text: String): Long {
        val noteName = service.getNoteName(header, text)
        return service.saveNote(id, noteName, text)
    }

    fun deleteNote(id: Long) {
        service.deleteNote(id)
    }

    fun getNoteText(id: Long): String =
        service.selectNote(id)
}


```

1. UI is now passive

MainWindow no longer:

- Constructs note names
- Decides business logic
- Talks directly to repository

It only:

- Collects input
- Displays output
- Delegates

That’s clean Swing architecture.


```Kotlin

class MainWindow(private val controller: NotesController) : JFrame() {

    private val listModel = DefaultListModel<NoteListItem>()
    private val notesJList = JList(listModel)

    private var currentNoteId: Long? = null
    private var isAdjustingSelection = false

    init {
        controller.initialize()
        configureFrame()
        configureLeftPanel()
        configureTextArea()
        configureMenuBar()
        addListeners()
    }

```


```Kotlin

class MainWindow(private val controller: NotesController) : JFrame() {

    // buttons and checkbox
    private val saveButton = JButton(UIText.SAVE_BUTTON_TITLE)
    private val loadButton = JButton(UIText.LOAD_BUTTON_TITLE)
    private val removeButton = JButton(UIText.REMOVE_BUTTON_TITLE)
    private val autosaveCheckbox = JCheckBox(UIText.AUTOSAVE_CHECKBOX_TITLE, false)

    // panels
    private val leftPanel = JPanel()
    private val centralPanel = JPanel(BorderLayout())
    private val notesPanel = JPanel()
    private lateinit var savedNotesScrollPane: JScrollPane

    // labels
    private val savedNotesLabel = JLabel(UIText.SAVED_NOTES_LABEL, SwingConstants.CENTER)
        .apply { font = Theme.savedNotesLabelFont }

    private val statusLabel = JLabel(UIText.READY_LABEL)
        .apply { border = Theme.statusPadding }

    // shortcuts
    private val saveShortcut = KeyStroke.getKeyStroke(UIText.SAVE_SHORTCUT)
    private val newNoteShortcut = KeyStroke.getKeyStroke(UIText.NEW_NOTE_SHORTCUT)
    private val exitShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)

    // notes list
    private val listModel = DefaultListModel<NoteListItem>()
    private val notesJList = JList(listModel)

    // editor
    private val header = JTextField(UIText.START_HEADER_TEXT)
    private val textArea = JTextArea()

    private var currentNoteId: Long? = null
    private var isAdjustingSelection = false

    init {
        controller.initialize()
        configureFrame()
        configureLeftPanel()
        configureTextArea()
        configureMenuBar()
        addListeners()
        refreshNotes()
    }

    // -------------------- UI CONFIG --------------------

    private fun configureFrame() {
        title = UIText.FRAME_TITLE
        setSize(Theme.FRAME_WIDTH, Theme.FRAME_HEIGHT)
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        add(statusLabel, BorderLayout.SOUTH)
        setAppIcon()
    }

    private fun setAppIcon() {
        val appImageAddress = javaClass.getResource(Icons.APP_ICON) ?: return
        val appImage = ImageIcon(appImageAddress)
        iconImage = appImage.image
    }

    private fun configureLeftPanel() {
        leftPanel.layout = BorderLayout()
        leftPanel.preferredSize = Theme.leftPanelSize

        createButtonsPanel()
        configureSavedNotesPanel()

        add(leftPanel, BorderLayout.WEST)
    }

    private fun createButtonsPanel() {
        val buttonsPanel = JPanel(BorderLayout()).apply {
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
        notesPanel.layout = BorderLayout()
        notesPanel.background = Theme.savedNotesPanelColor

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

        leftPanel.add(notesPanel, BorderLayout.CENTER)
    }

    private fun configureTextArea() {
        textArea.apply {
            background = Theme.textAreaColor
            lineWrap = true
            wrapStyleWord = true
            getInputMap(JComponent.WHEN_FOCUSED).put(newNoteShortcut, "none")
        }

        header.apply {
            font = Theme.noteHeaderFont
            horizontalAlignment = JTextField.CENTER
            background = Theme.textAreaColor
        }

        centralPanel.add(header, BorderLayout.NORTH)
        centralPanel.add(JScrollPane(textArea), BorderLayout.CENTER)

        add(centralPanel, BorderLayout.CENTER)
    }

    private fun configureMenuBar() {
        val menuBar = JMenuBar()
        val menu = JMenu(UIText.MAIN_MENU_TITLE)

        val newNoteItem = JMenuItem(UIText.NEW_NOTE_TITLE, loadMenuIcon(Icons.NEW_NOTE_ICON)).apply {
            accelerator = newNoteShortcut
            addActionListener { handleNewNote() }
        }

        val saveItem = JMenuItem(UIText.SAVE_TITLE, loadMenuIcon(Icons.SAVE_NOTE_ICON)).apply {
            accelerator = saveShortcut
            addActionListener { handleSaveWithConfirm() }
        }

        val exitItem = JMenuItem(UIText.EXIT_TITLE, loadMenuIcon(Icons.EXIT_ICON)).apply {
            accelerator = exitShortcut
            addActionListener { handleExit() }
        }

        menu.add(newNoteItem)
        menu.add(saveItem)
        menu.add(exitItem)

        val autosaveMenu = JMenu(UIText.AUTOSAVE_MENU_TITLE)
        autosaveMenu.add(autosaveCheckbox)

        menuBar.add(menu)
        menuBar.add(autosaveMenu)

        jMenuBar = menuBar
    }

    private fun loadMenuIcon(path: String): ImageIcon {
        val image = ImageIcon(javaClass.getResource(path)).image
        val scaled = image.getScaledInstance(
            Theme.MENU_ICON_WIDTH,
            Theme.MENU_ICON_HEIGHT,
            Image.SCALE_SMOOTH
        )
        return ImageIcon(scaled)
    }

    // -------------------- LISTENERS --------------------

    private fun addListeners() {
        saveButton.addActionListener { handleSaveWithConfirm() }
        loadButton.addActionListener {
            refreshNotes()
            restoreSelection()
            statusLabel.text = UIText.NOTES_UPDATED_MESSAGE
        }
        removeButton.addActionListener { handleDelete() }

        notesJList.addListSelectionListener {
            if (isAdjustingSelection || notesJList.isSelectionEmpty) return@addListSelectionListener

            val selectedNote = notesJList.selectedValue

            if (isNoteChanged()) {
                if (autosaveCheckbox.isSelected ||
                    confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)
                ) {
                    handleSave()
                }
            }

            currentNoteId = selectedNote.id
            header.text = selectedNote.title
            textArea.text = controller.getNoteText(selectedNote.id)
            restoreSelection()
        }
    }

    // -------------------- ACTIONS --------------------

    private fun handleSaveWithConfirm() {
        if (confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)) {
            handleSave()
            restoreSelection()
        }
    }

    private fun handleSave() {
        try {
            currentNoteId = controller.saveNote(currentNoteId, header.text, textArea.text)
            statusLabel.text = UIText.noteSaved(header.text)
            refreshNotes()
        } catch (ex: HeaderTooLongException) {
            JOptionPane.showMessageDialog(
                this,
                ex.message,
                UIText.HEADER_ERROR_TITLE,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun handleDelete() {
        if (notesJList.isSelectionEmpty) return

        if (confirmedAction(CONFIRM_DELETE_MESSAGE, CONFIRM_DELETE_NOTE_TITLE)) {
            val selected = notesJList.selectedValue
            controller.deleteNote(selected.id)

            currentNoteId = null
            header.text = UIText.START_HEADER_TEXT
            textArea.text = ""
            statusLabel.text = UIText.noteDeleted(selected.title)

            refreshNotes()
        }
    }

    private fun handleNewNote() {
        if (isNoteChanged()) {
            if (autosaveCheckbox.isSelected ||
                confirmedAction(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)
            ) {
                handleSave()
            }
        }

        currentNoteId = null
        header.text = UIText.START_HEADER_TEXT
        textArea.text = ""
        notesJList.clearSelection()
        statusLabel.text = UIText.NOTE_CREATED_MESSAGE
    }

    private fun handleExit() {
        if (!confirmedAction(CONFIRM_EXIT_MESSAGE, UIText.EXIT_TITLE)) return
        if (autosaveCheckbox.isSelected) handleSave()
        exitProcess(0)
    }

    // -------------------- HELPERS --------------------

    private fun refreshNotes() {
        isAdjustingSelection = true
        listModel.clear()
        controller.loadNotes().forEach { listModel.addElement(it) }
        isAdjustingSelection = false
    }

    private fun restoreSelection() {
        val id = currentNoteId ?: return
        val index = (0 until listModel.size).indexOfFirst { listModel[it].id == id }
        if (index >= 0) {
            isAdjustingSelection = true
            notesJList.selectedIndex = index
            isAdjustingSelection = false
            notesJList.ensureIndexIsVisible(index)
        } else {
            currentNoteId = null
            notesJList.clearSelection()
        }
    }

    private fun confirmedAction(message: String, title: String): Boolean {
        return JOptionPane.showConfirmDialog(
            this,
            message,
            title,
            JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION
    }

    private fun isNoteChanged(): Boolean {
        val currentText = textArea.text
        if (currentNoteId == null) {
            return currentText.isNotEmpty() ||
                    (header.text != UIText.START_HEADER_TEXT && header.text.isNotEmpty())
        }

        val savedText = controller.getNoteText(currentNoteId!!)
        val savedTitle = controller.loadNotes()
            .firstOrNull { it.id == currentNoteId }
            ?.title

        return currentText != savedText || header.text != savedTitle
    }
}

```


BUT WE COULD GO DEEPER

To the KOTLIN POWER

```Kotlin

fun panel(layout: LayoutManager? = null, block: JPanel.() -> Unit): JPanel =
    JPanel(layout).apply(block)

fun <T : AbstractButton> T.onClick(block: () -> Unit) =
    apply { addActionListener { block() } }

fun menu(title: String, block: JMenu.() -> Unit): JMenu =
    JMenu(title).apply(block)

fun menuItem(
    title: String,
    icon: Icon? = null,
    shortcut: KeyStroke? = null,
    block: () -> Unit
): JMenuItem =
    JMenuItem(title, icon).apply {
        accelerator = shortcut
        addActionListener { block() }
    }

```


```Kotlin
class MainWindow(private val controller: NotesController) : JFrame(UIText.FRAME_TITLE) {

    private val saveShortcut = KeyStroke.getKeyStroke(UIText.SAVE_SHORTCUT)
    private val newNoteShortcut = KeyStroke.getKeyStroke(UIText.NEW_NOTE_SHORTCUT)
    private val exitShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)

    private val listModel = DefaultListModel<NoteListItem>()
    private val notesList = JList(listModel)

    private val header = JTextField(UIText.START_HEADER_TEXT)
    private val textArea = JTextArea()

    private val autosaveCheckbox = JCheckBox(UIText.AUTOSAVE_CHECKBOX_TITLE, false)
    private val statusLabel = JLabel(UIText.READY_LABEL)

    private var currentNoteId: Long? = null
    private var isAdjustingSelection = false

    init {
        controller.initialize()

        defaultCloseOperation = EXIT_ON_CLOSE
        size = Dimension(Theme.FRAME_WIDTH, Theme.FRAME_HEIGHT)
        layout = BorderLayout()

        add(buildLeftPanel(), BorderLayout.WEST)
        add(buildEditorPanel(), BorderLayout.CENTER)
        add(statusLabel.apply { border = Theme.statusPadding }, BorderLayout.SOUTH)

        jMenuBar = buildMenuBar()

        refreshNotes()
    }

    // ---------- UI BUILDING ----------

    private fun buildLeftPanel() = panel(BorderLayout()) {
        preferredSize = Theme.leftPanelSize

        add(buildButtonsPanel(), BorderLayout.NORTH)
        add(buildNotesPanel(), BorderLayout.CENTER)
    }

    private fun buildButtonsPanel() = panel(BorderLayout()) {
        border = Theme.buttonsPadding
        background = Theme.buttonsPanelColor

        add(JButton(UIText.SAVE_BUTTON_TITLE)
            .apply { font = Theme.saveButtonFont }
            .onClick { handleSaveWithConfirm() }, BorderLayout.NORTH)

        add(JButton(UIText.LOAD_BUTTON_TITLE)
            .apply { font = Theme.loadButtonFont }
            .onClick {
                refreshNotes()
                restoreSelection()
                statusLabel.text = UIText.NOTES_UPDATED_MESSAGE
            }, BorderLayout.CENTER)

        add(JButton(UIText.REMOVE_BUTTON_TITLE)
            .apply { font = Theme.removeButtonFont }
            .onClick { handleDelete() }, BorderLayout.SOUTH)
    }

    private fun buildNotesPanel() = panel(BorderLayout()) {
        background = Theme.savedNotesPanelColor

        notesList.apply {
            background = Theme.savedNotesListColor
            selectionBackground = Theme.selectBackground
            selectionForeground = Theme.selectForeground
            font = Theme.savedNotesListFont

            addListSelectionListener {
                if (isAdjustingSelection || isSelectionEmpty) return@addListSelectionListener

                val selected = selectedValue

                if (isNoteChanged()) {
                    if (autosaveCheckbox.isSelected ||
                        confirm(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)
                    ) {
                        handleSave()
                    }
                }

                currentNoteId = selected.id
                header.text = selected.title
                textArea.text = controller.getNoteText(selected.id)
                restoreSelection()
            }
        }

        add(JLabel(UIText.SAVED_NOTES_LABEL, SwingConstants.CENTER), BorderLayout.NORTH)
        add(JScrollPane(notesList), BorderLayout.CENTER)
    }

    private fun buildEditorPanel() = panel(BorderLayout()) {
        textArea.apply {
            background = Theme.textAreaColor
            lineWrap = true
            wrapStyleWord = true
        }

        header.apply {
            font = Theme.noteHeaderFont
            horizontalAlignment = JTextField.CENTER
            background = Theme.textAreaColor
        }

        add(header, BorderLayout.NORTH)
        add(JScrollPane(textArea), BorderLayout.CENTER)
    }

    private fun buildMenuBar() = JMenuBar().apply {

        add(menu(UIText.MAIN_MENU_TITLE) {

            add(menuItem(
                UIText.NEW_NOTE_TITLE,
                loadIcon(Icons.NEW_NOTE_ICON),
                newNoteShortcut
            ) { handleNewNote() })

            add(menuItem(
                UIText.SAVE_TITLE,
                loadIcon(Icons.SAVE_NOTE_ICON),
                saveShortcut
            ) { handleSaveWithConfirm() })

            add(menuItem(
                UIText.EXIT_TITLE,
                loadIcon(Icons.EXIT_ICON),
                exitShortcut
            ) { handleExit() })
        })

        add(menu(UIText.AUTOSAVE_MENU_TITLE) {
            add(autosaveCheckbox)
        })
    }

    private fun loadIcon(path: String): Icon {
        val img = ImageIcon(javaClass.getResource(path)).image
        val scaled = img.getScaledInstance(
            Theme.MENU_ICON_WIDTH,
            Theme.MENU_ICON_HEIGHT,
            Image.SCALE_SMOOTH
        )
        return ImageIcon(scaled)
    }

    // ---------- ACTIONS ----------

    private fun handleSaveWithConfirm() {
        if (confirm(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)) {
            handleSave()
            restoreSelection()
        }
    }

    private fun handleSave() {
        try {
            currentNoteId =
                controller.saveNote(currentNoteId, header.text, textArea.text)

            statusLabel.text = UIText.noteSaved(header.text)
            refreshNotes()
        } catch (ex: HeaderTooLongException) {
            JOptionPane.showMessageDialog(
                this,
                ex.message,
                UIText.HEADER_ERROR_TITLE,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun handleDelete() {
        if (notesList.isSelectionEmpty) return

        if (confirm(CONFIRM_DELETE_MESSAGE, CONFIRM_DELETE_NOTE_TITLE)) {
            val selected = notesList.selectedValue
            controller.deleteNote(selected.id)

            currentNoteId = null
            header.text = UIText.START_HEADER_TEXT
            textArea.text = ""
            statusLabel.text = UIText.noteDeleted(selected.title)

            refreshNotes()
        }
    }

    private fun handleNewNote() {
        if (isNoteChanged()) {
            if (autosaveCheckbox.isSelected ||
                confirm(CONFIRM_SAVE_MESSAGE, CONFIRM_SAVE_NOTE_TITLE)
            ) {
                handleSave()
            }
        }

        currentNoteId = null
        header.text = UIText.START_HEADER_TEXT
        textArea.text = ""
        notesList.clearSelection()
        statusLabel.text = UIText.NOTE_CREATED_MESSAGE
    }

    private fun handleExit() {
        if (!confirm(CONFIRM_EXIT_MESSAGE, UIText.EXIT_TITLE)) return
        if (autosaveCheckbox.isSelected) handleSave()
        exitProcess(0)
    }

    // ---------- HELPERS ----------

    private fun refreshNotes() {
        isAdjustingSelection = true
        listModel.clear()
        controller.loadNotes().forEach { listModel.addElement(it) }
        isAdjustingSelection = false
    }

    private fun restoreSelection() {
        val id = currentNoteId ?: return
        val index = (0 until listModel.size)
            .firstOrNull { listModel[it].id == id } ?: return

        isAdjustingSelection = true
        notesList.selectedIndex = index
        isAdjustingSelection = false
        notesList.ensureIndexIsVisible(index)
    }

    private fun confirm(message: String, title: String): Boolean =
        JOptionPane.showConfirmDialog(
            this,
            message,
            title,
            JOptionPane.YES_NO_OPTION
        ) == JOptionPane.YES_OPTION

    private fun isNoteChanged(): Boolean {
        val currentText = textArea.text

        if (currentNoteId == null) {
            return currentText.isNotEmpty() ||
                    (header.text != UIText.START_HEADER_TEXT && header.text.isNotEmpty())
        }

        val savedText = controller.getNoteText(currentNoteId!!)
        val savedTitle = controller.loadNotes()
            .firstOrNull { it.id == currentNoteId }
            ?.title

        return currentText != savedText || header.text != savedTitle
    }
}

```


Not enough cool?
COMPOSE?


```gradle

plugins {
        kotlin("jvm") version "1.9.0"
        id("org.jetbrains.compose") version "1.6.0"
}

dependencies {
        implementation(compose.desktop.currentOs)
}

```

```Kotlin
import androidx.compose.material.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.window.*
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.lazy.*
        import androidx.compose.foundation.lazy.items
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.res.painterResource
        import androidx.compose.ui.window.WindowState
        import androidx.compose.ui.unit.DpSize

fun main() = application {
        val controller = NotesController(NoteService(createRepository()))

        Window(
                onCloseRequest = ::exitApplication,
                title = UIText.FRAME_TITLE,
                state = WindowState(size = DpSize(1000.dp, 700.dp))
        ) {
                App(controller)
        }
}

```


```Kotlin

@Composable
fun App(controller: NotesController) {

    var notes by remember { mutableStateOf(controller.loadNotes()) }
    var currentNoteId by remember { mutableStateOf<Long?>(null) }
    var header by remember { mutableStateOf(UIText.START_HEADER_TEXT) }
    var text by remember { mutableStateOf("") }
    var autosave by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(UIText.READY_LABEL) }

    var showSaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showHeaderError by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        notes = controller.loadNotes()
    }

    fun isNoteChanged(): Boolean {
        if (currentNoteId == null) {
            return text.isNotEmpty() ||
                    (header != UIText.START_HEADER_TEXT && header.isNotEmpty())
        }
        val savedText = controller.getNoteText(currentNoteId!!)
        val savedTitle = notes.firstOrNull { it.id == currentNoteId }?.title
        return text != savedText || header != savedTitle
    }

    fun save() {
        try {
            currentNoteId = controller.saveNote(currentNoteId, header, text)
            status = UIText.noteSaved(header)
            refresh()
        } catch (ex: HeaderTooLongException) {
            showHeaderError = ex.message
        }
    }

    MaterialTheme {

        Column {

            TopAppBar(
                title = { Text(UIText.MAIN_MENU_TITLE) },
                actions = {
                    Button(onClick = { showSaveConfirm = true }) {
                        Text(UIText.SAVE_TITLE)
                    }
                    Button(onClick = { showExitConfirm = true }) {
                        Text(UIText.EXIT_TITLE)
                    }
                }
            )

            Row(Modifier.fillMaxSize()) {

                // LEFT PANEL
                Column(
                    Modifier.width(250.dp).fillMaxHeight().padding(8.dp)
                ) {

                    Button(onClick = { showSaveConfirm = true }) {
                        Text(UIText.SAVE_BUTTON_TITLE)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(onClick = { refresh() }) {
                        Text(UIText.LOAD_BUTTON_TITLE)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(onClick = { showDeleteConfirm = true }) {
                        Text(UIText.REMOVE_BUTTON_TITLE)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        UIText.SAVED_NOTES_LABEL,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn {
                        items(notes) { note ->
                            Text(
                                note.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .clickable {
                                        if (isNoteChanged()) {
                                            if (autosave) save()
                                            else showSaveConfirm = true
                                        }
                                        currentNoteId = note.id
                                        header = note.title
                                        text = controller.getNoteText(note.id)
                                    }
                            )
                        }
                    }
                }

                // EDITOR
                Column(
                    Modifier.fillMaxSize().padding(8.dp)
                ) {

                    TextField(
                        value = header,
                        onValueChange = { header = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Text") },
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autosave,
                            onCheckedChange = { autosave = it }
                        )
                        Text(UIText.AUTOSAVE_CHECKBOX_TITLE)
                    }
                }
            }

            Text(
                status,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
        }

        // -------- Dialogs --------

        if (showSaveConfirm) {
            ConfirmDialog(
                CONFIRM_SAVE_MESSAGE,
                CONFIRM_SAVE_NOTE_TITLE,
                onConfirm = {
                    save()
                    showSaveConfirm = false
                },
                onDismiss = { showSaveConfirm = false }
            )
        }

        if (showDeleteConfirm) {
            ConfirmDialog(
                CONFIRM_DELETE_MESSAGE,
                CONFIRM_DELETE_NOTE_TITLE,
                onConfirm = {
                    currentNoteId?.let { controller.deleteNote(it) }
                    currentNoteId = null
                    header = UIText.START_HEADER_TEXT
                    text = ""
                    refresh()
                    showDeleteConfirm = false
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }

        if (showExitConfirm) {
            ConfirmDialog(
                CONFIRM_EXIT_MESSAGE,
                UIText.EXIT_TITLE,
                onConfirm = { exitApplication() },
                onDismiss = { showExitConfirm = false }
            )
        }

        showHeaderError?.let {
            AlertDialog(
                onDismissRequest = { showHeaderError = null },
                title = { Text(UIText.HEADER_ERROR_TITLE) },
                text = { Text(it) },
                confirmButton = {
                    Button(onClick = { showHeaderError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

```


```kotlin

@Composable
fun ConfirmDialog(
    message: String,
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Yes") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("No") }
        }
    )
}
```