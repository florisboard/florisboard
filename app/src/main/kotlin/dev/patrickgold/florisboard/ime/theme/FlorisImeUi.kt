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

enum class FlorisImeUi(val elementName: String) {
    Root("root"),

    Keyboard("keyboard"),
    Key("key"),
    KeyHint("key-hint"),
    KeyPopupBox("key-popup-box"),
    KeyPopupElement("key-popup-element"),
    KeyPopupExtendedIndicator("key-popup-extended-indicator"),

    ClipboardHeader("clipboard-header"),
    ClipboardSubheader("clipboard-subheader"),
    ClipboardContent("clipboard-content"),
    ClipboardItem("clipboard-item"),
    ClipboardItemPopup("clipboard-item-popup"),
    ClipboardItemPopupAction("clipboard-item-popup-action"),
    ClipboardItemPopupActionIcon("clipboard-item-popup-action-icon"),
    ClipboardItemPopupActionText("clipboard-item-popup-action-text"),
    ClipboardEnableHistoryButton("clipboard-enable-history-button"),

    EmojiKey("emoji-key"),
    EmojiKeyPopup("emoji-key-popup"),
    EmojiTab("emoji-tab"),

    ExtractedLandscapeInputLayout("extracted-landscape-input-layout"),
    ExtractedLandscapeInputField("extracted-landscape-input-field"),
    ExtractedLandscapeInputAction("extracted-landscape-input-action"),

    GlideTrail("glide-trail"),

    IncognitoModeIndicator("incognito-mode-indicator"),

    InlineAutofillChip("inline-autofill-chip"),

    OneHandedPanel("one-handed-panel"),
    OneHandedPanelButton("one-handed-panel-button"),

    Smartbar("smartbar"),
    SmartbarSharedActionsRow("smartbar-shared-actions-row"),
    SmartbarSharedActionsToggle("smartbar-shared-actions-toggle"),
    SmartbarExtendedActionsRow("smartbar-extended-actions-row"),
    SmartbarExtendedActionsToggle("smartbar-extended-actions-toggle"),
    SmartbarActionKey("smartbar-action-key"),
    SmartbarActionTile("smartbar-action-tile"),
    SmartbarActionsOverflow("smartbar-actions-overflow"),
    SmartbarActionsOverflowCustomizeButton("smartbar-actions-overflow-customize-button"),

    SmartbarActionsEditor("smartbar-actions-editor"),
    SmartbarActionsEditorHeader("smartbar-actions-editor-header"),
    SmartbarActionsEditorSubheader("smartbar-actions-editor-subheader"),
    SmartbarActionsEditorTileGrid("smartbar-actions-editor-tile-grid"),
    SmartbarActionsEditorTile("smartbar-actions-editor-tile"),

    SmartbarCandidatesRow("smartbar-candidates-row"),
    SmartbarCandidateWord("smartbar-candidate-word"),
    SmartbarCandidateClip("smartbar-candidate-clip"),
    SmartbarCandidateSpacer("smartbar-candidate-spacer");

    companion object {
        val elementNames by lazy { entries.map { it.elementName } }
    }

    object Attr {
        const val Code = "code"
        const val Mode = "mode"
        const val ShiftState = "shiftstate"
    }
}
