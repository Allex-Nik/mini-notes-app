package org.education.exceptions

class NoteNotInsertedException(
    message: String = "Note was not inserted"
) : IllegalStateException(message)