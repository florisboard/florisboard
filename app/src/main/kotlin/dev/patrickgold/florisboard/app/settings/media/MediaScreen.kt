/*
 * Copyright (C) 2022 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.settings.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSkinTone
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.pluralsRes
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun MediaScreen() = FlorisScreen {
    title = stringRes(R.string.settings__media__title)
    previewFieldVisible = true
    iconSpaceReserved = false

    content {
        ListPreference(
            prefs.media.emojiPreferredSkinTone,
            title = stringRes(R.string.prefs__media__emoji_preferred_skin_tone),
            entries = EmojiSkinTone.listEntries(),
        )
        val maxSize by prefs.media.emojiRecentlyUsedMaxSize.observeAsState()
        DialogSliderPreference(
            prefs.media.emojiRecentlyUsedMaxSize,
            title = stringRes(R.string.prefs__media__emoji_recently_used_max_size),
            summary = if (maxSize == 0) {
                stringRes(R.string.general__unlimited)
            } else {
                pluralsRes(R.plurals.unit__items__written, maxSize)
            },
            min = 0,
            max = 120,
            stepIncrement = 1,
        )
    }
}
