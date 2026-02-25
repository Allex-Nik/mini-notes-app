package org.education.exceptions


/**
 * REVIEW:
 * 1. Remove message duplication and pass domain data
 *
 * Better:
 *
 * class HeaderTooLongException(
 *     maxLength: Int,
 *     message: String = "Header length must not exceed $maxLength characters"
 * ) : IllegalArgumentException(message)
 *
 * 2. Use IllegalArgumentException
 *
 * Again, this is an argument validation failure, so IllegalArgumentException is semantically correct.
 *
 * General recommendations for exceptions in this project
 *
 * Inherit from the most specific meaningful base class (IllegalArgumentException for validation).
 *
 * Provide default messages to eliminate duplication in the service layer.
 *
 * Pass domain parameters (like maxLength) instead of formatting strings outside.
 *
 * Limit visibility to internal if not part of a public API.
 *
 * Keep exception classes simple—no logic, no state beyond what is required to describe the error.
 *
 */
class HeaderTooLongException(override val message: String) : Exception(message)