/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.theme

import dev.patrickgold.florisboard.R

enum class FlorisImeUi(val elementName: String, val resId: Int?) {
    Root(
        elementName = "root",
        resId = R.string.snygg__rule_element__root,
    ),
    Window(
        elementName = "window",
        resId = R.string.snygg__rule_element__window,
    ),

    Key(
        elementName = "key",
        resId = R.string.snygg__rule_element__key,
    ),
    KeyHint(
        elementName = "key-hint",
        resId = R.string.snygg__rule_element__key_hint,
    ),
    KeyPopupBox(
        elementName = "key-popup-box",
        resId = R.string.snygg__rule_element__key_popup_box,
    ),
    KeyPopupElement(
        elementName = "key-popup-element",
        resId = R.string.snygg__rule_element__key_popup_element,
    ),
    KeyPopupExtendedIndicator(
        elementName = "key-popup-extended-indicator",
        resId = R.string.snygg__rule_element__key_popup_extended_indicator,
    ),

    ClipboardHeader(
        elementName = "clipboard-header",
        resId = R.string.snygg__rule_element__clipboard_header,
    ),
    ClipboardHeaderButton(
        elementName = "clipboard-header-button",
        resId = R.string.snygg__rule_element__clipboard_header_button,
    ),
    ClipboardHeaderText(
        elementName = "clipboard-header-text",
        resId = R.string.snygg__rule_element__clipboard_header_text,
    ),
    ClipboardSubheader(
        elementName = "clipboard-subheader",
        resId = R.string.snygg__rule_element__clipboard_subheader,
    ),
    ClipboardContent(
        elementName = "clipboard-content",
        resId = R.string.snygg__rule_element__clipboard_content,
    ),
    ClipboardItem(
        elementName = "clipboard-item",
        resId = R.string.snygg__rule_element__clipboard_item,
    ),
    ClipboardItemPopup(
        elementName = "clipboard-item-popup",
        resId = R.string.snygg__rule_element__clipboard_item_popup,
    ),
    ClipboardItemActions(
        elementName = "clipboard-item-actions",
        resId = R.string.snygg__rule_element__clipboard_item_actions,
    ),
    ClipboardItemAction(
        elementName = "clipboard-item-action",
        resId = R.string.snygg__rule_element__clipboard_item_action,
    ),
    ClipboardItemActionIcon(
        elementName = "clipboard-item-action-icon",
        resId = R.string.snygg__rule_element__clipboard_item_action_icon,
    ),
    ClipboardItemActionText(
        elementName = "clipboard-item-action-text",
        resId = R.string.snygg__rule_element__clipboard_item_action_text,
    ),
    ClipboardClearAllDialog(
        elementName = "clipboard-clear-all-dialog",
        resId = R.string.snygg__rule_element__clipboard_clear_all_dialog,
    ),
    ClipboardClearAllDialogMessage(
        elementName = "clipboard-clear-all-dialog-message",
        resId = R.string.snygg__rule_element__clipboard_clear_all_dialog_message,
    ),
    ClipboardClearAllDialogButtons(
        elementName = "clipboard-clear-all-dialog-buttons",
        resId = R.string.snygg__rule_element__clipboard_clear_all_dialog_buttons,
    ),
    ClipboardClearAllDialogButton(
        elementName = "clipboard-clear-all-dialog-button",
        resId = R.string.snygg__rule_element__clipboard_clear_all_dialog_button,
    ),
    ClipboardHistoryDisabledTitle(
        elementName = "clipboard-history-disabled-title",
        resId = R.string.snygg__rule_element__clipboard_history_disabled_title,
    ),
    ClipboardHistoryDisabledMessage(
        elementName = "clipboard-history-disabled-message",
        resId = R.string.snygg__rule_element__clipboard_history_disabled_message,
    ),
    ClipboardHistoryDisabledButton(
        elementName = "clipboard-history-disabled-button",
        resId = R.string.snygg__rule_element__clipboard_history_disabled_button,
    ),
    ClipboardHistoryLockedTitle(
        elementName = "clipboard-history-locked-title",
        resId = R.string.snygg__rule_element__clipboard_history_locked_title,
    ),
    ClipboardHistoryLockedMessage(
        elementName = "clipboard-history-locked-message",
        resId = R.string.snygg__rule_element__clipboard_history_locked_message,
    ),

