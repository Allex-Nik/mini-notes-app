package org.education.exceptions

internal class NoteNotDeletedException(
    message: String = "Note was not deleted"
) : RuntimeException(message)