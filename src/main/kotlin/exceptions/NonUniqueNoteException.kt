package org.education.exceptions

internal class NonUniqueNoteException(
    message: String = "Multiple notes were obtained, while a unique note was expected"
) : RuntimeException(message)