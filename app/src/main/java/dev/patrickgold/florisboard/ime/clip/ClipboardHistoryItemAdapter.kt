package dev.patrickgold.florisboard.ime.clip

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

class ClipboardHistoryItemAdapter(private val dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClipboardHistoryTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        return when {
            dataSet[position].data.getItemAt(0).uri != null -> IMAGE
            dataSet[position].data.getItemAt(0).text != null -> TEXT
            dataSet[position].data.getItemAt(0).htmlText != null -> TEXT
            else -> null
        }!!
    }




    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val vh = when (viewType) {
            IMAGE -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.clipboard_history_item_image, viewGroup, false)
                (view as ClipboardHistoryItemView).type = IMAGE

                ClipboardHistoryImageViewHolder(view)
            }
            TEXT -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.clipboard_history_item_text, viewGroup, false)
                (view as ClipboardHistoryItemView).type = TEXT

                ClipboardHistoryTextViewHolder(view)
            }
            else -> null
        }!!
        Timber.d("AQWXS ${viewGroup.rootView}")
        (vh.itemView as ClipboardHistoryItemView).keyboardView =
            ClipboardInputManager.getInstance().recyclerView!!.parent.parent as ClipboardHistoryView
        return vh
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
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
