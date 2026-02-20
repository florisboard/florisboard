package com.speekez.api.model

import com.google.gson.annotations.SerializedName

data class OpenRouterRefinementRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<OpenRouterRefinementMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    @SerializedName("temperature") val temperature: Double = 0.3
)

data class OpenRouterRefinementMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class OpenRouterRefinementResponse(
    @SerializedName("choices") val choices: List<OpenRouterRefinementChoice>
)

data class OpenRouterRefinementChoice(
    @SerializedName("message") val message: OpenRouterRefinementMessage
)
