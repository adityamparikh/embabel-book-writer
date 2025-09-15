package com.embabel.bookwriter.agent

import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.testing.unit.FakeOperationContext
import com.embabel.agent.testing.unit.FakePromptRunner
import com.embabel.agent.testing.unit.LlmInvocation
import org.junit.jupiter.api.Assertions
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

        context.expectResponse(Book("One upon a time Sir Galahad . . "))

        agent.writeBook(
            UserInput("Write a book about langchain4j", Instant.now()),
            context
        )

        // Verify the prompt contains the expected keyword
        Assertions.assertTrue(
            promptRunner.llmInvocations.first().prompt.contains("langchain4j"),
            "Expected prompt to contain 'langchain4j'"
        )


        // Verify the temperature setting for creative output
        val actual = promptRunner.llmInvocations.first().interaction.llm.temperature
        Assertions.assertEquals(
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

        // Create fake context and set expected response
        val context = FakeOperationContext.create()
        context.expectResponse("Explains basics as well as advanced concepts with examples")

        // Execute the review
        agent.reviewStory(userInput, book, context)

        // Verify the LLM invocation contains expected content
        val llmInvocation: LlmInvocation =
            context.llmInvocations.singleOrNull()
                ?: error("Expected a single LLM invocation, not ${context.llmInvocations.single()}")
        Assertions.assertTrue(
            llmInvocation.prompt.contains("langchain4j"),
            "Expected prompt to contain 'langchain4j'"
        )
        Assertions.assertTrue(
            llmInvocation.prompt.contains("review"),
            "Expected prompt to contain 'review'"
        )
    }
}