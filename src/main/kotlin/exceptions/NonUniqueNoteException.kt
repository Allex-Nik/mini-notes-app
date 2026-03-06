package org.education.exceptions

class NonUniqueNoteException(
    message: String = "Multiple notes were obtained, while a unique note was expected"
) : IllegalStateException(message)