package org.education

import java.time.Instant

data class NoteListItem(
    val id: Long,
    val creationDateTime: Instant?,
    val lastEditedDateTime: Instant?,
    val title: String
) {
    override fun toString(): String = title
}