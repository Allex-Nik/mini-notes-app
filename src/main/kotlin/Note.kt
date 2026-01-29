package org.education

import java.time.Instant

data class Note(val id: Long, val creationDateTime: Instant?, val title: String) {
    override fun toString(): String = title
}