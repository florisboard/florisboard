package com.speekez.api

import com.speekez.api.model.AnthropicMessage
import com.speekez.api.model.AnthropicRequest
import com.speekez.api.model.AnthropicResponse
import com.speekez.core.NetworkUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AnthropicApi {
    @POST("v1/messages")
    suspend fun refine(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): AnthropicResponse
}

class AnthropicClaudeClient(
    private val apiKey: String,
    private val api: AnthropicApi = createDefaultApi()
) : RefinementClient {

    companion object {
        private fun createDefaultApi(): AnthropicApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://api.anthropic.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AnthropicApi::class.java)
        }
    }

    override suspend fun refine(text: String, model: String, systemPrompt: String): String {
        val request = AnthropicRequest(
            model = model,
            system = systemPrompt,
            messages = listOf(
                AnthropicMessage(role = "user", content = text)
            )
        )

        return NetworkUtils.safeApiCall {
            val response = api.refine(apiKey = apiKey, request = request)
            response.content.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from Anthropic")
        }.getOrThrow()
    }
}
