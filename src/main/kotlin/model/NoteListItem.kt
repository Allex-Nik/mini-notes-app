package org.education.model

import java.time.Instant

/**
 * REVIEW:
 *
 * 1. Consider making timestamps non-nullable if DB guarantees presence.
 *    Explanation: loadAllNotes() in repositories selects creationDateTime and lastEditedDateTime
 *    directly from the entity without null checks. If the entity fields are non-nullable
 *    in the DB, keeping them nullable here weakens the type safety and allows invalid states.
 *
 * 2. Ensure consistency with Note entity nullability.
 *    Explanation: If Note.creationDateTime and Note.lastEditedDateTime are made non-nullable
 *    (recommended for domain invariants), NoteListItem should mirror that contract.
 *    Projection models should not silently widen nullability compared to the entity.
 *
 * 3. Keep data class (appropriate here).
 *    Explanation: This is a projection/DTO used in repositories (SELECT new ... NoteListItem)
 *    and in UI (JList model). It is not a JPA entity. Therefore, data class is correct:
 *    structural equality and auto-generated methods are beneficial and safe.
 *
 * 4. toString() override is correct but should be documented.
 *    Explanation: MainWindow uses DefaultListModel<NoteListItem> and JList,
 *    which relies on toString() for rendering. Overriding toString() to return title
 *    is intentional UI behavior and should be explicitly documented.
 *
 * 5. Consider documenting projection intent in KDoc.
 *    Explanation: This class is constructed via HQL constructor expression:
 *    SELECT new org.education.model.NoteListItem(...)
 *    Making its purpose explicit avoids accidental misuse as a domain entity.
 *
 * 6. Evaluate whether lastEditedDateTime is actually used.
 *    Explanation: In MainWindow, only id and title are used for display/selection.
 *    If lastEditedDateTime is not used anywhere in UI or service logic,
 *    it may be unnecessary data transfer from the repository.
 *
 * 7. Visibility.
 *    Explanation: If this class is only used inside the module (repository + UI),
 *    consider marking it internal to reduce public API surface.
 */
data class NoteListItem(
    val id: Long,
    val creationDateTime: Instant?,
    val lastEditedDateTime: Instant?,
    val title: String
) {
    override fun toString(): String = title
}