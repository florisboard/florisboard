/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.ui.components

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R

@Composable
fun PreviewKeyboardField(
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.settings__preview_keyboard),
) {
    val context = LocalContext.current
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var text by remember { mutableStateOf(TextFieldValue("")) }
    TextField(
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusEvent { hasFocus = it.isFocused },
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(hint) },
        trailingIcon = {
            Row {
                IconButton(onClick = {
                    if (hasFocus) focusManager.clearFocus() else focusRequester.requestFocus()
                }) {
                    Icon(
                        painter = painterResource(id = when {
                            hasFocus -> R.drawable.ic_keyboard_arrow_down
                            else -> R.drawable.ic_keyboard_arrow_up
                        }),
                        contentDescription = null,
                    )
                }
                IconButton(onClick = {
                    if (inputMethodManager != null) {
                        inputMethodManager.showInputMethodPicker()
                    } else {
                        Toast.makeText(
                            context, "Error: InputMethodManager service not available!", Toast.LENGTH_SHORT
                        ).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_keyboard),
                        contentDescription = null,
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(autoCorrect = false),
        singleLine = true,
        shape = RectangleShape,
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}
