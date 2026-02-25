package org.education.repository

import org.education.model.NoteListItem

/**
 * REVIEW:
 *
 * 1. Inconsistent mutation semantics.
 *    Explanation:
 *      - insertNote returns Long (new id)
 *      - updateNote returns Unit
 *      - deleteNote returns Int (affected rows)
 *
 *      In the current codebase:
 *        - NoteService.deleteNote() simply forwards the Int.
 *        - MainWindow.removeNote() ignores the returned value completely.
 *        - Hibernate implementation already enforces affected row checks internally.
 *
 *      Therefore, returning Int from deleteNote() does not provide meaningful value
 *      to upper layers and leaks persistence detail. Mutation methods should either:
 *        - enforce single-row semantics internally and return Unit, or
 *        - consistently return affected row count across all mutating methods.
 *
 *
 * 2. updateNote(id: Long, ...) does not define behavior for non-existing id.
 *    Explanation:
 *      In the UI flow, updateNote is called assuming the note exists.
 *      No null handling or fallback logic exists in service or UI.
 *
 *      If zero rows are affected:
 *        - Is this an error?
 *        - Should it throw?
 *
 *      Current contract does not define this invariant, but the application
 *      assumes exactly one row is updated.
 *
 *
 * 3. selectNote(id: Long): String assumes existence.
 *    Explanation:
 *      In MainWindow.isNoteChanged(), selectNote(currentNoteId) is called
 *      without null checks.
 *
 *      This implies:
 *        - Either repository guarantees existence and throws otherwise,
 *        - Or absence is impossible by design.
 *
 *      The signature does not communicate this guarantee.
 *
 *
 * 4. loadAllNotes() does not define ordering.
 *    Explanation:
 *      MainWindow.readNotes() loads notes and displays them directly
 *      without additional sorting.
 *
 *      Therefore ordering must be guaranteed by repository.
 *      Different implementations (JDBC vs Hibernate) may return different order
 *      if ORDER BY is not enforced.
 *
 *
 * 5. createNotesTableIfNotExists() mixes schema initialization with repository contract.
 *    Explanation:
 *      This method is called from MainWindow.init().
 *      That makes UI responsible for schema setup.
 *
 *      While acceptable in an educational context,
 *      it means repository now has infrastructure responsibility,
 *      not just data access responsibility.
 *
 *
 * 6. Two implementations require stronger contract clarity.
 *    Explanation:
 *      Both JDBC and Hibernate implementations exist.
 *      Without clearly defined behavioral guarantees
 *      (single-row mutation, ordering, exception semantics),
 *      the two implementations may diverge subtly.
 *
 *      Interface should enforce consistent behavior expectations
 *      across both implementations.
 *
 *
 * 7. Visibility.
 *    Explanation:
 *      If not intended for public reuse, interface should be internal
 *      to reduce API surface.
 */
interface NoteRepository {
    fun createNotesTableIfNotExists()
    fun insertNote(title: String, text: String): Long
    fun updateNote(id: Long, title: String, text: String)
    fun selectNote(id: Long): String
    fun deleteNote(id: Long): Int
    fun loadAllNotes(): List<NoteListItem>
}