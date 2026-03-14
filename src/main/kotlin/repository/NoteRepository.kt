package org.education.repository

import org.education.exceptions.*
import org.education.model.NoteData
import org.education.model.NoteListItem

/**
 * Repository responsible for CRUD operations on notes in a database.
 */
internal interface NoteRepository {
    /**
     * Inserts a new note into the database.
     *
     * @param title name of the note to create.
     * @param text text (content) of the note to create.
     *
     * @throws NoteNotInsertedException if the note was not inserted into the database and its ID is not returned.
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
     * @throws NoteNotFoundException if no note was updated.
     * @throws MultipleRowsAffectedException if there was an attempt to update multiple notes.
     */
    fun updateNote(id: Long, title: String, text: String)

    /**
     * Returns the title and text of the note with the given [id].
     *
     * @param id ID of the note to select.
     *
     * @throws NoteNotFoundException if no note with the given [id] was found.
     * @throws NonUniqueNoteException if multiple notes with the given [id] were found.
     *
     * @return title and text of the selected note.
     */
    fun selectNote(id: Long): NoteData

    /**
     * Marks the note with the given [id] in the database as removed. Changes exactly one row in the database.
     *
     * @param id ID of the note to delete.
     *
     * @throws NoteNotDeletedException if no note was deleted.
     * @throws MultipleRowsAffectedException if there was an attempt to delete multiple notes.
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