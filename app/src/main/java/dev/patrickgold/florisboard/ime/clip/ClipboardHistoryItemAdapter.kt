package dev.patrickgold.florisboard.ime.clip

import android.content.ClipData
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import timber.log.Timber

class ClipboardHistoryItemAdapter(private val dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClipboardHistoryTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboard_history_item_text)
    }

    class ClipboardHistoryImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgView: ImageView = view.findViewById(R.id.clipboard_history_item_img)
    }

    companion object {
        private val IMAGE: Int = 1
        private val TEXT: Int = 2
        // TODO: add HTML

        private val MAX_SIZE: Int = 256
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            dataSet[position].data.getItemAt(0).uri != null -> IMAGE
            dataSet[position].data.getItemAt(0).text != null -> TEXT
            dataSet[position].data.getItemAt(0).htmlText != null -> TEXT
            else -> null // should be unreachable.
        }!!
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item

        return when (viewType) {
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
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        Timber.d("onBindViewHolder $position")
        when (viewHolder) {
            is ClipboardHistoryTextViewHolder -> {
                var text = dataSet[position].data.getItemAt(0).text
                if (text.length > MAX_SIZE) {
                    text = text.subSequence(0 until MAX_SIZE).toString() + "..."
                }
                viewHolder.textView.text = text
            }

            is ClipboardHistoryImageViewHolder -> {
                viewHolder.imgView.clipToOutline = true
                val resolver = FlorisBoard.getInstance().context.contentResolver
                val inputStream = resolver.openInputStream(dataSet[position].data.getItemAt(0).uri)
                val drawable = Drawable.createFromStream(inputStream, "clipboard URI")
                viewHolder.imgView.setImageDrawable(drawable)
            }
        }
    }
    override fun getItemCount() = dataSet.size

}
