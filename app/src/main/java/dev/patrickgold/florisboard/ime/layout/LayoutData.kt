package dev.patrickgold.florisboard.ime.layout

import dev.patrickgold.florisboard.ime.key.KeyData
import dev.patrickgold.florisboard.ime.key.KeyboardMode

typealias LayoutDataArrangement = List<List<KeyData>>
data class LayoutData(
    val type: LayoutType,
    val name: String,
    val direction: String,
    val arrangement: LayoutDataArrangement = listOf()
) {
    companion object {
        fun empty(): LayoutData {
            return LayoutData(LayoutType.CHARACTERS, "", "")
        }
    }
}

typealias ComputedLayoutDataArrangement = MutableList<MutableList<KeyData>>
data class ComputedLayoutData(
    val mode: KeyboardMode,
    val name: String,
    val direction: String,
    val arrangement: ComputedLayoutDataArrangement = mutableListOf()
) {
    companion object {
        fun empty(): ComputedLayoutData {
            return ComputedLayoutData(KeyboardMode.CHARACTERS, "", "")
        }
    }
}
