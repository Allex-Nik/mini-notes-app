package org.education.repository

import org.education.model.NoteListItem

/**
 * Repository responsible for CRUD operations on notes in a database.
 */
internal interface NoteRepository {
    fun createNotesTableIfNotExists() // TODO: move from repository

    /**
     * Inserts a new note into the database.
     *
     * @param title name of the note to create.
     * @param text text (content) of the note to create.
     *
     * @throws IllegalStateException if the note was not inserted into the database and its ID is not returned.
     * @return the ID of the inserted note.
     */
    fun insertNote(title: String, text: String): Long

    /**
     * Updates an existing note with a new given [title] and [text].
     * The note must exist in the database and must not be marked as removed.
     *
     * @param id ID of the note to update.
     * @param title new name of the note.
     * @param text new text (content) of the note.
     *
     * @throws IllegalArgumentException if the number of rows affected by the update is not 1.
     */
    fun updateNote(id: Long, title: String, text: String)

    /**
     * Returns the text of the note with the given [id].
     *
     * @param id ID of the note to select.
     *
     * @throws IllegalArgumentException if the number of rows with the given [id] is not 1.
     *
     * @return text of the selected note.
     */
    fun selectNote(id: Long): String

    /**
     * Marks the note with the given [id] in the database as removed. Changes exactly one row in the database.
     *
     * @param id ID of the note to delete.
     *
     * @throws IllegalArgumentException if the number of rows affected by the deletion is not 1.
     */
    fun deleteNote(id: Long)

    /**
     * Returns a list of all notes in the database without their texts. Removed notes are not returned.
     * The resulting list is sorted by the lastEditedDateTime in descending order and by id in descending order.
     *
     * @return list of all notes without their texts,
     * sorted by lastEditedDateTime in descending order and by id in descending order.
     */
    fun loadAllNotes(): List<NoteListItem>
}