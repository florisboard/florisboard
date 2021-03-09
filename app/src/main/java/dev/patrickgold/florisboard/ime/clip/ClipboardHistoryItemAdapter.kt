package dev.patrickgold.florisboard.ime.clip

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import timber.log.Timber

class ClipboardHistoryItemAdapter(private val dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClipboardHistoryTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboard_history_item_text)
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
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.clipboard_history_item_text, viewGroup, false)

        return ClipboardHistoryTextViewHolder(view)
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
        }
    }

    override fun getItemCount() = dataSet.size

}
