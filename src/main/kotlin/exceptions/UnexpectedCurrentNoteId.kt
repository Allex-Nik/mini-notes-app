package org.education.exceptions

internal class UnexpectedCurrentNoteId(
    message: String = "currentNoteId must not be null here"
) : IllegalStateException(message)