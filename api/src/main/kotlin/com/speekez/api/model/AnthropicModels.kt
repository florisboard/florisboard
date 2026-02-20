package com.speekez.api.model

import com.google.gson.annotations.SerializedName

data class AnthropicRequest(
    @SerializedName("model") val model: String,
    @SerializedName("system") val system: String,
    @SerializedName("messages") val messages: List<AnthropicMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 2048
)

data class AnthropicMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class AnthropicResponse(
    @SerializedName("content") val content: List<AnthropicContent>
)

data class AnthropicContent(
    @SerializedName("type") val type: String,
    @SerializedName("text") val text: String
)
