package dev.silo.omniboard.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class Recorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        val file = File.createTempFile("whisper", ".mp4", context.cacheDir)
        outputFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                // Handle exception
            }
        }
        return file
    }

    fun stop(): File {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: IllegalStateException) {
            // Handle exception
        } finally {
            mediaRecorder = null
        }
        val file = outputFile ?: throw IOException("Output file not set")
        if (file.length() < 1024) {
            throw IOException("File size is less than 1024 bytes")
        }
        return file
    }
}
