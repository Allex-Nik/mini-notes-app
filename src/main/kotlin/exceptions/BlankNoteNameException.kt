package org.education.exceptions

/**
 * REVIEW:
 * 1. Provide a default message
 * class BlankNoteNameException(
 *     message: String = "Note name must not be blank"
 * ) : IllegalArgumentException(message)
 *
 * This removes duplication of the error message in the service layer and makes usage cleaner:
 *
 * 2. Use a more specific base class
 * Since this is an input validation error, IllegalArgumentException is more appropriate than RuntimeException. It communicates intent more clearly.
 *
 * 3. Restrict visibility
 *
 * If the exception is only used inside the module: internal
 *
 */
class BlankNoteNameException(override val message: String) : Exception(message)