    ExtractedLandscapeInputLayout(
        elementName = "extracted-landscape-input-layout",
        resId = R.string.snygg__rule_element__extracted_landscape_input_layout,
    ),
    ExtractedLandscapeInputField(
        elementName = "extracted-landscape-input-field",
        resId = R.string.snygg__rule_element__extracted_landscape_input_field,
    ),
    ExtractedLandscapeInputAction(
        elementName = "extracted-landscape-input-action",
        resId = R.string.snygg__rule_element__extracted_landscape_input_action,
    ),

    GlideTrail(
        elementName = "glide-trail",
        resId = R.string.snygg__rule_element__glide_trail,
    ),

    IncognitoModeIndicator(
        elementName = "incognito-mode-indicator",
        resId = R.string.snygg__rule_element__incognito_mode_indicator,
    ),

    InlineAutofillChip(
        elementName = "inline-autofill-chip",
        resId = R.string.snygg__rule_element__inline_autofill_chip,
    ),

    Media(
        elementName = "media",
        resId = R.string.snygg__rule_element__media,
    ),

    MediaEmojiSubheader(
        elementName = "media-emoji-subheader",
        resId = R.string.snygg__rule_element__media_emoji_subheader,
    ),
    MediaEmojiKey(
        elementName = "media-emoji-key",
        resId = R.string.snygg__rule_element__media_emoji_key,
    ),
    MediaEmojiKeyPopupBox(
        elementName = "media-emoji-key-popup-box",
        resId = R.string.snygg__rule_element__media_emoji_key_popup_box,
    ),
    MediaEmojiKeyPopupElement(
        elementName = "media-emoji-key-popup-element",
        resId = R.string.snygg__rule_element__media_emoji_key_popup_element,
    ),
    MediaEmojiKeyPopupExtendedIndicator(
        elementName = "media-emoji-key-popup-extended-indicator",
        resId = R.string.snygg__rule_element__media_emoji_key_popup_extended_indicator,
    ),
    MediaEmojiTab(
        elementName = "media-emoji-tab",
        resId = R.string.snygg__rule_element__media_emoji_tab,
    ),

    MediaBottomRow(
        elementName = "media-bottom-row",
        resId = R.string.snygg__rule_element__media_bottom_row,
    ),
    MediaBottomRowButton(
        elementName = "media-bottom-row-button",
        resId = R.string.snygg__rule_element__media_bottom_row_button,
    ),

    OneHandedPanel(
        elementName = "one-handed-panel",
        resId = R.string.snygg__rule_element__one_handed_panel,
    ),
    OneHandedPanelButton(
        elementName = "one-handed-panel-button",
        resId = R.string.snygg__rule_element__one_handed_panel_button,
    ),

    Smartbar(
        elementName = "smartbar",
        resId = R.string.snygg__rule_element__smartbar,
    ),
    SmartbarSharedActionsRow(
        elementName = "smartbar-shared-actions-row",
        resId = R.string.snygg__rule_element__smartbar_shared_actions_row,
    ),
    SmartbarSharedActionsToggle(
        elementName = "smartbar-shared-actions-toggle",
        resId = R.string.snygg__rule_element__smartbar_shared_actions_toggle,
    ),
    SmartbarExtendedActionsRow(
        elementName = "smartbar-extended-actions-row",
        resId = R.string.snygg__rule_element__smartbar_extended_actions_row,
    ),
    SmartbarExtendedActionsToggle(
        elementName = "smartbar-extended-actions-toggle",
        resId = R.string.snygg__rule_element__smartbar_extended_actions_toggle,
    ),
    SmartbarActionKey(
        elementName = "smartbar-action-key",
        resId = R.string.snygg__rule_element__smartbar_action_key,
    ),

    SmartbarActionTile(
        elementName = "smartbar-action-tile",
        resId = R.string.snygg__rule_element__smartbar_action_tile,
    ),
    SmartbarActionTileIcon(
        elementName = "smartbar-action-tile-icon",
        resId = R.string.snygg__rule_element__smartbar_action_tile_icon,
    ),
    SmartbarActionTileText(
        elementName = "smartbar-action-tile-text",
        resId = R.string.snygg__rule_element__smartbar_action_tile_text,
    ),
    SmartbarActionsOverflow(
        elementName = "smartbar-actions-overflow",
        resId = R.string.snygg__rule_element__smartbar_actions_overflow,
    ),
    SmartbarActionsOverflowCustomizeButton(
        elementName = "smartbar-actions-overflow-customize-button",
        resId = R.string.snygg__rule_element__smartbar_actions_overflow_customize_button,
    ),

