package dev.patrickgold.florisboard.ime.core

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.*

class SubtypeTest {
    @Test
    fun fromString_ReturnsCorrectObject_WithLanguageCode() {
        val expected = Subtype(203, Locale("de", "ch"), "swiss_german")
        val actual = Subtype.fromString("203/de_CH/swiss_german")
        assertThat(actual, `is`(expected))
    }

    @Test
    fun fromString_ReturnsCorrectObject_WithLanguageTag() {
        val expected = Subtype(203, Locale("de", "ch"), "swiss_german")
        val actual = Subtype.fromString("203/de-CH/swiss_german")
        assertThat(actual, `is`(expected))
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun fromString_ThrowsException_PropertiesCountLessThan3() {
        Subtype.fromString("203/de-CH")
    }

    @Test(expected = InvalidPropertiesFormatException::class)
    fun fromString_ThrowsException_PropertiesCountMoreThan3() {
        Subtype.fromString("203/de-CH/swiss_german/a_forth_magical_property")
    }

    @Test(expected = NumberFormatException::class)
    fun fromString_ThrowsException_IdPropertyIsNaN() {
        Subtype.fromString("two_hundred_and_three/de-CH/swiss_german")
    }

    @Test
    fun toString_ReturnsCorrectString_FromInstantiatedObject() {
        val expected = "203/de-CH/swiss_german"
        val actual =
            Subtype(203, Locale("de", "ch"), "swiss_german").toString()
        assertThat(actual, `is`(expected))
    }
}
