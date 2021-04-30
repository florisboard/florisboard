package dev.patrickgold.florisboard.setup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

private typealias OnInputMethodChanged = () -> Unit

class InputMethodChangedReceiver(private val onInputMethodChanged: OnInputMethodChanged) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_INPUT_METHOD_CHANGED) {
            onInputMethodChanged.invoke()
        }
    }
}
