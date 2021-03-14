package dev.patrickgold.florisboard.ime.extension

import android.content.Context
import android.net.Uri

class ExternalContentUtils private constructor() {
    companion object {
        fun readTextFromUri(context: Context, uri: Uri): Result<String> {
            val contentResolver = context.contentResolver
                ?: return Result.failure(NullPointerException("System content resolver not available!"))
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(NullPointerException("Cannot open input stream!"))
            val rawText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return Result.success(rawText)
        }

        fun writeTextToUri(context: Context, uri: Uri, text: String): Result<Unit> {
            val contentResolver = context.contentResolver
                ?: return Result.failure(NullPointerException("System content resolver not available!"))
            val outputStream = contentResolver.openOutputStream(uri)
                ?: return Result.failure(NullPointerException("Cannot open output stream!"))
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
            return Result.success(Unit)
        }
    }
}
