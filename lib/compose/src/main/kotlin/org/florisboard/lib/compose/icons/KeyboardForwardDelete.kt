/*
 * Copyright 2025 The Android Open Source Project
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

package org.florisboard.lib.compose.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// Adapted from original androidx.compose.material.icons.automirrored.filled Backspace icon
// Changes: manually inverted path
@Suppress("UnusedReceiverParameter")
val Icons.AutoMirrored.Filled.ForwardDelete: ImageVector
    get() {
        if (_forwardDelete != null) {
            return _forwardDelete!!
        }
        _forwardDelete = materialIcon(name = "AutoMirrored.Filled.ForwardDelete", autoMirror = true) {
            materialPath {
                moveTo(2.0f, 3.0f)
                lineTo(17.0f, 3.0f)
                curveToRelative(0.69f, 0.0f, 1.23f, 0.35f, 1.59f, 0.88f)
                lineTo(24.0f, 12.0f)
                lineToRelative(-5.41f, 8.11f)
                curveToRelative(-0.36f, 0.53f, -0.9f, 0.89f, -1.59f, 0.89f)
                horizontalLineToRelative(-15.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                lineTo(0.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                close()
                moveTo(5.0f, 15.59f)
                lineTo(6.41f, 17.0f)
                lineTo(10.0f, 13.41f)
                lineTo(13.59f, 17.0f)
                lineTo(15.0f, 15.59f)
                lineTo(11.41f, 12.0f)
                lineTo(15.0f, 8.41f)
                lineTo(13.59f, 7.0f)
                lineTo(10.0f, 10.59f)
                lineTo(6.41f, 7.0f)
                lineTo(5.0f, 8.41f)
                lineTo(8.59f, 12.0f)
                lineTo(5.0f, 15.59f)
                close()
            }
        }
        return _forwardDelete!!
    }

@Suppress("ObjectPropertyName")
private var _forwardDelete: ImageVector? = null
