package com.speekez.api

import java.util.Base64
import retrofit2.HttpException
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun transcribe(
        @Header("Authorization") auth: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") title: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

data class OpenRouterMessage(
    val role: String,
    val content: Any // Can be String or List<OpenRouterContent>
)

data class OpenRouterContent(
    val type: String,
    val text: String? = null,
    val input_audio: OpenRouterAudio? = null
)

data class OpenRouterAudio(
    val data: String,
    val format: String
)

data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

data class OpenRouterChoice(
    val message: OpenRouterResponseMessage
)

data class OpenRouterResponseMessage(
    val content: String?
)

class OpenRouterAudioClient(
    private val api: OpenRouterApi,
    private val apiKey: String
) : SttClient {
    override suspend fun transcribe(audioFile: File, model: String, languages: List<String>): String {
        val audioBytes = audioFile.readBytes()
        val base64Audio = Base64.getEncoder().encodeToString(audioBytes)

        val request = OpenRouterRequest(
            model = model,
            messages = listOf(
                OpenRouterMessage(
                    role = "system",
                    content = "Transcribe the following audio exactly. Return only the transcribed text."
                ),
                OpenRouterMessage(
                    role = "user",
                    content = listOf(
                        OpenRouterContent(
                            type = "input_audio",
                            input_audio = OpenRouterAudio(
                                data = base64Audio,
                                format = "m4a" // AudioHandler uses AAC/MPEG_4 (.m4a)
                            )
                        )
                    )
                )
            )
        )

        return try {
            val response = api.transcribe(
                auth = "Bearer $apiKey",
                referer = "https://github.com/speekez/speekez",
                title = "SpeekEZ Android Keyboard",
                request = request
            )
            response.choices.firstOrNull()?.message?.content?.trim() ?: ""
        } catch (e: HttpException) {
            handleError(e)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun handleError(e: HttpException): Nothing {
        val code = e.code()
        when (code) {
            401 -> throw IllegalStateException("Invalid OpenRouter API Key")
            402 -> throw IllegalStateException("OpenRouter Payment Required / Credits Depleted")
            429 -> throw IllegalStateException("OpenRouter Rate Limit Exceeded")
            in 500..599 -> throw IllegalStateException("OpenRouter Server Error ($code)")
            else -> throw e
        }
    }
}
