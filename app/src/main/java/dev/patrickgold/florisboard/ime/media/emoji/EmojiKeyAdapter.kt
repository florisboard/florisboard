package dev.patrickgold.florisboard.ime.media.emoji

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager


class EmojiKeyAdapter(
    private val dataSet: List<EmojiKey>,
    private val emojiKeyboardView: EmojiKeyboardView,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val PLACEHOLDER_EMOJI_COUNT = 24
    }

    class EmojiKeyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EmojiKeyViewHolder(EmojiKeyView(emojiKeyboardView, EmojiKey(EmojiKeyData.EMPTY)))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < dataSet.size) {
            (holder.itemView as EmojiKeyView).key = dataSet[position]
            holder.itemView.layoutParams = FlexboxLayoutManager.LayoutParams(
                emojiKeyboardView.emojiKeyWidth, emojiKeyboardView.emojiKeyHeight
            )
        } else {
            (holder.itemView as EmojiKeyView).key = EmojiKey.EMPTY
            holder.itemView.layoutParams = FlexboxLayoutManager.LayoutParams(
                emojiKeyboardView.emojiKeyWidth, 0
            )
        }
    }

    override fun getItemCount(): Int {
        // Add empty placeholder emojis at the end so the grid view. Below is an illustration how
        // the UI looks with and without an placeholder (e = emoji):
        //   Without placeholder        With placeholder
        //     e e e e e e e             e e e e e e e
        //     .............             .............
        //     e e e e e e e             e e e e e e e
        //        e e e e                e e e e
        //
        // Based on this SO's answer idea (by La Nube - Luis R. Díaz Muñiz):
        //  https://stackoverflow.com/a/31478004/6801193
        //
        // 24 items are chosen here because that's probably the max items that will be shown per
        // row, even in landscape mode.
        return dataSet.size + PLACEHOLDER_EMOJI_COUNT
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }
}

