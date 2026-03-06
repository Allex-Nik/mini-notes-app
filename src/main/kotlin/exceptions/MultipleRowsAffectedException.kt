package org.education.exceptions

class MultipleRowsAffectedException(
    message: String = "Attempt to update multiple notes. Nothing was updated."
) : IllegalStateException(message)