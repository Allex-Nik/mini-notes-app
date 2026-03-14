package org.education.exceptions

class UnexpectedCurrentNoteId(
    message: String = "currentNoteId must not be null here"
) : IllegalStateException(message)