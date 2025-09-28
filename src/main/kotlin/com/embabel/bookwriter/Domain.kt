package com.embabel.bookwriter

import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.prompt.persona.Persona
import com.embabel.common.core.types.Timestamped
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Book(
    val topic: String,
)

data class ReviewedBook(
    val book: Book,
    val review: String,
    val reviewer: Persona,
) : HasContent, Timestamped {

    override val timestamp: Instant
        get() = Instant.now()

    override val content: String
        get() = """
            # Book
            ${book.topic}

            # Review
            $review

            # Reviewer
            ${reviewer.name}, ${
            timestamp.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
        }
        """.trimIndent()
}
