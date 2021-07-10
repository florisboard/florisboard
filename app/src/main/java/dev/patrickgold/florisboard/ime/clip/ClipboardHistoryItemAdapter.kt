package dev.patrickgold.florisboard.ime.clip

import android.animation.ValueAnimator
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ClipboardHistoryItemAdapter(
    private val dataSet: ArrayDeque<FlorisClipboardManager.TimedClipData>,
    private val pins: ArrayDeque<ClipboardItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope by MainScope() {

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
        } else {
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
            ItemType.TEXT.value -> {
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
                } else {
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
                } else {
                    (viewHolder.itemView as ClipboardHistoryItemView).setUnpinned()
                    dataSet[position - pins.size].data.uri
                }


                viewHolder.imgView.clipToOutline = true
                viewHolder.imgView.visibility = GONE

                // The code looks like a mess because we're jumping across threads so much :(
                // read dimensions (IO) -> set dimensions (UI) -> read bitmap (IO) -> set bitmap (UI)
                launch(Dispatchers.IO) {
                    val resolver = FlorisBoard.getInstance().contentResolver
                    val (imgWidth, imgHeight) = getImageDimensions(resolver, uri!!)
                    viewHolder.itemView.post {
                        val width = viewHolder.itemView.width
                        val sampleSize = calcSampleSize(imgWidth, width)
                        val params = viewHolder.itemView.layoutParams.apply {
                            height = (width * (imgHeight.toFloat() / imgWidth)).roundToInt() + 30
                        }
                        viewHolder.itemView.layoutParams = params
                        this@ClipboardHistoryItemAdapter.launch(Dispatchers.IO) {
                            val bitmap = loadSampledBitmap(resolver, uri, sampleSize)
                            bitmap?.let {
                                viewHolder.itemView.post {
                                    viewHolder.imgView.visibility = VISIBLE
                                    val animator = ValueAnimator.ofFloat(0f, 1f)
                                    animator.duration = 150
                                    animator.addUpdateListener {
                                        viewHolder.imgView.alpha = it.animatedValue as Float
                                    }
                                    animator.start()
                                    viewHolder.imgView.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount() = pins.size + dataSet.size

    // returns (width, height)
    private fun getImageDimensions(resolver: ContentResolver, uri: Uri): Pair<Int, Int> {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            val stream = resolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream, null, this)

            Pair(outWidth, outHeight)
        }
    }

    private fun calcSampleSize(imgWidth: Int, reqWidth: Int): Int {
        var inSampleSize = 2
        while (imgWidth / inSampleSize > reqWidth) {
            inSampleSize *= 2
        }
        return inSampleSize / 2
    }

    private fun loadSampledBitmap(resolver: ContentResolver, uri: Uri, inSampleSize: Int): Bitmap? {
        return BitmapFactory.Options().run {
            // Calculate inSampleSize
            this.inSampleSize = inSampleSize

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false
            val stream2 = resolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream2, null, this)
        }

    }


}
