package org.education

object UIText {
    const val EMPTY_NOTE_TITLE = "empty_note"
    const val TXT_EXTENSION = ".txt"

    const val FRAME_TITLE = "Notes"
    const val SAVE_BUTTON_TITLE = "Save"
    const val LOAD_BUTTON_TITLE = "Load"
    const val REMOVE_BUTTON_TITLE = "Remove"
    const val SAVED_NOTES_LABEL = "Saved Notes"
    const val AUTOSAVE_CHECKBOX_TITLE = "Autosave"
    const val SAVE_SHORTCUT = "ctrl S"
    const val NEW_NOTE_SHORTCUT = "ctrl N"
    const val SAVE_BUTTON_TOOLTIP = """Saves the current note to the database"""
    const val LOAD_BUTTON_TOOLTIP = "Refreshes the list of saved notes"
    const val REMOVE_BUTTON_TOOLTIP = "Deletes the note from the database"
    const val SAVED_NOTES_LIST_TOOLTIP = "Click on a note to view it"
    const val MAIN_MENU_TITLE = "Menu"
    const val AUTOSAVE_MENU_TITLE = "Autosave"
    const val CONFIRM_SAVE_NOTE_TITLE = "Save Note"
    const val EXIT_TITLE = "Exit"
    const val SAVE_TITLE = "Save"
    const val NEW_NOTE_TITLE = "New"
    const val START_HEADER_TEXT = "Add your header here"

    const val CONFIRM_SAVE_MESSAGE = "Do you want to save this note?"
    const val CONFIRM_EXIT_MESSAGE = "Are you sure you want to exit?"
    const val CONFIRM_DELETE_MESSAGE = "Do you want to delete this note?"
    const val CONFIRM_DELETE_NOTE_TITLE = "Delete Note"
    const val NOTES_UPDATED_MESSAGE = "Notes list updated successfully"
    const val NOTE_CREATED_MESSAGE = "New note created successfully"

    const val READY_LABEL = "Ready"

    fun noteSaved(fileName: String) = "Note $fileName saved successfully"
    fun noteDeleted(fileName: String) = "Note $fileName deleted successfully"
}