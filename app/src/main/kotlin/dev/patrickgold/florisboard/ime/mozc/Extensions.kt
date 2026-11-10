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

package dev.patrickgold.florisboard.ime.mozc

import org.k3lp.lib.text.K3String
import org.k3lp.lib.text.asK3String
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands


fun ProtoCommands.Preedit?.isNotEmpty() = length > 0

fun ProtoCommands.Preedit?.asK3String(): K3String = asNullableString()?.asK3String() ?: "".asK3String()

fun ProtoCommands.Preedit?.asString(): String = asNullableString() ?: ""

fun ProtoCommands.Preedit?.asNullableString(): String? {
    return this?.segmentList
            ?.stream()
            ?.map { seg -> seg.value }
            ?.reduce("") { obj: String?, s: String? -> obj + s }
}

val ProtoCommands.Preedit?.length: Int
    get() {
        return if (this == null) {
            0
        } else {
            var length = 0
            for (segment in segmentList) {
                length += segment.value.length
            }
            length
        }
    }
