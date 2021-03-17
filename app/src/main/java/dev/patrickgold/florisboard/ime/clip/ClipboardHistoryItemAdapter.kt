package dev.patrickgold.florisboard.ime.clip

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clip.provider.ItemType
import dev.patrickgold.florisboard.ime.core.FlorisBoard

class ClipboardHistoryItemAdapter(
        private val dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>,
        private val pins: ArrayDeque<ClipboardItem>
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClipboardHistoryTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.clipboard_history_item_text)
    }

    class ClipboardHistoryImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgView: ImageView = view.findViewById(R.id.clipboard_history_item_img)
    }

    companion object {
        private const val MAX_SIZE: Int = 256
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < pins.size) {
            // is a pin
            pins[position].type.value
        }else {
            // regular history item
            dataSet[position - pins.size].data.type.value
        }
    }




    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val vh = when (viewType) {
            ItemType.IMAGE.value -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.clipboard_history_item_image, viewGroup, false)

                ClipboardHistoryImageViewHolder(view)
            }
            ItemType.TEXT.value  -> {
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.clipboard_history_item_text, viewGroup, false)

                ClipboardHistoryTextViewHolder(view)
            }
            else -> null
        }!!
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
                    pins[position].text
                }else {
                    (viewHolder.itemView as ClipboardHistoryItemView).setUnpinned()
                    dataSet[position - pins.size].data.text
                }
                if (text!!.length > MAX_SIZE) {
                    text = text.subSequence(0 until MAX_SIZE).toString() + "..."
                }
                viewHolder.textView.text = text
            }

            is ClipboardHistoryImageViewHolder -> {
                val uri = if (position < pins.size) {
                    (viewHolder.itemView as ClipboardHistoryItemView).setPinned()
                    pins[position].uri
                }else {
                    (viewHolder.itemView as ClipboardHistoryItemView).setUnpinned()
                    dataSet[position - pins.size].data.uri
                }


                viewHolder.imgView.clipToOutline = true
                viewHolder.imgView.visibility = GONE
                // For very large images, this can take a bit
                FlorisClipboardManager.getInstance().executor.execute {
                    val resolver = FlorisBoard.getInstance().context.contentResolver
                    val inputStream = resolver.openInputStream(uri!!)

                    val drawable = Drawable.createFromStream(inputStream, "clipboard URI")
                    viewHolder.itemView.post {
                        viewHolder.imgView.setImageDrawable(drawable)
                        viewHolder.imgView.visibility = VISIBLE
                    }
                }
            }
        }
    }
    override fun getItemCount() = pins.size + dataSet.size

}
