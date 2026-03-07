package org.education.service

import org.education.model.NOTE_TITLE_MAX_LENGTH
import org.education.exceptions.BlankNoteNameException
import org.education.exceptions.HeaderTooLongException
import org.education.model.NoteListItem
import org.education.repository.NoteRepository
import org.education.ui.UIText

internal class NoteService(private val noteRepository: NoteRepository) {
    /**
     * Given the [header] and the [text] of the note, computes the name of the note.
     * If the [header] is not default and not empty, the name of the note is the [header].
     * Otherwise, the name of the note is the first [NOTE_TITLE_MAX_LENGTH] characters of the first word in the note.
     * If the [text] of the note is empty (and the [header] is empty or default),
     * the note is called [UIText.EMPTY_NOTE_TITLE].
     *
     * @param header header of the note.
     * @param text text of the note.
     *
     * @throws HeaderTooLongException if the [header] of the note is larger than [NOTE_TITLE_MAX_LENGTH].
     *
     * @return the name of the note.
     */
    fun getNoteName(header: String, text: String): String {
        if (header.length > NOTE_TITLE_MAX_LENGTH)
            throw HeaderTooLongException(NOTE_TITLE_MAX_LENGTH)

        val fileName = if (header != UIText.START_HEADER_TEXT && header.isNotEmpty()) {
            header
        } else {
            // take the first word of the note, remove punctuation
            val note = text
            var fileName = note
                .substringBefore(' ')
                .trim { !it.isLetterOrDigit() }

            // limit the word to the maximum allowed number of characters
            if (fileName.length > NOTE_TITLE_MAX_LENGTH) fileName = fileName.take(NOTE_TITLE_MAX_LENGTH)

            if (fileName.isEmpty()) fileName = UIText.EMPTY_NOTE_TITLE

            fileName
        }
        return fileName
    }

    /**
     * Updates an existing note with a new [name][noteName] or [text] if the [id] of the note is provided.
     * Creates a new note if the provided [id] is null.
     *
     * @param id id of the note.
     * @param noteName name of the note.
     * @param text text of the note.
     *
     * @throws HeaderTooLongException if the name of the note is larger than [NOTE_TITLE_MAX_LENGTH].
     *
     * @return the [id] of the created or updated note.
     */
    fun saveNote(id: Long?, noteName: String, text: String): Long {
        if (noteName.length > NOTE_TITLE_MAX_LENGTH)
            throw HeaderTooLongException(NOTE_TITLE_MAX_LENGTH)
        if (noteName.isBlank()) throw BlankNoteNameException()

        return if (id == null) {
            noteRepository.insertNote(noteName, text)
        } else {
            noteRepository.updateNote(id, noteName, text)
            id
        }
    }

    fun deleteNote(id: Long) = noteRepository.deleteNote(id)

    fun selectNote(id: Long): String = noteRepository.selectNote(id)

    fun loadAllNotes(): List<NoteListItem> = noteRepository.loadAllNotes()
}