    SmartbarActionsEditor(
        elementName = "smartbar-actions-editor",
        resId = R.string.snygg__rule_element__smartbar_actions_editor,
    ),
    SmartbarActionsEditorHeader(
        elementName = "smartbar-actions-editor-header",
        resId = R.string.snygg__rule_element__smartbar_actions_editor_header,
    ),
    SmartbarActionsEditorHeaderButton(
        elementName = "smartbar-actions-editor-header-button",
        resId = R.string.snygg__rule_element__smartbar_actions_editor_header_button,
    ),
    SmartbarActionsEditorSubheader(
        elementName = "smartbar-actions-editor-subheader",
        resId = R.string.snygg__rule_element__smartbar_actions_editor_subheader,
    ),
    SmartbarActionsEditorTileGrid(
        elementName = "smartbar-actions-editor-tile-grid",
        resId = R.string.snygg__rule_element__smartbar_actions_editor_tile_grid,
    ),
    SmartbarActionsEditorTile(
        elementName = "smartbar-actions-editor-tile",
        resId = R.string.snygg__rule_element__smartbar_actions_editor_tile,
    ),

    SmartbarCandidatesRow(
        elementName = "smartbar-candidates-row",
        resId = R.string.snygg__rule_element__smartbar_candidates_row,
    ),
    SmartbarCandidateWord(
        elementName = "smartbar-candidate-word",
        resId = R.string.snygg__rule_element__smartbar_candidate_word,
    ),
    SmartbarCandidateWordText(
        elementName = "smartbar-candidate-word-text",
        resId = R.string.snygg__rule_element__smartbar_candidate_word_text,
    ),
    SmartbarCandidateWordSecondaryText(
        elementName = "smartbar-candidate-word-secondary-text",
        resId = R.string.snygg__rule_element__smartbar_candidate_word_secondary_text,
    ),
    SmartbarCandidateClip(
        elementName = "smartbar-candidate-clip",
        resId = R.string.snygg__rule_element__smartbar_candidate_clip,
    ),
    SmartbarCandidateClipIcon(
        elementName = "smartbar-candidate-clip-icon",
        resId = R.string.snygg__rule_element__smartbar_candidate_clip_icon,
    ),
    SmartbarCandidateClipText(
        elementName = "smartbar-candidate-clip-text",
        resId = R.string.snygg__rule_element__smartbar_candidate_clip_text,
    ),
    SmartbarCandidateSpacer(
        elementName = "smartbar-candidate-spacer",
        resId = R.string.snygg__rule_element__smartbar_candidate_spacer,
    ),

    SubtypePanel(
        elementName = "subtype-panel",
        resId = R.string.snygg__rule_element__subtype_panel,
    ),
    SubtypePanelHeader(
        elementName = "subtype-panel-header",
        resId = R.string.snygg__rule_element__subtype_panel_header,
    ),
    SubtypePanelList(
        elementName = "subtype-panel-list",
        resId = R.string.snygg__rule_element__subtype_panel_list,
    ),
    SubtypePanelListItem(
        elementName = "subtype-panel-list-item",
        resId = R.string.snygg__rule_element__subtype_panel_list_item,
    ),
    SubtypePanelListItemIconLeading(
        elementName = "subtype-panel-list-item-icon-leading",
        resId = R.string.snygg__rule_element__subtype_panel_list_item_icon_leading,
    ),
    SubtypePanelListItemText(
        elementName = "subtype-panel-list-item-text",
        resId = R.string.snygg__rule_element__subtype_panel_list_item_text,
    );

    companion object {
        val elementNames by lazy { entries.map { it.elementName } }

        val elementNamesToOrdinals by lazy {
            val enumEntries = entries
            buildMap {
                enumEntries.forEach { entry ->
                    put(entry.elementName, entry.ordinal)
                }
            }
        }

        val elementNamesToTranslation by lazy {
            val enumEntries = entries
            buildMap {
                put("defines", R.string.snygg__rule_annotation__defines)
                put("font", R.string.snygg__rule_annotation__font)
                enumEntries.forEach { entry ->
                    put(entry.elementName, entry.resId)
                }
            }
        }
    }

    object Attr {
        const val Code = "code"
        const val Mode = "mode"
        const val ShiftState = "shiftstate"
    }
}
