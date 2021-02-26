package dev.patrickgold.florisboard.ime.core

import android.content.Context
import android.content.SharedPreferences
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

/**
 * Helper class which automatically sets up all mocks for the different pref categories.
 * Run with MockitoJUnitRunner.Silent::class to avoid the UnnecessaryStubbingException!
 */
class TestablePrefHelper(
    private val context: Context,
    val shared: SharedPreferences = mock(SharedPreferences::class.java)
) {
    val instance:       PrefHelper              = mock(PrefHelper::class.java)
    val advanced:       PrefHelper.Advanced     = mock(PrefHelper.Advanced::class.java)
    val correction:     PrefHelper.Correction   = mock(PrefHelper.Correction::class.java)
    val internal:       PrefHelper.Internal     = mock(PrefHelper.Internal::class.java)
    val localization:   PrefHelper.Localization = mock(PrefHelper.Localization::class.java)
    val keyboard:       PrefHelper.Keyboard     = mock(PrefHelper.Keyboard::class.java)
    val suggestion:     PrefHelper.Suggestion   = mock(PrefHelper.Suggestion::class.java)
    val theme:          PrefHelper.Theme        = mock(PrefHelper.Theme::class.java)

    init {
        doReturn(advanced).`when`(instance).advanced
        doReturn(correction).`when`(instance).correction
        doReturn(internal).`when`(instance).internal
        doReturn(localization).`when`(instance).localization
        doReturn(keyboard).`when`(instance).keyboard
        doReturn(suggestion).`when`(instance).suggestion
        doReturn(theme).`when`(instance).theme
    }
}
