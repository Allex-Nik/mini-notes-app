package org.education.exceptions

internal class BlankNoteNameException(
    message: String = "Note name must not be blank"
) : IllegalArgumentException(message)