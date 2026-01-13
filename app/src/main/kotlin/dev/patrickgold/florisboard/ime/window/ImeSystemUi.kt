/*
 * Copyright (C) 2021-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.window

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.inputmethodservice.InputMethodService
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.compose.drawableRes
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import org.florisboard.lib.snygg.ui.uriOrNull

/**
 * Configures the system navigation and caption bar.
 *
 * Itself this composable does not take up any space in the layout.
 */
@Composable
fun ImeSystemUi() {
    val windowController = LocalWindowController.current

    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val isSystemNavbarVisible by remember {
        derivedStateOf { windowSpec is ImeWindowSpec.Fixed }
    }

    val backgroundQuery = rememberSnyggThemeQuery(FlorisImeUi.Window.elementName)
    val backgroundColor = backgroundQuery.background()
    val backgroundImage = backgroundQuery.backgroundImage.uriOrNull()

    val hasBackgroundImage = backgroundImage != null
    val useDarkIcons = if (backgroundImage == null) {
        backgroundColor.luminance() >= 0.5
    } else {
        false
    }

    val view = LocalView.current
    val window = view.context.findWindow()!!
    val windowInsetsController = WindowInsetsControllerCompat(window, view)

    LaunchedEffect(isSystemNavbarVisible) {
        if (isSystemNavbarVisible) {
            windowInsetsController.show(WindowInsetsCompat.Type.captionBar())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.captionBar())
        }
    }

    LaunchedEffect(useDarkIcons, hasBackgroundImage) {
        windowInsetsController.isAppearanceLightNavigationBars = useDarkIcons
        if (AndroidVersion.ATLEAST_API29_Q) {
            window.isNavigationBarContrastEnforced = hasBackgroundImage
        }
    }
}

/**
 * Mimics a system caption bar in a floating window.
 */
@Composable
fun ImeSystemUiFloating() {
    val windowController = LocalWindowController.current
    val windowConfig by windowController.activeWindowConfig.collectAsState()
    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val editor by windowController.editor.state.collectAsState()

    val visible by remember {
        derivedStateOf {
            windowSpec is ImeWindowSpec.Floating
        }
    }
    val showHideAndSwitch by remember {
        derivedStateOf {
            editor.isEnabled
        }
    }
    val isNonDefaultSize by remember {
        derivedStateOf {
            when (val spec = windowSpec) {
                is ImeWindowSpec.Fixed -> false // wtf
                is ImeWindowSpec.Floating -> {
                    val defaultProps = spec.constraints.defaultProps
                    val props = spec.props
                    defaultProps.keyboardHeight != props.keyboardHeight ||
                        defaultProps.keyboardWidth != props.keyboardWidth
                }
            }
        }
    }

    val attributes = remember(windowConfig.mode) {
        mapOf(
            FlorisImeUi.Attr.WindowMode to windowConfig.mode.toString()
        )
    }

    if (visible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val weightModifier = Modifier.weight(1f)

            if (!showHideAndSwitch) {
                Box(
                    modifier = weightModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    FlorisIconButton(
                        onClick = {
                            FlorisImeService.hideUi()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, null)
                    }
                }
            } else {
                Box(
                    modifier = weightModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    Spacer(modifier = Modifier.size(DpSize(52.dp, 30.dp)))
                }
            }

            NavigationPill()

            if (!showHideAndSwitch) {
                Box(
                    modifier = weightModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    FlorisIconButton(
                        onClick = {
                            FlorisImeService.switchToNextInputMethod()
                        },
                        onLongClick = {
                            FlorisImeService.showImePicker()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Language, null)
                    }
                }
            } else {
                Box(
                    modifier = weightModifier,
                    contentAlignment = Alignment.Center,
                ) {
                    if (isNonDefaultSize) {
                        SnyggButton(
                            elementName = FlorisImeUi.WindowResizeAction.elementName,
                            attributes = attributes,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            onClick = {
                                windowController.actions.resetFloatingSize()
                            }
                        ) {
                            SnyggIcon(imageVector = drawableRes(R.drawable.ic_restart_alt))
                            SnyggText(text = stringRes(R.string.action__reset))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavigationPill() {
    val windowController = LocalWindowController.current

    val windowConfig by windowController.activeWindowConfig.collectAsState()

    val backgroundQuery = rememberSnyggThemeQuery(FlorisImeUi.Window.elementName)
    val backgroundColor = backgroundQuery.background()
    val backgroundImage = backgroundQuery.backgroundImage.uriOrNull()

    val attributes = remember(windowConfig.mode) {
        mapOf(
            FlorisImeUi.Attr.WindowMode to windowConfig.mode.toString()
        )
    }

    val useDarkIcons = if (backgroundImage == null) {
        backgroundColor.luminance() >= 0.5
    } else {
        false
    }

    val defaultScrimColor = if (useDarkIcons) {
        Color.Black
    } else {
        Color.White
    }.copy(0.5f)

    val style = rememberSnyggThemeQuery(FlorisImeUi.WindowMoveHandle.elementName, attributes)
    val scrimColor = style.background(defaultScrimColor)

    val size = DpSize(80.dp, 5.dp)

    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .wrapContentHeight()
            .imeWindowMoveHandle(windowController, onTap = {
                windowController.editor.toggleEnabled()
            })
            .padding(vertical = 20.dp)
            .align(Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(scrimColor, RoundedCornerShape(25.dp))
                .align(Alignment.Center)
        )
    }
}

private tailrec fun Context.findWindow(): Window? {
    val context = this
    if (context is Activity) return context.window
    if (context is InputMethodService) return context.window?.window
    return if (context is ContextWrapper) context.findWindow() else null
}
