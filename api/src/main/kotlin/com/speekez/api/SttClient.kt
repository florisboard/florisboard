package com.speekez.api

import java.io.File

interface SttClient {
    suspend fun transcribe(audioFile: File, model: String, languages: List<String>): String
}
