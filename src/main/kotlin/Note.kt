package org.education

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import java.time.Instant

@Entity
@Table(name = "notes")
data class Note (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val creationDateTime: Instant?,
    val lastEditedDateTime: Instant?,
    /**
     * Makes the type of the field varchar(60). Therefore, if we do it here, the constraint also works in JDBC.
     * If the database already has rows violating this constraint, the type does not change,
     * But the constraint is still enforced.
     */
    @field:Length(max = MAX_CHARACTERS)
    val title: String,
    @Column(columnDefinition = "TEXT")
    val text: String,
    val removed: Boolean
)