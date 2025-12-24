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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
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
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.FlorisIconButton
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import org.florisboard.lib.snygg.ui.uriOrNull

@Composable
fun ImeSystemUi() {
    val ims = LocalFlorisImeService.current

    val windowSpec by ims.windowController.activeWindowSpec.collectAsState()
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

@Composable
fun ImeSystemUiFloating(moveModifier: Modifier) {
    val ims = LocalFlorisImeService.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlorisIconButton(
            onClick = {
                ims.hideUi()
            }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Close floating keyboard window")
        }

        NavigationPill(moveModifier)

        FlorisIconButton(
            onClick = {
                ims.switchToNextInputMethod()
            },
            onLongClick = {
                InputMethodUtils.showImePicker(ims)
            }
        ) {
            Icon(Icons.Default.Language, "Click: switch to next input method; Longclick: Show ime picker")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.NavigationPill(moveModifier: Modifier) {
    val backgroundQuery = rememberSnyggThemeQuery(FlorisImeUi.Window.elementName)
    val backgroundColor = backgroundQuery.background()
    val backgroundImage = backgroundQuery.backgroundImage.uriOrNull()

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

    val style = rememberSnyggThemeQuery(FlorisImeUi.WindowMoveHandleFloating.elementName)
    val scrimColor = style.background(defaultScrimColor)

    val size = DpSize(80.dp, 5.dp)

    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .wrapContentHeight()
            .then(moveModifier)
            .padding(vertical = 20.dp)
            .align(Alignment.CenterVertically)
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
