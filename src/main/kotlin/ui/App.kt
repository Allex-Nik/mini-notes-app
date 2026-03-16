package org.education.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.education.exceptions.*
import org.education.mini_notes_app.generated.resources.Res
import org.education.mini_notes_app.generated.resources.exit
import org.education.mini_notes_app.generated.resources.new_note
import org.education.mini_notes_app.generated.resources.save_note
import org.education.model.NOTE_TITLE_MAX_LENGTH
import org.education.model.NoteListItem
import org.education.service.NoteService
import org.education.service.START_HEADER_TEXT
import org.education.ui.UIText.CONFIRM_DELETE_MESSAGE
import org.education.ui.UIText.CONFIRM_DELETE_NOTE_TITLE
import org.education.ui.UIText.CONFIRM_EXIT_MESSAGE
import org.education.ui.UIText.CONFIRM_SAVE_MESSAGE
import org.education.ui.UIText.CONFIRM_SAVE_NOTE_TITLE
import org.education.ui.UIText.EXIT_TITLE
import org.jetbrains.compose.resources.painterResource
import java.awt.Toolkit

private val selectBackground = Color.White
private val MAIN_WINDOW_COLOR = Color(244, 231, 207)
private val TEXT_AREA_COLOR = Color(255, 247, 232)
private val BUTTONS_COLOR = Color(204, 191, 167)
const val LEFT_PANEL_WIDTH = 300
const val BUTTONS_HEIGHT = 50

