package dev.patrickgold.florisboard.ime.text.layout

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class LayoutTypeTest {
    @Test
    fun fromString_ReturnsCorrectLayoutType_ForValidLayoutTypeString() {
        assertThat(LayoutType.fromString("characters"), `is`(LayoutType.CHARACTERS))
        assertThat(LayoutType.fromString("characters_mod"), `is`(LayoutType.CHARACTERS_MOD))
        assertThat(LayoutType.fromString("characters/mod"), `is`(LayoutType.CHARACTERS_MOD))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromString_ThrowsException_ForInvalidLayoutTypeString() {
        LayoutType.fromString("a_strange_layout_type")
    }

    @Test
    fun toString_ReturnsCorrectString_FromLayoutType() {
        // First test a layout type without a underline in it.
        // The returned string must be the layout type in lowercase.
        assertThat(LayoutType.CHARACTERS.toString(), `is`("characters"))

        // Now test a layout type with a underline in it.
        // The returned string must be the layout type in lowercase, also the underline
        //  must be a forward slash now.
        assertThat(LayoutType.CHARACTERS_MOD.toString(), `is`("characters/mod"))
    }
}
