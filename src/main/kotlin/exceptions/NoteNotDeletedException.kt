package org.education.exceptions

class NoteNotDeletedException(
    message: String = "Note was not deleted"
) : IllegalStateException(message)