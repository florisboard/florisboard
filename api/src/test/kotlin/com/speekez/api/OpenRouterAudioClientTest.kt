package com.speekez.api

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

class OpenRouterAudioClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OpenRouterAudioClient
    private lateinit var tempFile: File

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(OpenRouterApi::class.java)
        client = OpenRouterAudioClient(api, "test_or_key")

        tempFile = File.createTempFile("test_audio_or", ".m4a")
        tempFile.writeText("dummy audio content for or")
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        tempFile.delete()
    }

    @Test
    fun `transcribe success returns text`() = runBlocking {
        val responseJson = """
            {
              "choices": [
                {
                  "message": {
                    "content": "Transcribed text from OpenRouter"
                  }
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(responseJson).setResponseCode(200))

        val result = client.transcribe(tempFile, "openai/whisper-large-v3-turbo", emptyList())

        assertEquals("Transcribed text from OpenRouter", result)
        val request = server.takeRequest()
        assertEquals("Bearer test_or_key", request.getHeader("Authorization"))
        assertEquals("SpeekEZ Android Keyboard", request.getHeader("X-Title"))
    }

    @Test
    fun `transcribe handles 402 error`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(402))

        val exception = assertThrows<IllegalStateException> {
            client.transcribe(tempFile, "model", emptyList())
        }

        assertEquals("OpenRouter Payment Required / Credits Depleted", exception.message)
    }
}
