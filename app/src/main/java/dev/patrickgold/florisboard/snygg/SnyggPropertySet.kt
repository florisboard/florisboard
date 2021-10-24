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

package dev.patrickgold.florisboard.snygg

import dev.patrickgold.florisboard.snygg.value.SnyggImplicitInheritValue
import dev.patrickgold.florisboard.snygg.value.SnyggValue

class SnyggPropertySet(val properties: Map<String, SnyggValue>) {
    val width = properties[Snygg.Property.Width] ?: SnyggImplicitInheritValue
    val height = properties[Snygg.Property.Height] ?: SnyggImplicitInheritValue

    val background = properties[Snygg.Property.Background] ?: SnyggImplicitInheritValue

    val borderTop = properties[Snygg.Property.BorderTop] ?: properties[Snygg.Property.Border] ?: SnyggImplicitInheritValue
    val borderBottom = properties[Snygg.Property.BorderBottom] ?: properties[Snygg.Property.Border] ?: SnyggImplicitInheritValue
    val borderStart = properties[Snygg.Property.BorderStart] ?: properties[Snygg.Property.Border] ?: SnyggImplicitInheritValue
    val borderEnd = properties[Snygg.Property.BorderEnd] ?: properties[Snygg.Property.Border] ?: SnyggImplicitInheritValue

    val fontFamily = properties[Snygg.Property.FontFamily] ?: SnyggImplicitInheritValue
    val fontSize = properties[Snygg.Property.FontSize] ?: SnyggImplicitInheritValue
    val fontStyle = properties[Snygg.Property.FontStyle] ?: SnyggImplicitInheritValue
    val fontVariant = properties[Snygg.Property.FontVariant] ?: SnyggImplicitInheritValue
    val fontWeight = properties[Snygg.Property.FontWeight] ?: SnyggImplicitInheritValue

    val foreground = properties[Snygg.Property.Foreground] ?: SnyggImplicitInheritValue

    val shadow = properties[Snygg.Property.Shadow] ?: SnyggImplicitInheritValue
}
