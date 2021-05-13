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
    val instance:       Preferences              = mock(Preferences::class.java)
    val advanced:       Preferences.Advanced     = mock(Preferences.Advanced::class.java)
    val correction:     Preferences.Correction   = mock(Preferences.Correction::class.java)
    val internal:       Preferences.Internal     = mock(Preferences.Internal::class.java)
    val localization:   Preferences.Localization = mock(Preferences.Localization::class.java)
    val keyboard:       Preferences.Keyboard     = mock(Preferences.Keyboard::class.java)
    val suggestion:     Preferences.Suggestion   = mock(Preferences.Suggestion::class.java)
    val theme:          Preferences.Theme        = mock(Preferences.Theme::class.java)

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
