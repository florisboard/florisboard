package dev.patrickgold.florisboard.ime.layout

import dev.patrickgold.florisboard.ime.kbd.KeyData

data class LayoutData(
    var name: String,
    var direction: String,
    var arrangement: MutableList<MutableList<KeyData>> = mutableListOf()
)
