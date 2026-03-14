package org.education.model

/**
 * A lightweight Data Transfer Object containing reduced information from [Note] entity:
 * the title and the text of the note.
 * It is not a JPA/Hibernate entity.
 */
internal data class NoteData(
    val title: String,
    val text: String
)