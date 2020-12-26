package dev.patrickgold.florisboard.util

import android.os.Handler

fun Handler.postAtScheduledRate(delayMillis: Long, periodMillis: Long, r: Runnable) {
    val internalRunnable = object : Runnable {
        override fun run() {
            this@postAtScheduledRate.postDelayed(this, periodMillis)
            r.run()
        }
    }
    this.postDelayed(internalRunnable, delayMillis)
}

fun Handler.postDelayed(delayMillis: Long, r: Runnable) {
    this.postDelayed(r, delayMillis)
}

fun Handler.cancelAll() {
    this.removeCallbacksAndMessages(null)
}
