/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.landscapeinput

import android.annotation.SuppressLint
import android.inputmethodservice.ExtractEditText
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.lib.devtools.flogError
import org.florisboard.lib.compose.ProvideLocalizedResources
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

@SuppressLint("ViewConstructor")
class ExtractedInputRootView(val ims: FlorisImeService, eet: ExtractEditText?) : FrameLayout(ims) {
    val composeView: ComposeView
    val extractEditText: ExtractEditText

    init {
        isHapticFeedbackEnabled = true
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        extractEditText = (eet ?: ExtractEditText(context)).also {
            it.id = android.R.id.inputExtractEditText
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            it.background = null
            it.gravity = Gravity.TOP
            it.isVerticalScrollBarEnabled = true
        }
        addView(extractEditText)

        composeView = ComposeView(context).also { it.setContent { Content() } }
        addView(composeView)
    }

    @Composable
    fun Content() {
        CompositionLocalProvider(
            LocalInputFeedbackController provides ims.inputFeedbackController,
            LocalWindowController provides ims.windowController,
        ) {
            ProvideLocalizedResources(
                ims.resourcesContext,
                appName = R.string.app_name,
                forceLayoutDirection = LayoutDirection.Ltr,
            ) {
                FlorisImeTheme {
                    val activeEditorInfo by ims.editorInstance.activeInfoFlow.collectAsState()
                    val rootInsets by ims.windowController.activeRootInsets.collectAsState()
                    val windowInsets by ims.windowController.activeWindowInsets.collectAsState()
                    val height by remember {
                        derivedStateOf {
                            val rootBounds = rootInsets?.boundsDp ?: return@derivedStateOf 0.dp
                            val windowBounds = windowInsets?.boundsDp ?: return@derivedStateOf 0.dp
                            rootBounds.height - windowBounds.height
                        }
                    }
                    SnyggBox(FlorisImeUi.ExtractedLandscapeInputLayout.elementName) {
                        SnyggRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SnyggBox(
                                elementName = FlorisImeUi.ExtractedLandscapeInputLayout.elementName,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f),
                            ) {
                                val fieldStyle =
                                    rememberSnyggThemeQuery(FlorisImeUi.ExtractedLandscapeInputField.elementName)
                                val foreground = fieldStyle.foreground()
                                AndroidView(
                                    factory = { extractEditText },
                                    update = { view ->
                                        view.background = null
                                        view.backgroundTintList = null
                                        view.foregroundTintList = null
                                        view.setTextColor(foreground.toArgb())
                                        view.setHintTextColor(foreground.copy(foreground.alpha * 0.6f).toArgb())
                                        view.setTextSize(
                                            TypedValue.COMPLEX_UNIT_SP,
                                            fieldStyle.fontSize(default = 16.sp).value,
                                        )
                                    },
                                )
                            }
                            SnyggButton(
                                FlorisImeUi.ExtractedLandscapeInputAction.elementName,
                                onClick = {
                                    if (activeEditorInfo.extractedActionId != 0) {
                                        ims.currentInputConnection?.performEditorAction(activeEditorInfo.extractedActionId)
                                    } else {
                                        ims.editorInstance.performEnterAction(activeEditorInfo.imeOptions.action)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            ) {
                                SnyggText(
                                    text = activeEditorInfo.extractedActionLabel
                                        ?: ims.getTextForImeAction(activeEditorInfo.imeOptions.action.toInt())
                                        ?: "ACTION",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    override fun onAttachedToWindow() {
        removeView(extractEditText)
        super.onAttachedToWindow()
        try {
            (parent as LinearLayout).let { extractEditLayout ->
                extractEditLayout.layoutParams = LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                ).also { it.setMargins(0, 0, 0, 0) }
                extractEditLayout.setPadding(0, 0, 0, 0)
            }
        } catch (e: Throwable) {
            flogError { e.message.toString() }
        }
    }
}
