package com.speekez.api

/**
 * A refinement client that returns the input text without any modifications.
 * Used when refinement is disabled.
 */
class NoOpRefinementClient : RefinementClient {
    override suspend fun refine(text: String, model: String, systemPrompt: String): String {
        return text
    }
}
