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

package dev.patrickgold.florisboard.ime.theme

import dev.patrickgold.florisboard.ime.clipboard.provider.ItemType
import dev.patrickgold.florisboard.ime.keyboard3.touch.InputShiftState
import dev.patrickgold.florisboard.ime.keyboard3.ui.asAttrValue
import dev.patrickgold.florisboard.ime.window.ImeWindowMode
import org.k3lp.lib.text.K3StringOrDescriptor
import org.k3lp.model.layer.K3LayerId

sealed interface ImeUiAttrs {
    data object Output : ImeUiAttr.AnyString<K3StringOrDescriptor>(
        name = "output",
    ), ImeUiAttrs {
        override fun encode(value: K3StringOrDescriptor): String {
            return value.asAttrValue()
        }
    }

    data object TargetLayer : ImeUiAttr.Custom<K3LayerId>(
        name = "targetlayer",
        predicate = { K3LayerId(it).isValid() },
    ), ImeUiAttrs {
        override fun encode(value: K3LayerId): String {
            return value.value
        }
    }

    data object Layer : ImeUiAttr.Custom<K3LayerId>(
        name = "layer",
        predicate = { K3LayerId(it).isValid() },
    ), ImeUiAttrs {
        override fun encode(value: K3LayerId): String {
            return value.value
        }
    }

    data object ShiftState : ImeUiAttr.Enumeration<InputShiftState>(
        name = "shiftstate",
        allowedValues = InputShiftState.entries.map { it.toString() },
    ), ImeUiAttrs {
        override fun encode(value: InputShiftState): String {
            return value.toString()
        }
    }

    data object WindowMode : ImeUiAttr.Enumeration<ImeWindowMode>(
        name = "windowmode",
        allowedValues = ImeWindowMode.entries.map { it.toString() },
    ), ImeUiAttrs {
        override fun encode(value: ImeWindowMode): String {
            return value.toString()
        }
    }

    data object ClipboardDialogAction : ImeUiAttr.Enumeration<Boolean>(
        name = "action",
        allowedValues = listOf(
            ClipboardDialogAction.YES,
            ClipboardDialogAction.NO,
        ),
    ), ImeUiAttrs {
        const val YES = "yes"
        const val NO = "no"

        override fun encode(value: Boolean): String {
            return (if (value) YES else NO)
        }
    }

    data object ClipboardItemType : ImeUiAttr.Enumeration<ItemType>(
        name = "type",
        allowedValues = ItemType.entries.map { it.name.lowercase() },
    ), ImeUiAttrs {
        override fun encode(value: ItemType): String {
            return value.name.lowercase()
        }
    }

    data object ClipboardFilterState : ImeUiAttr.Enumeration<Boolean>(
        name = "state",
        allowedValues = listOf(
            ClipboardFilterState.ACTIVE,
            ClipboardFilterState.INACTIVE,
        ),
    ), ImeUiAttrs {
        const val ACTIVE = "active"
        const val INACTIVE = "inactive"

        override fun encode(value: Boolean): String {
            return (if (value) ACTIVE else INACTIVE)
        }
    }
}
