/*
 * Copyright (C) 2020 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.media.emoticon

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import kotlinx.coroutines.*

/**
 * Manages the layout creation and touch events for the emoticon section of the media context. Parts
 * of the layout of this view will be generated in coroutines and will therefore not instantly be
 * visible.
 */
class EmoticonKeyboardView : LinearLayout {
    private val florisboard: FlorisBoard = FlorisBoard.getInstance()
    private var layout: Deferred<EmoticonLayoutData?>
    private val mainScope = MainScope()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        layout = mainScope.async(Dispatchers.IO) {
            EmoticonLayoutData.fromJsonFile("ime/media/emoticon/emoticons.json")
        }
        orientation = VERTICAL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mainScope.launch {
            layout.await()
            buildLayout()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainScope.cancel()
    }

    /**
     * Builds the layout by dynamically adding rows filled with [EmoticonKeyView]s.
     * This method runs in the [Dispatchers.Default] context and will block the main thread only
     * when it attaches a built row to the view hierarchy.
     */
    private suspend fun buildLayout() = withContext(Dispatchers.Default) {
        val layout = layout.await() ?: return@withContext
        for (row in layout.arrangement) {
            val rowView = LinearLayout(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).apply {
                    weight = 1.0f
                }
                orientation = HORIZONTAL
            }
            for (emoticonKeyData in row) {
                val emoticonKeyView = EmoticonKeyView(context).apply {
                    data = emoticonKeyData
                }
                rowView.addView(emoticonKeyView)
            }
            withContext(Dispatchers.Main) {
                addView(rowView)
            }
        }
    }
}
