package org.education.repository

import org.education.model.NoteListItem

interface NoteRepository {
    fun createNotesTableIfNotExists()
    fun insertNote(title: String, text: String): Long
    fun updateNote(id: Long, title: String, text: String)
    fun selectNote(id: Long): String
    fun deleteNote(id: Long): Int
    fun loadAllNotes(): List<NoteListItem>
}