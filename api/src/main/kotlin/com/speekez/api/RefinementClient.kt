package com.speekez.api

interface RefinementClient {
    suspend fun refine(text: String, model: String, systemPrompt: String): String
}
