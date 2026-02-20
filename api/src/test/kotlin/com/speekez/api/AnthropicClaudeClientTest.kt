package com.speekez.api

import com.speekez.api.model.AnthropicContent
import com.speekez.api.model.AnthropicResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnthropicClaudeClientTest {

    private val api = mockk<AnthropicApi>()
    private val client = AnthropicClaudeClient("test-key", api)

    @Test
    fun `refine returns expected text`() = runTest {
        val rawText = "hello"
        val systemPrompt = "be nice"
        val refinedText = "Hello!"

        val response = AnthropicResponse(
            content = listOf(
                AnthropicContent(type = "text", text = refinedText)
            )
        )

        coEvery { api.refine(any(), any(), any()) } returns response

        val result = client.refine(rawText, "claude-3", systemPrompt)

        assertEquals(refinedText, result)
    }
}
