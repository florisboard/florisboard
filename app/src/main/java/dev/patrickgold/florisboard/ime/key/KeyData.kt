package dev.patrickgold.florisboard.ime.key

data class KeyData(
    var code: Int,
    var label: String = "",
    var popup: MutableList<KeyData> = mutableListOf(),
    var type: KeyType = KeyType.CHARACTER,
    var variation: KeyVariation = KeyVariation.ALL
)
