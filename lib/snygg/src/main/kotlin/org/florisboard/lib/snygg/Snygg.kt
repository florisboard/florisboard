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

package org.florisboard.lib.snygg

/**
 * Main object for defining all known Snygg property names.
 *
 * snygg = Swedish for stylish
 */
// TODO: How can we make this compose state conform
object Snygg {
    const val Width = "width"
    const val Height = "height"

    // TODO: add onImagePathResolve API for library consumer
    const val Background = "background" // TODO: add image() function
    // TODO: object-fit support
    const val Foreground = "foreground"

    const val BorderColor = "border-color"
    const val BorderStyle = "border-style"
    const val BorderWidth = "border-width"

    // TODO: font-family also needs a ref-like system, like image too
    const val FontFamily = "font-family" // TODO: no impact yet???
    const val FontSize = "font-size" // TODO: no impact yet???
    const val FontStyle = "font-style"
    const val FontVariant = "font-variant"
    const val FontWeight = "font-weight" // TODO: no impact yet???

    const val Margin = "margin"
    const val Padding = "padding"

    const val ShadowColor = "shadow-color"
    const val ShadowElevation = "shadow-elevation"

    const val Shape = "shape"
}
