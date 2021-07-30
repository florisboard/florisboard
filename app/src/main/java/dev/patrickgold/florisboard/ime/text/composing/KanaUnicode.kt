package dev.patrickgold.florisboard.ime.text.composing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("kana-unicode")
class KanaUnicode : Composer {
    override val name: String = "kana-unicode"
    override val label: String = "Kana Unicode"
    override val toRead: Int = 1

    private val daku = mapOf(
        'う' to "ゔ",

        'か' to "が",
        'き' to "ぎ",
        'く' to "ぐ",
        'け' to "げ",
        'こ' to "ご",

        'さ' to "ざ",
        'し' to "じ",
        'す' to "ず",
        'せ' to "ぜ",
        'そ' to "ぞ",

        'た' to "だ",
        'ち' to "ぢ",
        'つ' to "づ",
        'て' to "で",
        'と' to "ど",

        'は' to "ば",
        'ひ' to "び",
        'ふ' to "ぶ",
        'へ' to "べ",
        'ほ' to "ぼ",

        'ウ' to "ヴ",

        'カ' to "ガ",
        'キ' to "ギ",
        'ク' to "グ",
        'ケ' to "ゲ",
        'コ' to "ゴ",

        'サ' to "ザ",
        'シ' to "ジ",
        'ス' to "ズ",
        'セ' to "ゼ",
        'ソ' to "ゾ",

        'タ' to "ダ",
        'チ' to "ヂ",
        'ツ' to "ヅ",
        'テ' to "デ",
        'ト' to "ド",

        'ハ' to "バ",
        'ヒ' to "ビ",
        'フ' to "ブ",
        'ヘ' to "ベ",
        'ホ' to "ボ",

        'ワ' to "ヷ",
        'ヰ' to "ヸ",
        'ヱ' to "ヹ",
        'ヲ' to "ヺ",
    )

    private val handaku = mapOf(
        'は' to "ぱ",
        'ひ' to "ぴ",
        'ふ' to "ぷ",
        'へ' to "ぺ",
        'ほ' to "ぽ",

        'ハ' to "パ",
        'ヒ' to "ピ",
        'フ' to "プ",
        'ヘ' to "ペ",
        'ホ' to "ポ",
    )

    private val small = mapOf(
        'あ' to "ぁ",
        'い' to "ぃ",
        'え' to "ぅ",
        'う' to "ぇ",
        'お' to "ぉ",

        'か' to "ゕ",
        'け' to "ゖ",

        'つ' to "っ",
                   
        'や' to "ゃ",
        'ゆ' to "ゅ",
        'よ' to "ょ",

        'わ' to "ゎ",
        'ゐ' to "𛅐",
        'ゑ' to "𛅑",
        'を' to "𛅒",

        'ア' to "ァ",
        'イ' to "ィ",
        'エ' to "ゥ",
        'ウ' to "ェ",
        'オ' to "ォ",

        'カ' to "ヵ",
        'ク' to "ㇰ",
        'ケ' to "ヶ",

        'シ' to "ㇱ",
        'ス' to "ㇲ",

        'ツ' to "ッ",
        'ト' to "ㇳ",

        'ヌ' to "ㇴ",

        'ハ' to "ㇵ",
        'ヒ' to "ㇶ",
        'フ' to "ㇷ",
        'ヘ' to "ㇸ",
        'ホ' to "ㇹ",

        'ム' to "ㇺ",

        'ヤ' to "ヤ",
        'ユ' to "ュ",
        'ヨ' to "ョ",

        'ラ' to "ㇻ",
        'リ' to "ㇼ",
        'ル' to "ㇽ",
        'レ' to "ㇾ",
        'ロ' to "ㇿ",

        'ワ' to "ヮ",
        'ヰ' to "𛅤",
        'ヱ' to "𛅥",
        'ヲ' to "𛅦",

        'ン' to "𛅧",

        'ぁ' to "あ",
        'ぃ' to "い",
        'ぅ' to "え",
        'ぇ' to "う",
        'ぉ' to "お",

        'ゕ' to "か",
        'ゖ' to "け",

        'っ' to "つ",
                   
        'ゃ' to "や",
        'ゅ' to "ゆ",
        'ょ' to "よ",

        'ゎ' to "わ",

        'ァ' to "ア",
        'ィ' to "イ",
        'ゥ' to "エ",
        'ェ' to "ウ",
        'ォ' to "オ",

        'ヵ' to "カ",
        'ㇰ' to "ク",
        'ヶ' to "ケ",

        'ㇱ' to "シ",
        'ㇲ' to "ス",

        'ッ' to "ツ",
        'ㇳ' to "ト",

        'ㇴ' to "ヌ",

        'ㇵ' to "ハ",
        'ㇶ' to "ヒ",
        'ㇷ' to "フ",
        'ㇸ' to "ヘ",
        'ㇹ' to "ホ",

        'ㇺ' to "ム",

        'ヤ' to "ヤ",
        'ュ' to "ユ",
        'ョ' to "ヨ",

        'ㇻ' to "ラ",
        'ㇼ' to "リ",
        'ㇽ' to "ル",
        'ㇾ' to "レ",
        'ㇿ' to "ロ",

        'ヮ' to "ワ",
    )

    private fun isDakuten(char: Char): Boolean {
        return char == '゙' || char == '゛' || char == 'ﾞ'
    }

    private fun isHandakuten(char: Char): Boolean {
        return char == '゚' || char == '゜' || char == 'ﾟ'
    }

    override fun getActions(s: String, c: Char): Pair<Int, String> {
        // s is "at least the last 1 character of what's currently here"
        if (s.isEmpty()) {
            return Pair(0, ""+c)
        }
        val lastChar = s.last()
        val lastOrd = lastChar.toInt()
        val isSmall = false

        if (isDakuten(c)) {
            val dakuChar = daku.get(lastChar)
            if (dakuChar == null) {
                return Pair(0, ""+c)
            } else {
                return Pair(1, ""+dakuChar)
            }
        } else if (isHandakuten(c)) {
            val handakuChar = handaku.get(lastChar)
            if (handakuChar == null) {
                return Pair(0, ""+c)
            } else {
                return Pair(1, ""+handakuChar)
            }
        } else if (isSmall) {
            val smallChar = small.get(lastChar)
            if (smallChar == null) {
                return Pair(0, ""+c)
            } else {
                return Pair(1, ""+smallChar)
            }
        }

        return Pair(0, ""+c)
    }
}
