/*
 * Copyright (C) 2024 Patrick Goldinger
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.media.emoji.EmojiHistory
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSkinTone
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSuggestionType
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.pluralsRes
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun MediaScreen() = FlorisScreen {
    title = stringRes(R.string.settings__media__title)
    previewFieldVisible = true
    iconSpaceReserved = true

    content {
        ListPreference(
            prefs.emoji.preferredSkinTone,
            title = stringRes(R.string.prefs__media__emoji_preferred_skin_tone),
            entries = enumDisplayEntriesOf(EmojiSkinTone::class),
        )

        PreferenceGroup(title = stringRes(R.string.prefs__media__emoji_history__title)) {
            SwitchPreference(
                prefs.emoji.historyEnabled,
                icon = Icons.Outlined.Schedule,
                title = stringRes(R.string.prefs__media__emoji_history_enabled),
                summary = stringRes(R.string.prefs__media__emoji_history_enabled__summary),
            )
            ListPreference(
                prefs.emoji.historyPinnedUpdateStrategy,
                title = stringRes(R.string.prefs__media__emoji_history_pinned_update_strategy),
                entries = enumDisplayEntriesOf(EmojiHistory.UpdateStrategy::class),
                enabledIf = { prefs.emoji.historyEnabled.isTrue() },
            )
            ListPreference(
                prefs.emoji.historyRecentUpdateStrategy,
                title = stringRes(R.string.prefs__media__emoji_history_recent_update_strategy),
                entries = enumDisplayEntriesOf(EmojiHistory.UpdateStrategy::class),
                enabledIf = { prefs.emoji.historyEnabled.isTrue() },
            )
            DialogSliderPreference(
                primaryPref = prefs.emoji.historyPinnedMaxSize,
                secondaryPref = prefs.emoji.historyRecentMaxSize,
                title = stringRes(R.string.prefs__media__emoji_history_max_size),
                primaryLabel = stringRes(R.string.emoji__history__pinned),
                secondaryLabel = stringRes(R.string.emoji__history__recent),
                valueLabel = { maxSize ->
                    if (maxSize == EmojiHistory.MaxSizeUnlimited) {
                        stringRes(R.string.general__unlimited)
                    } else {
                        pluralsRes(R.plurals.unit__items__written, maxSize, "v" to maxSize)
                    }
                },
                min = 0,
                max = 120,
                stepIncrement = 1,
                enabledIf = { prefs.emoji.historyEnabled.isTrue() },
            )
        }

        PreferenceGroup(title = stringRes(R.string.prefs__media__emoji_suggestion__title)) {
            SwitchPreference(
                prefs.emoji.suggestionEnabled,
                icon = Icons.Outlined.EmojiSymbols,
                title = stringRes(R.string.prefs__media__emoji_suggestion_enabled),
                summary = stringRes(R.string.prefs__media__emoji_suggestion_enabled__summary),
            )
            ListPreference(
                prefs.emoji.suggestionType,
                title = stringRes(R.string.prefs__media__emoji_suggestion_type),
                entries = enumDisplayEntriesOf(EmojiSuggestionType::class),
                enabledIf = { prefs.emoji.suggestionEnabled.isTrue() },
            )
            SwitchPreference(
                prefs.emoji.suggestionUpdateHistory,
                title = stringRes(R.string.prefs__media__emoji_suggestion_update_history),
                summary = stringRes(R.string.prefs__media__emoji_suggestion_update_history__summary),
                enabledIf = {
                    prefs.emoji.suggestionEnabled.isTrue() && prefs.emoji.historyEnabled.isTrue()
                },
            )
            SwitchPreference(
                prefs.emoji.suggestionCandidateShowName,
                title = stringRes(R.string.prefs__media__emoji_suggestion_candidate_show_name),
                summary = stringRes(R.string.prefs__media__emoji_suggestion_candidate_show_name__summary),
                enabledIf = { prefs.emoji.suggestionEnabled.isTrue() },
            )
            DialogSliderPreference(
                prefs.emoji.suggestionQueryMinLength,
                title = stringRes(R.string.prefs__media__emoji_suggestion_query_min_length),
                valueLabel = { length ->
                    pluralsRes(R.plurals.unit__characters__written, length, "v" to length)
                },
                min = 1,
                max = 5,
                stepIncrement = 1,
                enabledIf = { prefs.emoji.suggestionEnabled.isTrue() },
            )
            DialogSliderPreference(
                prefs.emoji.suggestionCandidateMaxCount,
                title = stringRes(R.string.prefs__media__emoji_suggestion_candidate_max_count),
                valueLabel = { count ->
                    pluralsRes(R.plurals.unit__candidates__written, count, "v" to count)
                },
                min = 1,
                max = 10,
                stepIncrement = 1,
                enabledIf = { prefs.emoji.suggestionEnabled.isTrue() },
            )
        }
    }
}
