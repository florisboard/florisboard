package dev.patrickgold.florisboard.ime.clip

import android.annotation.SuppressLint
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.*
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.EditorInstance
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.util.cancelAll
import dev.patrickgold.florisboard.util.postAtScheduledRate
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Handles the clipboard view. Most code stolen directly from [dev.patrickgold.florisboard.ime.media.MediaInputManager].
 */
class ClipboardInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener{

    private val florisboard = FlorisBoard.getInstance()
    private val activeEditorInstance: EditorInstance
        get() = florisboard.activeEditorInstance

    private var repeatedKeyPressHandler: Handler? = null

    companion object {
        private var instance: ClipboardInputManager? = null

        @Synchronized
        fun getInstance(): ClipboardInputManager {
            if (instance == null) {
                instance = ClipboardInputManager()
            }
            return instance!!
        }
    }

    init {
        florisboard.addEventListener(this)
    }

    override fun onCreateInputView() {
        super.onCreateInputView()
        repeatedKeyPressHandler = Handler(florisboard.context.mainLooper)
    }

    /**
     * Called when a new input view has been registered. Used to initialize all media-relevant
     * views and layouts.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onRegisterInputView(inputView: InputView) {
        Timber.i("onRegisterInputView(inputView)")

        launch(Dispatchers.Default) {

            inputView.findViewById<Button>(R.id.back_to_keyboard_button)
                .setOnTouchListener { view, event -> onBackToKeyboardEvent(view, event) }
        }
    }

    /**
     * Clean-up of resources and stopping all coroutines.
     */
    override fun onDestroy() {
        Timber.i("onDestroy()")

        cancel()
        instance = null
    }

    /**
     * Handles clicks on the back to keyboard button.
     */
    private fun onBackToKeyboardEvent(view: View, event: MotionEvent?): Boolean {
        Timber.d("onBottomButtonEvent")

        event ?: return false
        val data = KeyData(code = KeyCode.SWITCH_TO_TEXT_CONTEXT)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                florisboard.keyPressVibrate()
                florisboard.keyPressSound(data)
                if (florisboard.textInputManager.inputEventDispatcher.requireSeparateDownUp(data.code)) {
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.down(data))
                }
            }
            MotionEvent.ACTION_UP -> {
                if (florisboard.textInputManager.inputEventDispatcher.requireSeparateDownUp(data.code)) {
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.up(data))
                } else {
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.downUp(data))
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (florisboard.textInputManager.inputEventDispatcher.requireSeparateDownUp(data.code)) {
                    florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
                }
            }
        }
        // MUST return false here so the background selector for showing a transparent bg works
        return false
    }
}
