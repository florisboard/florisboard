/*
 * Copyright (C) 2026 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless rencodeuired by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.theme

import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard3.touch.InputShiftState
import dev.patrickgold.florisboard.ime.keyboard3.ui.asAttrValue
import dev.patrickgold.florisboard.ime.window.ImeWindowMode
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.layer.K3LayerId

abstract class ImeUiAttr<T : Any>(
    val name: String,
    val valueSpec: ValueSpec,
) {
    abstract fun encode(value: T): String

    fun eq(value: T): Pair<String, String> {
        return name to encode(value)
    }

    fun eqAnyOf(vararg value: T): Pair<String, List<String>> {
        return name to value.map { encode(it) }
    }

    sealed interface ValueSpec {
        fun validate(input: String): Boolean

        data object AnyString : ValueSpec {
            override fun validate(input: String): Boolean {
                return true
            }
        }

        data class Enumeration(
            val allowedValues: List<String>,
        ) : ValueSpec {
            override fun validate(input: String): Boolean {
                return allowedValues.contains(input)
            }
        }
    }
}

fun <T : Any> MutableMap<String, Any>.putAttr(attr: ImeUiAttr<T>, value: T) {
    put(attr.name, attr.encode(value))
}

object ImeUiAttrs {
    object Output : ImeUiAttr<K3StringOrDescriptor>(
        name = "output",
        valueSpec = ValueSpec.AnyString,
    ) {
        override fun encode(value: K3StringOrDescriptor): String {
            return value.asAttrValue()
        }
    }

    object TargetLayer : ImeUiAttr<K3LayerId>(
        name = "targetlayer",
        valueSpec = ValueSpec.AnyString,
    ) {
        override fun encode(value: K3LayerId): String {
            return value.value
        }
    }

    object Layer : ImeUiAttr<K3LayerId>(
        name = "layer",
        valueSpec = ValueSpec.AnyString,
    ) {
        override fun encode(value: K3LayerId): String {
            return value.value
        }
    }

    object ShiftState : ImeUiAttr<InputShiftState>(
        name = "shiftstate",
        valueSpec = ValueSpec.Enumeration(
            allowedValues = InputShiftState.entries.map { it.toString() },
        ),
    ) {
        override fun encode(value: InputShiftState): String {
            return value.toString()
        }
    }

    object WindowMode : ImeUiAttr<ImeWindowMode>(
        name = "windowmode",
        valueSpec = ValueSpec.Enumeration(
            allowedValues = ImeWindowMode.entries.map { it.toString() },
        ),
    ) {
        override fun encode(value: ImeWindowMode): String {
            return value.toString()
        }
    }

    object ClipboardDialogAction : ImeUiAttr<Boolean>(
        name = "action",
        valueSpec = ValueSpec.Enumeration(
            allowedValues = listOf(
                ClipboardDialogAction.YES,
                ClipboardDialogAction.NO,
            ),
        ),
    ) {
        const val YES = "yes"
        const val NO = "no"

        override fun encode(value: Boolean): String {
            return (if (value) YES else NO)
        }
    }

    object ClipboardItemType : ImeUiAttr<ItemType>(
        name = "type",
        valueSpec = ValueSpec.Enumeration(
            allowedValues = ItemType.entries.map { it.name.lowercase() },
        ),
    ) {
        override fun encode(value: ItemType): String {
            return value.name.lowercase()
        }
    }

    object ClipboardFilterState : ImeUiAttr<Boolean>(
        name = "state",
        valueSpec = ValueSpec.Enumeration(
            allowedValues = listOf(
                ClipboardFilterState.ACTIVE,
                ClipboardFilterState.INACTIVE,
            ),
        ),
    ) {
        const val ACTIVE = "active"
        const val INACTIVE = "inactive"

        override fun encode(value: Boolean): String {
            return (if (value) ACTIVE else INACTIVE)
        }
    }
}
