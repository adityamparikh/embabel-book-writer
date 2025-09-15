/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.bookwriter.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.Export
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.core.CoreToolGroups
import com.embabel.agent.core.CoreToolGroups.WEB
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.prompt.persona.Persona
import com.embabel.agent.prompt.persona.RoleGoalBackstory
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.core.types.Timestamped
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val writer = RoleGoalBackstory(
    role = "An engaging passionate educator who loves to teach programming and software",
    goal = "Teach engineers experienced or not in a manner captivate the reader's imagination.",
    backstory = "You have been teaching programming for as long as you can remember. Your style is engaging with fun analogies and code examples that leave a lasting impression on your audience.",
)

val Reviewer = Persona(
    name = "Programming Book Review",
    persona = "Someone like Dr. Venkat Subramaniam",
    voice = "Professional and insightful",
    objective = "Help educate readers on the topic that is the focus of the book",
)

data class Book(
    val topic: String,
)

data class ReviewedStory(
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


@Agent(
    description = "Write a book based on user input and review it",
)
@Profile("!test")
class WriteAndReviewAgent(
    @param:Value("\${storyWordCount:3000}") private val bookWordCount: Int,
    @param:Value("\${reviewWordCount:100}") private val reviewWordCount: Int,
) {

    @Action
    fun writeBook(userInput: UserInput, context: OperationContext): Book =
        context.ai()
            .withLlm(LlmOptions.withAutoLlm().withTemperature(0.7))
            .withPromptContributor(writer)
            .withToolGroup(WEB)
            .create(
                """
            Write a book in about $bookWordCount words.
            The content should be engaging and with examples.
            Use the web to research.
            If the user has provided topics, include it in the outline.

            # User input
            ${userInput.content}
        """.trimIndent()
            )

    @AchievesGoal(
        description = "The book has been created and reviewed by a professional",
        export = Export(remote = true, name = "writeAndReviewStory")
    )
    @Action
    fun reviewStory(userInput: UserInput, book: Book, context: OperationContext): ReviewedStory {
        val review = context.ai()
            .withAutoLlm()
            .withPromptContributor(Reviewer)
            .generateText(
                """
            You will be given a programming and software book to review.
            Review it in $reviewWordCount words or less.
            Consider whether or not the style is engaging, and well-written.
            Also consider whether the topics incorporate given the original user input.

            # Topic
            ${book.topic}

            # User input that inspired the story
            ${userInput.content}
        """.trimIndent()
            )
        return ReviewedStory(
            book = book,
            review = review,
            reviewer = Reviewer,
        )
    }

}
