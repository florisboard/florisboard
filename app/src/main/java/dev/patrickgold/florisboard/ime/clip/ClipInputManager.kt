package dev.patrickgold.florisboard.ime.clip

import android.annotation.SuppressLint
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Handles the clipboard view. Most code stolen directly from [dev.patrickgold.florisboard.ime.media.MediaInputManager].
 */
class ClipboardInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener{

    private var dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>? = null
    private val florisboard = FlorisBoard.getInstance()

    private var repeatedKeyPressHandler: Handler? = null

    private var recyclerView: RecyclerView? = null

    private var adapter: ClipboardHistoryItemAdapter? = null

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
                .setOnTouchListener { view, event -> onButtonPressEvent(view, event) }

            inputView.findViewById<Button>(R.id.clear_clipboard_history)
                .setOnTouchListener { view, event -> onButtonPressEvent(view, event) }

            recyclerView = inputView.findViewById(R.id.clipboard_history_items)
            recyclerView!!.adapter = adapter
            val manager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            recyclerView!!.layoutManager = manager
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
    private fun onButtonPressEvent(view: View, event: MotionEvent?): Boolean {
        Timber.d("onBottomButtonEvent")

        event ?: return false
        val data = when (view.id) {
            R.id.back_to_keyboard_button -> KeyData(code = KeyCode.SWITCH_TO_TEXT_CONTEXT)
            R.id.clear_clipboard_history -> KeyData(code = KeyCode.CLEAR_CLIPBOARD_HISTORY)
            else -> null
        }!!
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

    fun initClipboard(dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>) {
        this.adapter =  ClipboardHistoryItemAdapter(dataSet = dataSet)
        this.dataSet = dataSet
    }

    fun notifyClipboardDataChanged() {
        this.recyclerView?.adapter?.notifyDataSetChanged()
        Timber.d("NOTIFY CHANGED ${dataSet?.size}")
    }
}
