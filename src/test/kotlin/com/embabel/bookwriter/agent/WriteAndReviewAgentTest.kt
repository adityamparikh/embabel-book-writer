package com.embabel.bookwriter.agent

import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.unit.FakeOperationContext
import com.embabel.agent.testing.unit.FakePromptRunner
import com.embabel.agent.testing.unit.LlmInvocation
import com.embabel.bookwriter.Book
import com.embabel.bookwriter.ReviewedBook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant


/**
 * Unit tests for the WriteAndReviewAgent class.
 * Tests the agent's ability to craft and review stories based on user input.
 */
internal class WriteAndReviewAgentTest {

    /**
     * Tests the story crafting functionality of the WriteAndReviewAgent.
     * Verifies that the LLM call contains expected content and configuration.
     */
    @Test
    fun testWriteBook() {
        // Create agent with word limits: 200 min, 400 max
        val agent = WriteAndReviewAgent(200, 400)
        val context = FakeOperationContext.create()
        val promptRunner = context.promptRunner() as FakePromptRunner

        context.expectResponse(Book("LangChain for Java: Supercharge your Java application with the power of LLMs"))

        agent.writeBook(
            UserInput("Write a book about langchain4j", Instant.now()),
            context
        )

        // Verify the prompt contains the expected keyword
        assertTrue(
            promptRunner.llmInvocations.first().prompt.contains("langchain4j"),
            "Expected prompt to contain 'langchain4j'"
        )

        // Verify the temperature setting for creative output
        val actual = promptRunner.llmInvocations.first().interaction.llm.temperature
        assertEquals(
            0.7, actual!!, 0.01,
            "Expected temperature to be 0.7: Higher for more creative output"
        )
    }

    /**
     * Tests the story review functionality of the WriteAndReviewAgent.
     * Verifies that the review process includes expected prompt content.
     */
    @Test
    fun testReview() {
        // Create agent with word limits
        val agent = WriteAndReviewAgent(200, 400)

        // Set up test data
        val userInput = UserInput("Write me a book about langchain4j", Instant.now())
        val book = Book("LangChain for Java: Supercharge your Java application with the power of LLMs")
        val review = ReviewedBook(book, "Explains basics as well as advanced concepts with examples", Reviewer)

        // Create fake context and set expected response
        val context = FakeOperationContext.create()
        context.expectResponse("Explains basics as well as advanced concepts with examples")

        // Execute the review
        agent.reviewBook(userInput, book, context)

        // Verify the LLM invocation contains expected content
        val llmInvocation: LlmInvocation =
            context.llmInvocations.singleOrNull()
                ?: error("Expected a single LLM invocation, not ${context.llmInvocations.single()}")
        assertTrue(
            llmInvocation.prompt.contains("langchain4j"),
            "Expected prompt to contain 'langchain4j'"
        )
        assertTrue(
            llmInvocation.prompt.contains("review") || llmInvocation.prompt.contains("critique"),
            "Expected prompt to contain 'review' or 'critique'"
        )
    }

    @Test
    fun shouldHandleMultipleLlmInteractions() {
        // Arrange
        // Create agent with word limits
        val agent = WriteAndReviewAgent(200, 400)

        // Set up test data
        val userInput = UserInput("Write me a book about langchain4j", Instant.now())
        val book = Book("LangChain for Java: Supercharge your Java application with the power of LLMs")
        val review = ReviewedBook(book, "Explains basics as well as advanced concepts with examples", Reviewer)

        // Create fake context and set expected response
        val context = FakeOperationContext.create()
        context.expectResponse(book)
        context.expectResponse("Explains basics as well as advanced concepts with examples")

        // Act
        val writtenBook : Book = agent.writeBook(userInput, context)
        val reviewedBook : ReviewedBook = agent.reviewBook(userInput, book, context)

        // Assert
        assertEquals(book, writtenBook)
        assertEquals(review, reviewedBook)

        // Verify both LLM calls were made
        val invocations = context.llmInvocations
        assertEquals(2, invocations.size)

        // Verify first call (writer)
        val writerCall = invocations.get(0)
        assertEquals(0.7, writerCall.interaction.llm.temperature!!, 0.01)

        // Verify second call (reviewer)
        val reviewerCall = invocations.get(1)
        assertEquals(0.2, reviewerCall.interaction.llm.temperature!!, 0.01)
    }
}