package org.education

data class Note(val id: Long, val title: String) {
    override fun toString(): String = title
}