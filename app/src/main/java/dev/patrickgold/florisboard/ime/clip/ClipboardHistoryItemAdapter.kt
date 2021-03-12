package dev.patrickgold.florisboard.ime.clip

import android.content.ClipData
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginEnd
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeValue
import timber.log.Timber

class ClipboardHistoryItemAdapter(
        private val dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>,
        private val pins: ArrayDeque<ClipData>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClipboardHistoryTextViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboard_history_item_text)
    }

    class ClipboardHistoryImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgView: ImageView = view.findViewById(R.id.clipboard_history_item_img)
    }

    companion object {
        const val IMAGE: Int = 1
        const val TEXT: Int = 2
        // TODO: add HTML

        private const val MAX_SIZE: Int = 256
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < pins.size) {
            // is a pin
            when {
                pins[position].getItemAt(0).uri != null -> IMAGE
                pins[position].getItemAt(0).text != null -> TEXT
                pins[position].getItemAt(0).htmlText != null -> TEXT
                else -> null
            }
        }else {
            // regular history item
            when {
                dataSet[position - pins.size].data.getItemAt(0).uri != null -> IMAGE
                dataSet[position - pins.size].data.getItemAt(0).text != null -> TEXT
                dataSet[position - pins.size].data.getItemAt(0).htmlText != null -> TEXT
                else -> null
            }
        }!!
    }




    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val vh = when (viewType) {
            IMAGE -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.clipboard_history_item_image, viewGroup, false)

                ClipboardHistoryImageViewHolder(view)
            }
            TEXT -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.clipboard_history_item_text, viewGroup, false)

                ClipboardHistoryTextViewHolder(view)
            }
            else -> null
        }!!
        Timber.d("AQWXS ${viewGroup.rootView}")
        val clipboardInputManager = ClipboardInputManager.getInstance()
        (vh.itemView as ClipboardHistoryItemView).keyboardView = clipboardInputManager.getClipboardHistoryView()
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder) {
            is ClipboardHistoryTextViewHolder -> {
                var text = if (position < pins.size) {
                    (viewHolder.itemView as ClipboardHistoryItemView).setPinned()
                    pins[position].getItemAt(0).text
                }else {
                    (viewHolder.itemView as ClipboardHistoryItemView).setUnpinned()
                    dataSet[position - pins.size].data.getItemAt(0).text
                }
                if (text.length > MAX_SIZE) {
                    text = text.subSequence(0 until MAX_SIZE).toString() + "..."
                }
                viewHolder.textView.text = text
            }

            is ClipboardHistoryImageViewHolder -> {
                val uri = if (position < pins.size) {
                    (viewHolder.itemView as ClipboardHistoryItemView).setPinned()
                    pins[position].getItemAt(0).uri
                }else {
                    (viewHolder.itemView as ClipboardHistoryItemView).setUnpinned()
                    dataSet[position - pins.size].data.getItemAt(0).uri
                }


                viewHolder.imgView.clipToOutline = true
                val resolver = FlorisBoard.getInstance().context.contentResolver
                val inputStream = resolver.openInputStream(uri)

                val drawable = Drawable.createFromStream(inputStream, "clipboard URI")
                viewHolder.imgView.setImageDrawable(drawable)
            }
        }
    }
    override fun getItemCount() = pins.size + dataSet.size

}
