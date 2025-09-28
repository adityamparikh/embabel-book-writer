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
import com.embabel.agent.core.CoreToolGroups.WEB
import com.embabel.agent.domain.io.UserInput
import com.embabel.bookwriter.Book
import com.embabel.bookwriter.ReviewedBook
import com.embabel.common.ai.model.LlmOptions
import org.springframework.beans.factory.annotation.Value

@Agent(
    description = "Write a book based on user input and review it",
)
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
        export = Export(remote = true, name = "writeAndReviewBook")
    )
    @Action
    fun reviewBook(userInput: UserInput, book: Book, context: OperationContext): ReviewedBook {
        val review = context.ai()
            .withLlm(LlmOptions.withAutoLlm().withTemperature(0.2))
            .withPromptContributor(Reviewer)
            .generateText(
                """
            You will be given a book to review.
            Review it in $reviewWordCount words or less.
            Consider whether or not the style is engaging, and well-written.
            Also consider whether the topics incorporate given the original user input.

            # Topic
            ${book.topic}

            # User input that inspired the story
            ${userInput.content}
        """.trimIndent()
            )
        return ReviewedBook(
            book = book,
            review = review,
            reviewer = Reviewer,
        )
    }

}
