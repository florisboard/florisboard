package com.speekez.api

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class OpenAiWhisperClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OpenAiWhisperClient
    private lateinit var tempFile: File

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(OpenAiApi::class.java)
        client = OpenAiWhisperClient(api, "test_key")

        tempFile = File.createTempFile("test_audio", ".m4a")
        tempFile.writeText("dummy audio content")
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        tempFile.delete()
    }

    @Test
    fun `transcribe success returns text`() = runBlocking {
        val responseJson = "{\"text\": \"Hello world\"}"
        server.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.transcribe(tempFile, "whisper-1", listOf("en"))

        assertEquals("Hello world", result)
        val request = server.takeRequest()
        assertEquals("Bearer test_key", request.getHeader("Authorization"))
    }

    @Test
    fun `transcribe handles 401 error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val exception = assertThrows<IllegalStateException> {
            client.transcribe(tempFile, "whisper-1", listOf("en"))
        }

        assertEquals("Invalid OpenAI API Key", exception.message)
    }

    @Test
    fun `transcribe handles 429 error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))

        val exception = assertThrows<IllegalStateException> {
            client.transcribe(tempFile, "whisper-1", listOf("en"))
        }

        assertEquals("OpenAI Rate Limit Exceeded", exception.message)
    }
}
