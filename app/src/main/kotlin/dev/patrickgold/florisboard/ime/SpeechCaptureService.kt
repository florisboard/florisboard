package dev.patrickgold.florisboard.ime

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import dev.patrickgold.florisboard.R

class SpeechCaptureService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            1,
            NotificationCompat.Builder(this, "speech_capture")
                .setContentTitle("Recording audio")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?) = null
}
