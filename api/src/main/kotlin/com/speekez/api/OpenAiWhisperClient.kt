package com.speekez.api

import com.speekez.core.NetworkUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

import retrofit2.http.Header

interface OpenAiApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: okhttp3.RequestBody,
        @Part("language") language: okhttp3.RequestBody?
    ): TranscriptionResponse
}

data class TranscriptionResponse(val text: String)

class OpenAiWhisperClient(
    private val api: OpenAiApi,
    private val apiKey: String
) : SttClient {
    override suspend fun transcribe(audioFile: File, model: String, languages: List<String>): String {
        val requestFile = audioFile.asRequestBody("audio/mpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
        val modelBody = model.toRequestBody("text/plain".toMediaTypeOrNull())
        val languageBody = if (languages.isNotEmpty()) {
            languages.first().toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        return NetworkUtils.safeApiCall {
            val response = api.transcribe("Bearer $apiKey", body, modelBody, languageBody)
            response.text
        }.onFailure { e ->
            if (e is HttpException) {
                handleError(e)
            }
        }.getOrThrow()
    }

    private fun handleError(e: HttpException): String {
        val code = e.code()
        when (code) {
            401 -> throw IllegalStateException("Invalid OpenAI API Key")
            402 -> throw IllegalStateException("OpenAI Payment Required / Quota Exceeded")
            429 -> throw IllegalStateException("OpenAI Rate Limit Exceeded")
            in 500..599 -> throw IllegalStateException("OpenAI Server Error ($code)")
            else -> throw e
        }
    }
}
