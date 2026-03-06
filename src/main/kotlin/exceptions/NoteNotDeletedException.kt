package org.education.exceptions

class NoteNotDeletedException(
    message: String = "Note was not deleted"
) : RuntimeException(message)