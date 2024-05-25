package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

enum class IncognitoDisplayMode {
    REPLACE_SHARED_ACTIONS_TOGGLE,
    DISPLAY_BEHIND_KEYBOARD;

    companion object {
        @Composable
        fun listEntries() = listPrefEntries {
            entry(
                key = REPLACE_SHARED_ACTIONS_TOGGLE,
                label = stringRes(id = R.string.enum__incognito_display_mode__replace_shared_actions_toggle),
            )
            entry(
                key = DISPLAY_BEHIND_KEYBOARD,
                label = stringRes(id = R.string.enum__incognito_display_mode__display_behind_keyboard),
            )
        }
    }
}
