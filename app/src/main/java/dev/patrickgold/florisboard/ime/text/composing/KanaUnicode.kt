package dev.patrickgold.florisboard.ime.text.composing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("kana-unicode")
class KanaUnicode : Composer {
    override val id: String = "kana-unicode"
    override val label: String = "Kana Unicode"
    override val toRead: Int = 1

    val sticky: Boolean = false

    private val daku = mapOf(
        'う' to 'ゔ',

        'か' to 'が',
        'き' to 'ぎ',
        'く' to 'ぐ',
        'け' to 'げ',
        'こ' to 'ご',

        'さ' to 'ざ',
        'し' to 'じ',
        'す' to 'ず',
        'せ' to 'ぜ',
        'そ' to 'ぞ',

        'た' to 'だ',
        'ち' to 'ぢ',
        'つ' to 'づ',
        'て' to 'で',
        'と' to 'ど',

        'は' to 'ば',
        'ひ' to 'び',
        'ふ' to 'ぶ',
        'へ' to 'べ',
        'ほ' to 'ぼ',

        'ウ' to 'ヴ',

        'カ' to 'ガ',
        'キ' to 'ギ',
        'ク' to 'グ',
        'ケ' to 'ゲ',
        'コ' to 'ゴ',

        'サ' to 'ザ',
        'シ' to 'ジ',
        'ス' to 'ズ',
        'セ' to 'ゼ',
        'ソ' to 'ゾ',

        'タ' to 'ダ',
        'チ' to 'ヂ',
        'ツ' to 'ヅ',
        'テ' to 'デ',
        'ト' to 'ド',

        'ハ' to 'バ',
        'ヒ' to 'ビ',
        'フ' to 'ブ',
        'ヘ' to 'ベ',
        'ホ' to 'ボ',

        'ワ' to 'ヷ',
        'ヰ' to 'ヸ',
        'ヱ' to 'ヹ',
        'ヲ' to 'ヺ',

        'ゝ' to 'ゞ',
        'ヽ' to 'ヾ',
    )

    private val handaku = mapOf(
        'は' to 'ぱ',
        'ひ' to 'ぴ',
        'ふ' to 'ぷ',
        'へ' to 'ぺ',
        'ほ' to 'ぽ',

        'ハ' to 'パ',
        'ヒ' to 'ピ',
        'フ' to 'プ',
        'ヘ' to 'ペ',
        'ホ' to 'ポ',
    )

    private val small = mapOf(
        'あ' to "ぁ",
        'い' to "ぃ",
        'え' to "ぇ",
        'う' to "ぅ",
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
        'エ' to "ェ",
        'ウ' to "ゥ",
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

        'ヤ' to "ャ",
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
    )

    private val reverseDaku = mapOf(
        'ゔ' to 'う',

        'が' to 'か',
        'ぎ' to 'き',
        'ぐ' to 'く',
        'げ' to 'け',
        'ご' to 'こ',

        'ざ' to 'さ',
        'じ' to 'し',
        'ず' to 'す',
        'ぜ' to 'せ',
        'ぞ' to 'そ',

        'だ' to 'た',
        'ぢ' to 'ち',
        'づ' to 'つ',
        'で' to 'て',
        'ど' to 'と',

        'ば' to 'は',
        'び' to 'ひ',
        'ぶ' to 'ふ',
        'べ' to 'へ',
        'ぼ' to 'ほ',

        'ヴ' to 'ウ',

        'ガ' to 'カ',
        'ギ' to 'キ',
        'グ' to 'ク',
        'ゲ' to 'ケ',
        'ゴ' to 'コ',

        'ザ' to 'サ',
        'ジ' to 'シ',
        'ズ' to 'ス',
        'ゼ' to 'セ',
        'ゾ' to 'ソ',

        'ダ' to 'タ',
        'ヂ' to 'チ',
        'ヅ' to 'ツ',
        'デ' to 'テ',
        'ド' to 'ト',

        'バ' to 'ハ',
        'ビ' to 'ヒ',
        'ブ' to 'フ',
        'ベ' to 'ヘ',
        'ボ' to 'ホ',

        'ヷ' to 'ワ',
        'ヸ' to 'ヰ',
        'ヹ' to 'ヱ',
        'ヺ' to 'ヲ',

        'ゞ' to 'ゝ',
        'ヾ' to 'ヽ',
    )

