package service

import org.education.model.Note
import org.education.model.NoteListItem
import org.education.repository.NoteRepository
import java.time.Instant

// Assumes that the notes are not deleted, and only the "remove" flag is changed
class NoteRepositoryMock : NoteRepository {
    private var nextId = 1L
    private val notes = mutableListOf<Note>()

    override fun createNotesTableIfNotExists() {}

    override fun insertNote(title: String, text: String): Long {
        val id = nextId++
        val now = Instant.now()
        val note = Note(id, now, now, title, text, false)
        notes.add(note)
        return id
    }

    override fun updateNote(id: Long, title: String, text: String) {
        val listIndex = (id - 1).toInt()
        val note = notes.getOrNull(listIndex) ?: error("Note with id=$id not found")
        if (note.removed) error("Note with id=$id was already removed")
        notes[listIndex] = note.copy(lastEditedDateTime = Instant.now(), title = title, text = text)
    }

    override fun selectNote(id: Long): String {
        val listIndex = (id - 1).toInt()
        val note = notes.getOrNull(listIndex) ?: error("Note with id=$id not found")
        if (note.removed) error("Note with id=$id was already removed")
        return note.text
    }

    override fun deleteNote(id: Long): Int {
        val listIndex = (id - 1).toInt()
        val note = notes.getOrNull(listIndex) ?: error("Note with id=$id not found")
        if (note.removed) error("Note with id=$id was already removed")
        notes[listIndex] = note.copy(removed = true)
        return 1
    }

    override fun loadAllNotes(): List<NoteListItem> =
        notes
            .filter { !it.removed }
            .map {
                NoteListItem(
                    id = it.id ?: error("id is missing"),
                    creationDateTime = it.creationDateTime,
                    lastEditedDateTime = it.lastEditedDateTime,
                    title = it.title
                )
            }

    fun size() = notes.size
}