@Composable
internal fun App(
    noteService: NoteService,
    onExit: () -> Unit
) {
    LaunchedEffect(Unit) {
        Icons.installMacDockIcon()
    }

    var notes by remember { mutableStateOf(noteService.loadAllNotes()) }
    var currentNoteId by remember { mutableStateOf<Long?>(null) }
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }

    var header by remember { mutableStateOf(START_HEADER_TEXT) }
    var text by remember { mutableStateOf("") }
    var autosave by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(UIText.READY_LABEL) }

    var showSaveConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var headerError by remember { mutableStateOf<String?>(null) }
    var genericError by remember { mutableStateOf<Pair<String, String>?>(null) }

    var pendingSelectedNote by remember { mutableStateOf<NoteListItem?>(null) }
    var pendingNewNote by remember { mutableStateOf(false) }

    val saveNoteIcon = painterResource(Res.drawable.save_note)
    val newNoteIcon = painterResource(Res.drawable.new_note)
    val exitIcon = painterResource(Res.drawable.exit)

    fun refreshNotes() {
        notes = noteService.loadAllNotes()
    }

    fun restoreSelection() {
        val id = currentNoteId
        if (id == null) {
            selectedNoteId = null
            return
        }

        val exists = notes.any { it.id == id }
        if (!exists) {
            currentNoteId = null
            selectedNoteId = null
            return
        }

        selectedNoteId = id
    }

    fun clearEditor() {
        currentNoteId = null
        selectedNoteId = null
        header = START_HEADER_TEXT
        text = ""
    }

    fun handleNoteNotFound(message: String) {
        genericError = UIText.NOTE_NOT_FOUND_TITLE to message
        currentNoteId = null
        selectedNoteId = null
        status = UIText.NOTE_NOT_FOUND_LABEL
        refreshNotes()
    }

    fun isNoteChanged(): Boolean {
        val currentText = text
        val currentHeader = header

        if (currentNoteId == null) {
            return currentText.isNotEmpty() ||
                    (currentHeader != START_HEADER_TEXT && currentHeader.isNotEmpty())
        }

        val savedNoteData = try {
            noteService.selectNote(currentNoteId ?: throw UnexpectedCurrentNoteId())
        } catch (ex: NoteNotFoundException) {
            handleNoteNotFound(ex.message ?: "Note not found")
            return true
        }

        return currentText != savedNoteData.text || currentHeader != savedNoteData.title
    }

    fun saveNote(): Boolean {
        return try {
            val noteName = noteService.getNoteName(header, text)
            currentNoteId = noteService.saveNote(currentNoteId, noteName, text)
            status = UIText.noteSaved(noteName)
            refreshNotes()
            restoreSelection()
            true
        } catch (ex: HeaderTooLongException) {
            headerError = ex.message ?: "Header is too long"
            false
        } catch (ex: NoteNotFoundException) {
            handleNoteNotFound(ex.message ?: "Note not found")
            false
        }
    }

    fun loadSelectedNote(selectedNote: NoteListItem) {
        try {
            val loadedNote = noteService.selectNote(selectedNote.id)
            header = loadedNote.title
            text = loadedNote.text
            currentNoteId = selectedNote.id
            selectedNoteId = selectedNote.id
        } catch (ex: NoteNotFoundException) {
            handleNoteNotFound(ex.message ?: "Note not found")
            clearEditor()
        }
    }

    fun handleNewNote() {
        if (isNoteChanged()) {
            if (autosave) {
                val saved = saveNote()
                if (!saved) return
            } else {
                pendingSelectedNote = null
                pendingNewNote = true
                showSaveConfirm = true
                return
            }
        }

        clearEditor()
        status = UIText.NOTE_CREATED_MESSAGE
    }

    fun handleSave() {
        showSaveConfirm = true
    }

    fun handleLoad() {
        refreshNotes()
        restoreSelection()
        status = UIText.NOTES_UPDATED_MESSAGE
    }

    fun handleRemove() {
        if (selectedNoteId == null) return
        showDeleteConfirm = true
    }

    fun handleSelectNote(note: NoteListItem) {
        if (note.id == selectedNoteId) return

        if (isNoteChanged()) {
            if (autosave) {
                val saved = saveNote()
                if (!saved) return
                loadSelectedNote(note)
            } else {
                pendingSelectedNote = note
                pendingNewNote = false
                showSaveConfirm = true
            }
            return
        }

        loadSelectedNote(note)
    }

    fun handleExit() {
        showExitConfirm = true
    }

    fun isMenuShortcut(event: KeyEvent): Boolean {
        val isMac = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
        return if (isMac) event.isMetaPressed else event.isCtrlPressed
    }

    MaterialTheme(
        colors = lightColors(
            primary = MAIN_WINDOW_COLOR,
            onPrimary = Color.Black,
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        isMenuShortcut(event) && event.key == Key.S -> {
                            handleSave()
                            true
                        }

                        isMenuShortcut(event) && event.key == Key.N -> {
                            handleNewNote()
                            true
                        }

                        event.key == Key.Escape -> {
                            handleExit()
                            true
                        }

                        else -> false
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(UIText.MAIN_MENU_TITLE) },
                    actions = {
                        Button(
                            onClick = { handleNewNote() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = BUTTONS_COLOR)
                        ) {
                            Icon(
                                painter = newNoteIcon,
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(UIText.NEW_NOTE_TITLE)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { handleSave() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = BUTTONS_COLOR)
                        ) {
                            Icon(
                                painter = saveNoteIcon,
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(UIText.SAVE_TITLE)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { handleExit() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = BUTTONS_COLOR)
                        ) {
                            Icon(
                                painter = exitIcon,
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(EXIT_TITLE)
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LeftPanel(
                        modifier = Modifier
                            .width(LEFT_PANEL_WIDTH.dp)
                            .fillMaxHeight()
                            .background(MAIN_WINDOW_COLOR),
                        notes = notes,
                        selectedNoteId = selectedNoteId,
                        autosave = autosave,
                        onAutosaveChange = { autosave = it },
                        onSaveClick = { handleSave() },
                        onLoadClick = { handleLoad() },
                        onRemoveClick = { handleRemove() },
                        onSelectNote = { handleSelectNote(it) }
                    )

                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )

                    EditorPanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(TEXT_AREA_COLOR),
                        header = header,
                        text = text,
                        onHeaderChange = { newValue ->
                            if (newValue.length <= NOTE_TITLE_MAX_LENGTH) {
                                header = newValue
                            } else {
                                Toolkit.getDefaultToolkit().beep()
                            }
                        },
                        onTextChange = { text = it }
                    )
                }

                Divider()

                Text(
                    text = status,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MAIN_WINDOW_COLOR)
                )
            }
        }

        if (showSaveConfirm) {
            ConfirmDialog(
                message = CONFIRM_SAVE_MESSAGE,
                title = CONFIRM_SAVE_NOTE_TITLE,
                onConfirm = {
                    val saved = saveNote()
                    showSaveConfirm = false

                    if (!saved) return@ConfirmDialog

                    pendingSelectedNote?.let {
                        loadSelectedNote(it)
                        pendingSelectedNote = null
                    }

                    if (pendingNewNote) {
                        clearEditor()
                        status = UIText.NOTE_CREATED_MESSAGE
                        pendingNewNote = false
                    }
                },
                onDismiss = {
                    showSaveConfirm = false
                    pendingSelectedNote?.let {
                        loadSelectedNote(it)
                        pendingSelectedNote = null
                    }

                    if (pendingNewNote) {
                        clearEditor()
                        status = UIText.NOTE_CREATED_MESSAGE
                        pendingNewNote = false
                    }
                }
            )
        }

        if (showDeleteConfirm) {
            ConfirmDialog(
                message = CONFIRM_DELETE_MESSAGE,
                title = CONFIRM_DELETE_NOTE_TITLE,
                onConfirm = {
                    val id = selectedNoteId
                    val selectedNote = notes.firstOrNull { it.id == id }
                    if (selectedNote == null || id == null) {
                        showDeleteConfirm = false
                        return@ConfirmDialog
                    }
                    try {
                        noteService.deleteNote(id)
                        clearEditor()
                        refreshNotes()
                        status = selectedNote.let { UIText.noteDeleted(it.title) }
                    } catch (ex: NoteNotDeletedException) {
                        genericError = UIText.NOTE_NOT_DELETED_TITLE to
                                (ex.message ?: "The note could not be deleted")
                        status = UIText.NOTE_NOT_DELETED_LABEL
                        refreshNotes()
                    } catch (ex: MultipleRowsAffectedException) {
                        genericError = UIText.NOTE_NOT_DELETED_TITLE to
                                (ex.message ?: "Attempt to delete multiple rows")
                        status = UIText.NOTE_NOT_DELETED_LABEL
                        refreshNotes()
                    }

                    showDeleteConfirm = false
                },
                onDismiss = {
                    showDeleteConfirm = false
                }
            )
        }

        if (showExitConfirm) {
            ConfirmDialog(
                message = CONFIRM_EXIT_MESSAGE,
                title = EXIT_TITLE,
                onConfirm = {
                    showExitConfirm = false
                    if (!autosave) {
                        onExit()
                        return@ConfirmDialog
                    }
                    val saved = saveNote()
                    if (!saved) return@ConfirmDialog
                    onExit()
                },
                onDismiss = {
                    showExitConfirm = false
                }
            )
        }

        headerError?.let { message ->
            AlertDialog(
                onDismissRequest = { headerError = null },
                title = { Text(UIText.HEADER_ERROR_TITLE) },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = { headerError = null }) {
                        Text("OK")
                    }
                }
            )
        }

        genericError?.let { (title, message) ->
            AlertDialog(
                onDismissRequest = { genericError = null },
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    Button(onClick = { genericError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun LeftPanel(
    modifier: Modifier = Modifier,
    notes: List<NoteListItem>,
    selectedNoteId: Long?,
    autosave: Boolean,
    onAutosaveChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onLoadClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onSelectNote: (NoteListItem) -> Unit
) {
    Column(modifier = modifier) {
        AppTooltip(UIText.SAVE_BUTTON_TOOLTIP) {
            Button(
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BUTTONS_HEIGHT.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = BUTTONS_COLOR)
            ) {
                Text(UIText.SAVE_BUTTON_TITLE)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppTooltip(UIText.LOAD_BUTTON_TOOLTIP) {
            Button(
                onClick = onLoadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BUTTONS_HEIGHT.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = BUTTONS_COLOR)
            ) {
                Text(UIText.LOAD_BUTTON_TITLE)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppTooltip(UIText.REMOVE_BUTTON_TOOLTIP) {
            Button(
                onClick = onRemoveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BUTTONS_HEIGHT.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = BUTTONS_COLOR)
            ) {
                Text(UIText.REMOVE_BUTTON_TITLE)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = UIText.SAVED_NOTES_LABEL,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(notes, key = { it.id }) { note ->
                val selected = note.id == selectedNoteId

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) selectBackground else Color.Transparent)
                        .clickable { onSelectNote(note) }
                        .padding(6.dp)
                ) {
                    Text(note.title)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = autosave,
                onCheckedChange = onAutosaveChange
            )
            Text(UIText.AUTOSAVE_CHECKBOX_TITLE)
        }
    }
}

@Composable
private fun EditorPanel(
    modifier: Modifier = Modifier,
    header: String,
    text: String,
    onHeaderChange: (String) -> Unit,
    onTextChange: (String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = header,
            onValueChange = onHeaderChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Title") }
        )

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Text") }
        )
    }
}

@Composable
private fun ConfirmDialog(
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
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTooltip(
    text: String,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = Color(255, 255, 210),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        },
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.BottomEnd,
            offset = DpOffset(0.dp, 0.dp)
        )
    ) {
        content()
    }
}