    private val reverseHandaku = mapOf(
        'ぱ' to 'は',
        'ぴ' to 'ひ',
        'ぷ' to 'ふ',
        'ぺ' to 'へ',
        'ぽ' to 'ほ',

        'パ' to 'ハ',
        'ピ' to 'ヒ',
        'プ' to 'フ',
        'ペ' to 'ヘ',
        'ポ' to 'ホ',
    )

    private val reverseSmall = mapOf(
        'ぁ' to 'あ',
        'ぃ' to 'い',
        'ぅ' to 'う',
        'ぇ' to 'え',
        'ぉ' to 'お',

        'ゕ' to 'か',
        'ゖ' to 'け',

        'っ' to 'つ',

        'ゃ' to 'や',
        'ゅ' to 'ゆ',
        'ょ' to 'よ',

        'ゎ' to 'わ',


        'ァ' to 'ア',
        'ィ' to 'イ',
        'ゥ' to 'ウ',
        'ェ' to 'エ',
        'ォ' to 'オ',

        'ヵ' to 'カ',
        'ㇰ' to 'ク',
        'ヶ' to 'ケ',

        'ㇱ' to 'シ',
        'ㇲ' to 'ス',

        'ッ' to 'ツ',
        'ㇳ' to 'ト',

        'ㇴ' to 'ヌ',

        'ㇵ' to 'ハ',
        'ㇶ' to 'ヒ',
        'ㇷ' to 'フ',
        'ㇸ' to 'ヘ',
        'ㇹ' to 'ホ',

        'ㇺ' to 'ム',

        'ャ' to 'ヤ',
        'ュ' to 'ユ',
        'ョ' to 'ヨ',

        'ㇻ' to 'ラ',
        'ㇼ' to 'リ',
        'ㇽ' to 'ル',
        'ㇾ' to 'レ',
        'ㇿ' to 'ロ',

        'ヮ' to 'ワ',
    )

    private val smallSentinel: Char = '〓'

    private fun isDakuten(char: Char): Boolean {
        return char == '゙' || char == '゛' || char == 'ﾞ'
    }

    private fun isHandakuten(char: Char): Boolean {
        return char == '゚' || char == '゜' || char == 'ﾟ'
    }

    private fun isComposingCharacter(char: Char): Boolean {
        return char == '゙' || char == '゚'
    }

    private fun getBaseCharacter(c: Char): Char {
        return reverseDaku.getOrElse(c) {
            reverseHandaku.getOrElse(c) {
                reverseSmall.getOrElse(c) { c }
            }
        }
    }

    private fun <K>handleTransform(l: Char, c: Char, base: Map<Char, K>, rev: Map<Char, Char>, addOnFalse: Boolean): Pair<Int, String> {
        val char = getBaseCharacter(l)
        val trans = if (sticky) {
            base[char]
        } else {
            rev.getOrElse(l) { base[char] }
        }
        return if (trans != null) {
            Pair(1, ""+trans)
        } else if (isComposingCharacter(l) && isComposingCharacter(c)) {
            Pair(1, if (l == c && !sticky) { "" } else { ""+c  })
        } else {
            Pair(0, if (addOnFalse) { ""+c } else { "" })
        }
    }

    override fun getActions(s: String, c: Char): Pair<Int, String> {
        // s is "at least the last 1 character of what's currently here"
        if (s.isEmpty()) {
            return if (c == smallSentinel || isComposingCharacter(c)) {
                Pair(0, "")
            } else {
                Pair(0, ""+c)
            }
        }
        val lastChar = s.last()

        return when {
            isDakuten(c) -> handleTransform(lastChar, c, daku, reverseDaku, true)
            isHandakuten(c) -> handleTransform(lastChar, c, handaku, reverseHandaku, true)
            c == smallSentinel -> handleTransform(lastChar, c, small, reverseSmall, false)
            else -> Pair(0, ""+c)
        }
    }
}
