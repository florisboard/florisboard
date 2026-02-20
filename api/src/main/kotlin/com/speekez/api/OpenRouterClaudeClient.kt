package com.speekez.api

import com.speekez.api.model.OpenRouterRefinementMessage
import com.speekez.api.model.OpenRouterRefinementRequest
import com.speekez.api.model.OpenRouterRefinementResponse
import com.speekez.core.NetworkUtils
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenRouterRefinementApi {
    @POST("api/v1/chat/completions")
    suspend fun refine(
        @Header("Authorization") auth: String,
        @Body request: OpenRouterRefinementRequest
    ): OpenRouterRefinementResponse
}

class OpenRouterClaudeClient(
    private val apiKey: String,
    private val api: OpenRouterRefinementApi = createDefaultApi()
) : RefinementClient {

    companion object {
        private fun createDefaultApi(): OpenRouterRefinementApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl("https://openrouter.ai/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenRouterRefinementApi::class.java)
        }
    }

    override suspend fun refine(text: String, model: String, systemPrompt: String): String {
        val request = OpenRouterRefinementRequest(
            model = model,
            messages = listOf(
                OpenRouterRefinementMessage(role = "system", content = systemPrompt),
                OpenRouterRefinementMessage(role = "user", content = text)
            )
        )

        return NetworkUtils.safeApiCall {
            val response = api.refine("Bearer $apiKey", request)
            response.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response from OpenRouter")
        }.getOrThrow()
    }
}
