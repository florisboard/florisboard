/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.media.emoji

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.keyboard3.ImeActions
import dev.patrickgold.florisboard.ime.keyboard3.ImeController
import dev.patrickgold.florisboard.ime.keyboard3.ImeEditor
import dev.patrickgold.florisboard.ime.keyboard3.ImeState
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.keyboard3.ui.ImeKeyButton
import dev.patrickgold.florisboard.ime.keyboard3.ui.ImeKeyboardBox
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.FlorisLocale
import kotlinx.coroutines.launch
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggText
import org.k3lp.lib.text.asK3String
import org.k3lp.runtime.K3SurroundingText
import org.k3lp.runtime.K3TextRange
import java.lang.ref.WeakReference

// TODO: this is a proff-of-concept / feasibility check for in-ime-ui text fields
//  a proper nice UX for this layout and official release with follow in a separate PR
//  See: https://github.com/florisboard/florisboard/issues/45
@Composable
fun EmojiSearchLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imeController = LocalImeController.current
    val imeState by imeController.activeState.collectAsState()

    val prefs by FlorisPreferenceStore
    val preferredSkinTone = prefs.emoji.preferredSkinTone.get()

    var emojiData by remember { mutableStateOf(EmojiData.Fallback) }
    LaunchedEffect(Unit) {
        emojiData = EmojiData.get(context, FlorisLocale.ENGLISH)
    }
    val emojis = remember(emojiData, preferredSkinTone) {
        emojiData.bySkinTone[preferredSkinTone] ?: emptyList()
    }

    val scope = rememberCoroutineScope()
    val editor = remember {
        object : ImeEditor(WeakReference(null), FlorisEditorInfo.Unspecified) {
            var value by mutableStateOf(TextFieldValue())

            override fun replaceText(range: IntRange, text: String, newSelection: K3TextRange, newComposition: K3TextRange?) {
                value = value.copy(
                    text = value.text.replaceRange(range, text),
                    selection = newSelection.let { TextRange(it.start, it.end) },
                    composition = newComposition?.let { TextRange(it.start, it.end) },
                )
            }

            override fun setComposition(newComposition: K3TextRange?) {
                value = value.copy(
                    composition = newComposition?.let { TextRange(it.start, it.end) },
                )
            }
        }
    }
    val inputMethod = remember {
        ImeController(initialState = ImeState(editor = editor), imeController.touchModelCache)
    }

    LaunchedEffect(imeState.model) {
        inputMethod.updateState {
            switchModel(imeState.model)
        }
    }

    val searchQuery = editor.value
    var searchResults by remember { mutableStateOf(emptyList<Emoji>()) }
    LaunchedEffect(emojiData, searchQuery) {
        searchResults = emojis.searchByInput(searchQuery.text, 10)
    }

    SnyggColumn(
        elementName = FlorisImeUi.Media.elementName,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        BasicTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = editor.value,
            minLines = 3,
            maxLines = 3,
            onValueChange = { newValue ->
                scope.launch {
                    inputMethod.updateState {
                        editor.value = newValue
                        resetContent(
                            newSelection = newValue.selection.toK3TextRange(),
                            newSurrounding = newValue.getSurroundingText(50, 10),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                showKeyboardOnFocus = null,
            ),
            textStyle = TextStyle(color = Color.Black),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        ) {
            SnyggButton(
                onClick = {
                    imeController.updateStateBlocking {
                        emitDescriptor(ImeActions.ShowTextPanel)
                    }
                }
            ) {
                SnyggText(text = "BACK")
            }
            searchResults.forEach { emoji ->
                EmojiText(
                    modifier = Modifier.clickable {
                        imeController.updateStateBlocking {
                            emitText(emoji.value.asK3String())
                        }
                    },
                    text = emoji.value,
                    emojiCompatInstance = null,
                )
            }
            ImeKeyButton(
                output = ImeActions.Backspace,
            )
        }
        CompositionLocalProvider(LocalImeController provides inputMethod) {
            ImeKeyboardBox()
        }
    }
}

fun TextRange.toK3TextRange() = K3TextRange(start, end)

fun TextFieldValue.getSurroundingText(charsBefore: Int, charsAfter: Int) = K3SurroundingText(
    textBefore = getTextBeforeSelection(charsBefore).text,
    textSelected = getSelectedText().text,
    textAfter = getTextAfterSelection(charsAfter).text,
)
