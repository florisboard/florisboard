package dev.patrickgold.florisboard.ime.clip

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.InputKeyEvent
import dev.patrickgold.florisboard.ime.core.InputView
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import kotlinx.coroutines.*
import kotlin.math.pow

/**
 * Handles the clipboard view and allows for communication between UI and logic.
 */
class ClipboardInputManager private constructor() : CoroutineScope by MainScope(),
    FlorisBoard.EventListener{

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

        launch(Dispatchers.Default) {

            inputView.findViewById<ImageButton>(R.id.back_to_keyboard_button)
                .setOnTouchListener { view, event -> onButtonPressEvent(view, event) }

            inputView.findViewById<ImageButton>(R.id.clear_clipboard_history)
                .setOnTouchListener { view, event -> onButtonPressEvent(view, event) }

            recyclerView = inputView.findViewById(R.id.clipboard_history_items)

            if (BuildConfig.DEBUG && adapter == null) {
                error("initClipboard() not called")
            }

            recyclerView!!.adapter = adapter
            val manager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            recyclerView!!.layoutManager = manager
        }
    }

    /**
     * Clean-up of resources and stopping all coroutines.
     */
    override fun onDestroy() {

        cancel()
        instance = null
    }


    /**
     * Returns a reference to the [ClipboardHistoryView]
     */
    fun getClipboardHistoryView() : ClipboardHistoryView{
        return FlorisBoard.getInstance().inputView?.mainViewFlipper?.getChildAt(2) as ClipboardHistoryView
    }

    /**
     * Returns the adapter position of the view, i.e the position that the item is displayed at (including pins and
     * history items).
     *
     * @param view The ClipboardHistoryItemView whose position is to be determined.
     * @return The adapter position of the view
     */
    fun getPositionOfView(view: View): Int {
        return recyclerView?.getChildLayoutPosition(view)!!
    }

    /**
     * Notify adapter that an item was inserted.
     *
     * @param position The position the item was inserted at
     */
    fun notifyItemInserted(position: Int) = adapter?.notifyItemInserted(position)

    /**
     * Notify adapter that an item was removed
     * @param position The position the item was removed from
     */
    fun notifyItemRemoved(position: Int) = adapter?.notifyItemRemoved(position)

    /**
     * Notify adapter that an item range was removed.
     * @param start The index the range starts at (inclusive)
     * @param numberOfItems The number of items removed
     */
    fun notifyItemRangeRemoved(start: Int, numberOfItems: Int) = adapter?.notifyItemRangeRemoved(start, numberOfItems)

    /**
     * Notify adapter that an item was moved
     * @param from The original position
     * @param to The final position
     */
    fun notifyItemMoved(from: Int, to: Int) = adapter?.notifyItemMoved(from, to)

    /**
     * Notify adapter that an item was changed.
     *
     * @param i The position of the item
     */
    fun notifyItemChanged(i: Int) = adapter?.notifyItemChanged(i)

    /**
     * Handles clicks on the back to keyboard button.
     */
    private fun onButtonPressEvent(view: View, event: MotionEvent?): Boolean {

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
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.down(data))
            }
            MotionEvent.ACTION_UP -> {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.up(data))
            }
            MotionEvent.ACTION_CANCEL -> {
                florisboard.textInputManager.inputEventDispatcher.send(InputKeyEvent.cancel(data))
            }
        }

        // MUST return false here so the background selector for showing a transparent bg works
        return false
    }

    /**
     * [recyclerView] will be linked to [dataSet] and [pins] when initialized.
     *
     * @param dataSet the data set to link to
     * @param pins The pins to link to
     */
    fun initClipboard(dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>, pins: ArrayDeque<ClipboardItem>) {
        this.adapter =  ClipboardHistoryItemAdapter(dataSet = dataSet, pins= pins)
    }

    /**
     * Plays an animation of all items moving off the the clipboard from the top.
     *
     * @param start The index to start at (to ignore pins)
     * @param size The size of the clipboard
     * @return The time in millis till the last animation will complete.
     */
    fun clearClipboardWithAnimation(start: Int, size: Int): Long {
        // list of views to animate
        val views = arrayListOf<View>()
        for(i in 0 until size){
            recyclerView?.findViewHolderForLayoutPosition(i + start)?.let {
                views.add(it.itemView)
            }
        }

        // animate the views
        var delay = 1L
        for (view in views) {
            delay += (10 * delay.toDouble().pow(0.1)).toLong()
            val an = view.animate().translationX(1500f)
            an.startDelay = delay
            an.duration = 250
        }

        // a little while later we reset the views so they can be reused.
        Handler(Looper.getMainLooper()).postDelayed({
            for (view in views) {
                view.translationX = 0f
            }
        }, 450 + delay)

        return 280 + delay
    }


}
