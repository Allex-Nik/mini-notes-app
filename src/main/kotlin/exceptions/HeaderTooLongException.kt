package org.education.exceptions

internal class HeaderTooLongException(
    maxLength: Int,
    message: String = "Header length must not exceed $maxLength characters"
) : IllegalArgumentException(message)