package dev.patrickgold.florisboard.ime.text.nlp

import dev.patrickgold.florisboard.ime.nlp.NgramNode
import dev.patrickgold.florisboard.ime.nlp.NgramTree
import dev.patrickgold.florisboard.ime.nlp.WeightedToken
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class NgramNodeTest {
    private val ngramTreeToBeTested = NgramTree(higherOrderChildren = mutableMapOf(
        Pair('t', NgramNode(1, 't', NgramNode.FREQ_CHARACTER, sameOrderChildren = mutableMapOf(
            Pair('h', NgramNode(1, 'h', NgramNode.FREQ_CHARACTER, sameOrderChildren = mutableMapOf(
                Pair('e', NgramNode(1, 'e', 255, sameOrderChildren = mutableMapOf(
                    Pair('m', NgramNode(1, 'm', 230)),
                    Pair('r', NgramNode(1, 'r', NgramNode.FREQ_CHARACTER, sameOrderChildren = mutableMapOf(
                        Pair('e', NgramNode(1, 'e', 210))
                    )))
                )))
            ))),
            Pair('o', NgramNode(1, 'o', 220))
        )))
    ))

    @Test
    fun findWord_returnsCorrectNode_forExistingWord() {
        val expected = ngramTreeToBeTested.higherOrderChildren['t']?.sameOrderChildren?.get('h')?.sameOrderChildren?.get('e')
        val actual = ngramTreeToBeTested.findWord("the")
        assertThat(actual, `is`(expected))
    }

    @Test
    fun findWord_returnsNull_forNonExisting() {
        assertThat(
            ngramTreeToBeTested.findWord("ther"),
            `is`(nullValue())
        )
        assertThat(
            ngramTreeToBeTested.findWord("sun"),
            `is`(nullValue())
        )
        assertThat(
            ngramTreeToBeTested.findWord("th"),
            `is`(nullValue())
        )
    }

    @Test
    fun listAllSameOrderWords_returnsCorrectList_forGivenPrefix() {
        val words = mutableListOf<WeightedToken<String, Int>>()
        ngramTreeToBeTested.higherOrderChildren['t']?.listAllSameOrderWords(words, "t")
        assertThat(
            words,
            `is`(listOf(
                WeightedToken("the", 255),
                WeightedToken("them", 230),
                WeightedToken("there", 210),
                WeightedToken("to", 220)
            ))
        )
    }
}

class FlorisLanguageModelTest {
}
