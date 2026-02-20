package com.speekez.api

import com.speekez.api.model.OpenRouterRefinementChoice
import com.speekez.api.model.OpenRouterRefinementMessage
import com.speekez.api.model.OpenRouterRefinementResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenRouterClaudeClientTest {

    private val api = mockk<OpenRouterRefinementApi>()
    private val client = OpenRouterClaudeClient("test-key", api)

    @Test
    fun `refine returns expected text`() = runTest {
        val rawText = "hello"
        val systemPrompt = "be nice"
        val refinedText = "Hello!"

        val response = OpenRouterRefinementResponse(
            choices = listOf(
                OpenRouterRefinementChoice(
                    message = OpenRouterRefinementMessage(role = "assistant", content = refinedText)
                )
            )
        )

        coEvery { api.refine(any(), any()) } returns response

        val result = client.refine(rawText, "claude-3", systemPrompt)

        assertEquals(refinedText, result)
    }
}
