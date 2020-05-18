package dev.patrickgold.florisboard.ime.media.emoji

data class EmojiKeyData(
    var codePoints: List<Int>,
    var label: String = "",
    var popup: MutableList<EmojiKeyData> = mutableListOf()
) {
    fun getCodePointsAsString(): String {
        var ret = ""
        for (codePoint in codePoints) {
            ret += String(Character.toChars(codePoint))
        }
        return ret
    }
}
