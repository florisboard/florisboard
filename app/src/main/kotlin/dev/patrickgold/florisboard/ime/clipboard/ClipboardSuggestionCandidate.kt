/*
 * Copyright (C) 2023 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.clipboard

import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.lib.util.NetworkUtils

/**
 * Default implementation for a clipboard candidate. Should generally not be used by a suggestion provider, except by
 * the clipboard suggestion provider.
 *
 * @see SuggestionCandidate
 */
class ClipboardSuggestionCandidate(
    val clipboardItem: ClipboardItem,
    sourceProvider: SuggestionProvider?,
) : SuggestionCandidate(
    text = clipboardItem.stringRepresentation(),
    secondaryText = null,
    confidence = 1.0,
    isEligibleForAutoCommit = false,
    isEligibleForUserRemoval = true,
    iconId = when (clipboardItem.type) {
        ItemType.TEXT -> when {
            NetworkUtils.isEmailAddress(clipboardItem.stringRepresentation()) -> R.drawable.ic_email
            NetworkUtils.isUrl(clipboardItem.stringRepresentation()) -> R.drawable.ic_link
            NetworkUtils.isPhoneNumber(clipboardItem.stringRepresentation()) -> R.drawable.ic_phone
            else -> R.drawable.ic_assignment
        }
        ItemType.IMAGE -> R.drawable.ic_image
        ItemType.VIDEO -> R.drawable.ic_videocam
    },
    sourceProvider = sourceProvider,
)
