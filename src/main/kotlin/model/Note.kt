package org.education.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.education.ui.MAX_CHARACTERS
import org.hibernate.validator.constraints.Length
import java.time.Instant


/**
 * REVIEW:
 *
 * 1. Replace data class with regular class.
 *    Explanation: data class generates equals()/hashCode() based on all properties.
 *    In JPA entities equality should usually rely on identity (id) only.
 *    Hibernate proxies can also break data-class-generated equality semantics.
 *
 * 2. Replace val with var for JPA compatibility.
 *    Explanation: Hibernate sets fields via reflection and may require mutability.
 *    Immutable val properties can cause issues with lazy loading and proxy initialization.
 *
 * 3. Add default values to satisfy JPA.
 *    Explanation: JPA requires a no-arg constructor (spec requirement).
 *    In Kotlin this is typically achieved by providing default values for parameters.
 *
 * 4. Remove nullable timestamps if possible.
 *    Explanation: Nullable fields allow invalid states at the type level.
 *    If timestamps are always present in the DB, they should be non-nullable
 *    to enforce domain invariants at compile time.
 *
 * 5. Add @Column(nullable = false) where appropriate.
 *    Explanation: Makes DB constraints explicit and prevents silent schema drift.
 *    Without it, the database may allow null even if business logic forbids it.
 *
 * 6. Replace columnDefinition = "TEXT" with @Lob if portability matters.
 *    Explanation: columnDefinition ties the entity to a specific SQL dialect.
 *    @Lob delegates type selection to Hibernate, improving cross-database portability.
 *
 * 7. Remove UI-layer constant dependency.
 *    Explanation: Entity layer must not depend on UI layer.
 *    This violates clean layering and creates unnecessary coupling between persistence and presentation.
 */
@Entity
@Table(name = "notes")
data class Note (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val creationDateTime: Instant?,
    val lastEditedDateTime: Instant?,
    @field:Length(max = MAX_CHARACTERS)
    val title: String,
    @Column(columnDefinition = "TEXT")
    val text: String,
    val removed: Boolean
)