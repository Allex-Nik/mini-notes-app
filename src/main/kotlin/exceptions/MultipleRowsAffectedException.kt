package org.education.exceptions

// is it correct to use RuntimeException here and in other repository-related exceptions?
// I understood that IllegalStateException should not be used here,
// but I'm not sure if IllegalArgumentException is appropriate either
class MultipleRowsAffectedException(
    message: String = "Attempt to update multiple notes. Nothing was updated."
) : RuntimeException(message)