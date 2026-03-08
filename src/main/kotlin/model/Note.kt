package org.education.model

import jakarta.persistence.*
import org.hibernate.validator.constraints.Length
import java.time.Instant

internal const val NOTE_TITLE_MAX_LENGTH = 60

// we can use jpa-plugin instead of default values: https://www.baeldung.com/kotlin/jpa#compiler-plugins-jpa-plugin
@Entity
@Table(name = "notes")
class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var creationDateTime: Instant = Instant.now()

    @Column(nullable = false)
    var lastEditedDateTime: Instant = Instant.now()

    @field:Length(max = NOTE_TITLE_MAX_LENGTH)
    @Column(nullable = false, length = NOTE_TITLE_MAX_LENGTH)
    var title: String = ""

    // @Lob doesn't work correctly here: Hibernate consistently maps it to TINYTEXT,
    // no matter the initial type of the column or if it is a new table or an existing one
    @Column(columnDefinition = "TEXT", nullable = false)
    var text: String = ""

    @Column(nullable = false)
    var removed: Boolean = false
}