package com.speekez.api

import android.content.Context
import com.speekez.core.ApiMode
import com.speekez.core.ModelTier
import com.speekez.security.EncryptedPreferencesManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiRouterManager(
    private val context: Context,
    private val prefs: EncryptedPreferencesManager
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val openRouterRetrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val openAiRetrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getSttClient(): SttClient? {
        val mode = prefs.getApiMode()
        return when (mode) {
            ApiMode.OPENROUTER -> {
                val key = prefs.getOpenRouterKey() ?: return null
                OpenRouterAudioClient(openRouterRetrofit.create(OpenRouterApi::class.java), key)
            }
            ApiMode.SEPARATE -> {
                val key = prefs.getOpenAiKey() ?: return null
                OpenAiWhisperClient(openAiRetrofit.create(OpenAiApi::class.java), key)
            }
            ApiMode.NO_KEYS -> null
        }
    }

    fun getRefinementClient(): RefinementClient? {
        // Implementation for refinement clients will be added later
        // Returning null for now as per current requirements focus
        return null
    }

    fun getApiMode(): ApiMode = prefs.getApiMode()

    fun getSttModel(tier: ModelTier): String {
        val mode = prefs.getApiMode()
        return when (mode) {
            ApiMode.OPENROUTER -> when (tier) {
                ModelTier.CHEAP -> "openai/whisper-large-v3-turbo"
                ModelTier.BEST -> "openai/whisper-large-v3"
                ModelTier.CUSTOM -> prefs.getCustomSttModel() ?: "openai/whisper-large-v3-turbo"
            }
            ApiMode.SEPARATE -> "whisper-1" // OpenAI standard
            ApiMode.NO_KEYS -> ""
        }
    }

    fun getRefinementModel(tier: ModelTier): String {
        return when (tier) {
            ModelTier.CHEAP -> "anthropic/claude-3-haiku"
            ModelTier.BEST -> "anthropic/claude-3-sonnet"
            ModelTier.CUSTOM -> prefs.getCustomRefinementModel() ?: "anthropic/claude-3-haiku"
        }
    }
}
