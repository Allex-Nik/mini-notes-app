package org.education.model

/**
 * A lightweight Data Transfer Object containing reduced information from [Note] entity.
 * Represents a note in the list of notes. It is not a JPA/Hibernate entity.
 */
internal data class NoteListItem(
    val id: Long,
    val title: String
) {
    /**
     * Returns the [title] of the note.
     *
     * The method is overridden because in the UI the instances of the [NoteListItem] class are put into a JList
     * to be shown to the user as a list of the notes. JList shows the `toString()` return value
     * of the object. Therefore, the `toString()` method should return the [title] of the note, so that the user
     * sees only the titles of the notes in the list.
     */
    override fun toString(): String = title
}