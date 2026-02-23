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