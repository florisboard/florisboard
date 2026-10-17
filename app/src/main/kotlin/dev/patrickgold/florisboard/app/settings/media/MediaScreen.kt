/*
 * Copyright (C) 2024-2026 The FlorisBoard Contributors
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

import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.components.ScrollableScreenColumn
import dev.patrickgold.florisboard.app.components.SegmentedListColumn
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.media.emoji.EmojiHistory
import dev.patrickgold.florisboard.ime.media.emoji.EmojiHistoryHelper
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSkinTone
import dev.patrickgold.florisboard.ime.media.emoji.EmojiSuggestionType
import dev.patrickgold.florisboard.lib.compose.FlorisScreenNg
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.pluralsRes
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun MediaScreen() {
    val prefs by FlorisPreferenceStore

    var shouldDelete by remember { mutableStateOf<ShouldDelete?>(null) }
    val scope = rememberCoroutineScope()

    FlorisScreenNg(
        title = stringRes(R.string.settings__media__title),
        previewFieldVisible = true,
    ) { contentPadding ->
        ScrollableScreenColumn(contentPadding) {
            SegmentedListColumn {
                val count = 1
                ListPreference(
                    prefs.emoji.preferredSkinTone,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.prefs__media__emoji_preferred_skin_tone),
                    entries = enumDisplayEntriesOf(EmojiSkinTone::class),
                )
            }

            SegmentedListColumn(title = stringRes(R.string.prefs__media__emoji_history__title)) {
                val count = 6
                SwitchPreference(
                    prefs.emoji.historyEnabled,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.prefs__media__emoji_history_enabled),
                    summary = stringRes(R.string.prefs__media__emoji_history_enabled__summary),
                )
                ListPreference(
                    prefs.emoji.historyPinnedUpdateStrategy,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.prefs__media__emoji_history_pinned_update_strategy),
                    entries = enumDisplayEntriesOf(EmojiHistory.UpdateStrategy::class),
                    enabledIf = { prefs.emoji.historyEnabled.isTrue() },
                )
                ListPreference(
                    prefs.emoji.historyRecentUpdateStrategy,
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.prefs__media__emoji_history_recent_update_strategy),
                    entries = enumDisplayEntriesOf(EmojiHistory.UpdateStrategy::class),
                    enabledIf = { prefs.emoji.historyEnabled.isTrue() },
                )
                DialogSliderPreference(
                    primaryPref = prefs.emoji.historyPinnedMaxSize,
                    secondaryPref = prefs.emoji.historyRecentMaxSize,
                    shapes = ListItemDefaults.segmentedShapes(3, count),
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
                Preference(
                    shapes = ListItemDefaults.segmentedShapes(4, count),
                    title = stringRes(R.string.prefs__media__emoji_history_pinned_reset),
                    onClick = {
                        shouldDelete = ShouldDelete(true)
                    },
                    enabledIf = { prefs.emoji.historyEnabled.isTrue() },
                )
                Preference(
                    shapes = ListItemDefaults.segmentedShapes(5, count),
                    title = stringRes(R.string.prefs__media__emoji_history_reset),
                    onClick = {
                        shouldDelete = ShouldDelete(false)
                    },
                    enabledIf = { prefs.emoji.historyEnabled.isTrue() },
                )
            }

            SegmentedListColumn(title = stringRes(R.string.prefs__media__emoji_suggestion__title)) {
                val count = 6
                SwitchPreference(
                    prefs.emoji.suggestionEnabled,
                    shapes = ListItemDefaults.segmentedShapes(0, count),
                    title = stringRes(R.string.prefs__media__emoji_suggestion_enabled),
                    summary = stringRes(R.string.prefs__media__emoji_suggestion_enabled__summary),
                )
                ListPreference(
                    prefs.emoji.suggestionType,
                    shapes = ListItemDefaults.segmentedShapes(1, count),
                    title = stringRes(R.string.prefs__media__emoji_suggestion_type),
                    entries = enumDisplayEntriesOf(EmojiSuggestionType::class),
                    enabledIf = { prefs.emoji.suggestionEnabled.isTrue() },
                )
                SwitchPreference(
                    prefs.emoji.suggestionUpdateHistory,
                    shapes = ListItemDefaults.segmentedShapes(2, count),
                    title = stringRes(R.string.prefs__media__emoji_suggestion_update_history),
                    summary = stringRes(R.string.prefs__media__emoji_suggestion_update_history__summary),
                    enabledIf = {
                        prefs.emoji.suggestionEnabled.isTrue() && prefs.emoji.historyEnabled.isTrue()
                    },
                )
                SwitchPreference(
                    prefs.emoji.suggestionCandidateShowName,
                    shapes = ListItemDefaults.segmentedShapes(3, count),
                    title = stringRes(R.string.prefs__media__emoji_suggestion_candidate_show_name),
                    summary = stringRes(R.string.prefs__media__emoji_suggestion_candidate_show_name__summary),
                    enabledIf = { prefs.emoji.suggestionEnabled.isTrue() },
                )
                DialogSliderPreference(
                    prefs.emoji.suggestionQueryMinLength,
                    shapes = ListItemDefaults.segmentedShapes(4, count),
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
                    shapes = ListItemDefaults.segmentedShapes(5, count),
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

        DeleteEmojiHistoryConfirmDialog(
            shouldDelete = shouldDelete,
            onDismiss = {
                shouldDelete = null
            },
            onConfirm = {
                shouldDelete?.let {
                    scope.launch {
                        if (it.pinned) {
                            EmojiHistoryHelper.deletePinned(prefs = prefs)
                        } else {
                            EmojiHistoryHelper.deleteHistory(prefs = prefs)
                        }
                    }
                    shouldDelete = null
                }
            },
        )
    }
}

@Composable
fun DeleteEmojiHistoryConfirmDialog(
    shouldDelete: ShouldDelete?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    shouldDelete?.let {
        JetPrefAlertDialog(
            title = stringRes(R.string.action__reset_confirm_title),
            confirmLabel = stringRes(R.string.action__yes),
            dismissLabel = stringRes(R.string.action__no),
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        ) {
            if (it.pinned) {
                Text(stringRes(R.string.action__reset_confirm_message, "name" to "pinned emojis"))
            } else {
                Text(stringRes(R.string.action__reset_confirm_message, "name" to "emoji history"))
            }

        }
    }
}

data class ShouldDelete(val pinned: Boolean)
