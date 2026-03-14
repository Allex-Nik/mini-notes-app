package org.education.exceptions

internal class NoteNotFoundException(
    message: String = """Note was not found. 
        |It might have been deleted externally.""".trimMargin()
) : RuntimeException(message)