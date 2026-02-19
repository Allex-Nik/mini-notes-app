package org.education

import org.education.repository.NoteRepository

class NoteService(private val noteRepository: NoteRepository) {
    fun getNoteName(header: String, text: String): String {
        var fileName = if (header != UIText.START_HEADER_TEXT && header.isNotEmpty()) {
            header
        } else {
            // take the first word of the note, remove punctuation
            val note = text
            var fileName = note
                .substringBefore(' ')
                .trim { !it.isLetterOrDigit() }

            // limit the word to the maximum allowed number of characters
            if (fileName.length > MAX_CHARACTERS) fileName = fileName.take(MAX_CHARACTERS)

            if (fileName.isEmpty()) fileName = UIText.EMPTY_NOTE_TITLE

            fileName
        }
        return fileName
    }

    fun saveNote(id: Long?, noteName: String, text: String): Long {
        return if (id == null) {
            noteRepository.insertNote(noteName, text) // TODO: Don't work with DB on EDT
        } else {
            noteRepository.updateNote(id, noteName, text)
            id
        }
    }

    fun deleteNote(id: Long) = noteRepository.deleteNote(id)

    fun selectNote(id: Long): String = noteRepository.selectNote(id)

    fun loadAllNotes(): List<NoteListItem> = noteRepository.loadAllNotes()
}