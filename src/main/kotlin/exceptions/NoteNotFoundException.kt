package org.education.exceptions

class NoteNotFoundException(
    message: String = """Note was not found. 
        |It might have been deleted externally.""".trimMargin()
) : IllegalStateException(message)