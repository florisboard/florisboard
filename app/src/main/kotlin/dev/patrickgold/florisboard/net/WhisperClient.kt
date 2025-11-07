package dev.silo.omniboard.net

import dev.silo.omniboard.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

object WhisperClient {
    private val client = OkHttpClient()

    suspend fun transcribe(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/mp4".toMediaType())
                )
                .addFormDataPart("model", BuildConfig.WHISPER_MODEL)
                .build()

            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val text = responseBody?.let { JSONObject(it).getString("text") } ?: ""
                    Result.success(text)
                } else {
                    val errorBody = response.body?.string()?.take(200) ?: ""
                    Result.failure(Exception("API Error: ${response.code} $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
