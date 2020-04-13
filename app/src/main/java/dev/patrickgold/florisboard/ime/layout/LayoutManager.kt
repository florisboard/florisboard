package dev.patrickgold.florisboard.ime.layout

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.kbd.KeyData
import dev.patrickgold.florisboard.ime.kbd.KeyboardMode

class LayoutManager {

    private val context: Context

    var characterLayout: LayoutData = LayoutData("notinit", "ltr")
        private set
    var characterCtrlLayout: LayoutData = LayoutData("notinit", "ltr")
        private set
    var numericLayout: LayoutData = LayoutData("notinit", "ltr")
        private set
    var symbolLayout: LayoutData = LayoutData("notinit", "ltr")
        private set
    var symbolExtLayout: LayoutData = LayoutData("notinit", "ltr")
        private set

    constructor(context: Context) {
        this.context = context
    }

    private fun parseLayout(name: String): LayoutData {
        // TODO: choose correct kbd_layout for given string (currently only qwerty)
        val jsonRaw = context.resources.openRawResource(R.raw.kbd_qwerty)
            .bufferedReader().use { it.readText() }
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val layoutAdapter = moshi.adapter(LayoutData::class.java)
        val layout = layoutAdapter.fromJson(jsonRaw) ?: throw Error("Provided JSON layout '$name' is invalid and cannot be parsed!")
        return layout
    }

    fun init() {
        characterLayout = parseLayout("qwerty")
    }

    fun getComputedLayout(keyboardMode: KeyboardMode): LayoutData {
        val computedLayout = LayoutData(keyboardMode.toString(), "ltr")
        when (keyboardMode) {
            KeyboardMode.ALPHABET -> {
                // explicit copy list to create new one
                computedLayout.arrangement = characterLayout.arrangement.toMutableList()
                /*val lastCharRow = computedLayout.arrangement.last()
                lastCharRow.add(0, characterCtrlLayout.arrangement.first().firstOrNull() ?: KeyData(0))
                lastCharRow.add(characterCtrlLayout.arrangement.last().lastOrNull() ?: KeyData(0))
                computedLayout.arrangement.add(characterCtrlLayout.arrangement.last())*/
            }
        }
        return computedLayout
    }
}