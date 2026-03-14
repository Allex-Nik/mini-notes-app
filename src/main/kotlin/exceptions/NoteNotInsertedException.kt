package org.education.exceptions

internal class NoteNotInsertedException(
    message: String = "Note was not inserted"
) : RuntimeException(message)