package org.education

class NoteService(private val noteRepository: NoteRepositoryHibernateImpl) {
    fun getNoteName(header: String, text: String): String {
        var fileName = if (header != UIText.START_HEADER_TEXT && header.isNotEmpty()) {
            header
        } else {
            // take the first word of the note, remove punctuation
            val note = text
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