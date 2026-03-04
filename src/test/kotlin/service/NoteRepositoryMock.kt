package service

import org.education.model.Note
import org.education.model.NoteListItem
import org.education.repository.NoteRepository
import java.time.Instant

// Assumes that the notes are not deleted, and only the "remove" flag is changed
internal class NoteRepositoryMock : NoteRepository {
    private var nextId = 1L
    private val notes = mutableListOf<Note>()

    override fun createNotesTableIfNotExists() {}

    override fun insertNote(title: String, text: String): Long {
        val id = nextId++
        val now = Instant.now()
        val note = Note().apply {
            this.id = id
            this.creationDateTime = now
            this.lastEditedDateTime = now
            this.title = title
            this.text = text
            this.removed = false
        }
        notes.add(note)
        return id
    }

    override fun updateNote(id: Long, title: String, text: String) {
        val listIndex = (id - 1).toInt()
        val initialNote = notes.getOrNull(listIndex) ?: error("Note with id=$id not found")
        if (initialNote.removed) error("Note with id=$id was already removed")
        val updatedNote = Note().apply {
            this.id = initialNote.id
            this.creationDateTime = initialNote.creationDateTime
            this.lastEditedDateTime = Instant.now()
            this.title = title
            this.text = text
            this.removed = initialNote.removed
        }
        notes[listIndex] = updatedNote
    }

    override fun selectNote(id: Long): String {
        val listIndex = (id - 1).toInt()
        val note = notes.getOrNull(listIndex) ?: error("Note with id=$id not found")
        if (note.removed) error("Note with id=$id was already removed")
        return note.text
    }

    override fun deleteNote(id: Long) {
        val listIndex = (id - 1).toInt()
        val note = notes.getOrNull(listIndex) ?: error("Note with id=$id not found")
        if (note.removed) error("Note with id=$id was already removed")
        val removedNote = Note().apply {
            this.id = note.id
            this.creationDateTime = note.creationDateTime
            this.lastEditedDateTime = Instant.now()
            this.title = note.title
            this.text = note.text
            this.removed = true
        }
        notes[listIndex] = removedNote
    }

    override fun loadAllNotes(): List<NoteListItem> =
        notes
            .filter { !it.removed }
            .map {
                NoteListItem(
                    id = it.id ?: error("id is missing"),
                    title = it.title
                )
            }

    fun size() = notes.size
}