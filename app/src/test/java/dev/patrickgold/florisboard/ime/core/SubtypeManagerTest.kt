package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.content.res.AssetManager
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner.Silent::class)
class SubtypeManagerTest {
    // TODO: rewrite this test, this is miles outdated
    /*@Mock private lateinit var context: Context
    @Mock private lateinit var assetManager: AssetManager
    private lateinit var prefs: TestablePrefHelper
    private lateinit var subtypeManagerUnderTest: SubtypeManager

    @Before
    fun initMocks() {
        prefs = TestablePrefHelper(context)

        `when`(context.packageName)
            .thenReturn("dev.patrickgold.florisboard")
        `when`(context.assets)
            .thenReturn(assetManager)
        `when`(assetManager.open(SubtypeManager.IME_CONFIG_FILE_PATH))
            .thenReturn("{package:'dev.patrickgold.florisboard'}".byteInputStream())

        subtypeManagerUnderTest = SubtypeManager(context, prefs.instance)
    }

    @Test
    fun testSubtypesGetter() {
        doReturn("1/de-DE/qwertz;23/fr-CH/swiss_french").`when`(prefs.localization).subtypes
        assertThat(subtypeManagerUnderTest.subtypes, `is`(listOf(
            Subtype(1, Locale("de", "DE"), "qwertz"),
            Subtype(23, Locale("fr", "CH"), "swiss_french")
        )))
    }

    @Test
    fun testSubtypesSetter() {
        subtypeManagerUnderTest.subtypes = listOf(
            Subtype(1, Locale("de", "DE"), "qwertz"),
            Subtype(23, Locale("fr", "CH"), "swiss_french")
        )
        verify(prefs.localization).subtypes = "1/de-DE/qwertz;23/fr-CH/swiss_french"
    }

    @Test
    fun testAddSubtype() {
        doReturn("1/de-DE/qwertz;23/fr-CH/swiss_french").`when`(prefs.localization).subtypes

        // First test adding a non-existing subtype to the list.
        val locale = Locale("pt", "PT")
        val layout = "qwerty"
        val id = locale.hashCode() + layout.hashCode()
        subtypeManagerUnderTest.addSubtype(locale, layout)
        verify(prefs.localization).subtypes = "1/de-DE/qwertz;23/fr-CH/swiss_french;$id/pt-PT/qwerty"

        // Now test to add the same subtype twice. It should work fine for the first call but should fail
        // on the second call.
        val locale2 = Locale("pt", "PT")
        val layout2 = "qwerty"
        val id2 = locale.hashCode() + layout.hashCode()
        doReturn("").`when`(prefs.localization).subtypes
        assertThat(subtypeManagerUnderTest.addSubtype(locale2, layout2), `is`(true))
        doReturn("$id2/pt-PT/qwerty").`when`(prefs.localization).subtypes
        assertThat(subtypeManagerUnderTest.addSubtype(locale2, layout2), `is`(false))
    }

    @Test
    fun testGetActiveSubtype() {
        doReturn("42/en-CA/qwerty;1/de-DE/qwertz;23/fr-CH/swiss_french")
            .`when`(prefs.localization).subtypes

        // First test activeSubtypeId pointing to existing subtype in list
        doReturn(1).`when`(prefs.localization).activeSubtypeId
        assertThat(
            subtypeManagerUnderTest.getActiveSubtype(),
            `is`(Subtype(1, Locale("de", "DE"), "qwertz"))
        )

        // Now test that the first subtype in the list is returned if activeSubtypeId points to a
        // non-existent subtype and that activeSubtypeId value is updated accordingly.
        doReturn(99).`when`(prefs.localization).activeSubtypeId
        assertThat(
            subtypeManagerUnderTest.getActiveSubtype(),
            `is`(Subtype(42, Locale("en", "CA"), "qwerty"))
        )
        verify(prefs.localization).activeSubtypeId = 42

        // Now test that the subtype list is empty. null should be returned and activeSubtypeId
        // should be returned.
        doReturn("").`when`(prefs.localization).subtypes
        doReturn(99).`when`(prefs.localization).activeSubtypeId
        assertThat(
            subtypeManagerUnderTest.getActiveSubtype(),
            `is`(nullValue())
        )
        verify(prefs.localization).activeSubtypeId = -1
    }

    @Test
    fun testGetSubtypeById() {
        doReturn("42/en-CA/qwerty;1/de-DE/qwertz;23/fr-CH/swiss_french")
            .`when`(prefs.localization).subtypes

        // First test a existing subtype for given id.
        assertThat(
            subtypeManagerUnderTest.getSubtypeById(1),
            `is`(Subtype(1, Locale("de", "DE"), "qwertz"))
        )

        // Now test that a non-existing subtype for given id.
        assertThat(
            subtypeManagerUnderTest.getSubtypeById(99),
            `is`(nullValue())
        )
    }

    @Test
    fun testModifySubtypeWithSameId() {
        doReturn("42/en-CA/qwerty;1/de-DE/qwertz;23/fr-CH/swiss_french")
            .`when`(prefs.localization).subtypes

        // First test a subtype with id that does not exist in subtype list.
        subtypeManagerUnderTest.modifySubtypeWithSameId(
            Subtype(99, Locale("de", "AT"), "qwertz")
        )
        verify(prefs.localization).subtypes = "42/en-CA/qwerty;1/de-DE/qwertz;23/fr-CH/swiss_french"

        // Now test a subtype with id that does exist in subtype list.
        subtypeManagerUnderTest.modifySubtypeWithSameId(
            Subtype(23, Locale("de", "AT"), "qwertz")
        )
        verify(prefs.localization).subtypes = "42/en-CA/qwerty;1/de-DE/qwertz;23/de-AT/qwertz"
    }

    @Test
    fun testRemoveSubtype() {
        doReturn("42/en-CA/qwerty;1/de-DE/qwertz;23/fr-CH/swiss_french")
            .`when`(prefs.localization).subtypes

        // First test that subtype is removed from list.
        subtypeManagerUnderTest.removeSubtype(
            Subtype(42, Locale("en", "CA"), "qwerty")
        )
        verify(prefs.localization).subtypes = "1/de-DE/qwertz;23/fr-CH/swiss_french"

        // Now test that list stays untouched when attempting to remove non-existent subtype.
        subtypeManagerUnderTest.removeSubtype(
            Subtype(99, Locale("es", "ES"), "spanish")
        )
        verify(prefs.localization).subtypes = "42/en-CA/qwerty;1/de-DE/qwertz;23/fr-CH/swiss_french"
    }

    @Test
    fun testSwitchToNextSubtype() {
        doReturn("1/en-CA/qwerty;2/de-DE/qwertz;3/fr-CH/swiss_french")
            .`when`(prefs.localization).subtypes

        // First test that next subtype is returned from existing id.
        doReturn(2).`when`(prefs.localization).activeSubtypeId
        assertThat(
            subtypeManagerUnderTest.switchToNextSubtype(),
            `is`(Subtype(3, Locale("fr", "CH"), "swiss_french"))
        )
        verify(prefs.localization).activeSubtypeId = 3

        // Now test that second subtype is selected when id points to non-existent.
        doReturn(99).`when`(prefs.localization).activeSubtypeId
        assertThat(
            subtypeManagerUnderTest.switchToNextSubtype(),
            `is`(Subtype(2, Locale("de", "DE"), "qwertz"))
        )
        verify(prefs.localization).activeSubtypeId = 2

        // Now test that null / -1 is used when list is empty.
        doReturn("").`when`(prefs.localization).subtypes
        doReturn(99).`when`(prefs.localization).activeSubtypeId
        assertThat(
            subtypeManagerUnderTest.switchToNextSubtype(),
            `is`(nullValue())
        )
        verify(prefs.localization).activeSubtypeId = -1
    }*/
}
