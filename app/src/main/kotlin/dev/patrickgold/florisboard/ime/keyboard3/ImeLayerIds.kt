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

package dev.patrickgold.florisboard.ime.keyboard3

import org.k3lp.model.layer.K3LayerId

object ImeLayerIds {
    val Base = K3LayerId.BASE

    val Shift = K3LayerId("shift")

    val Caps = K3LayerId("caps")

    val Numpad = K3LayerId("numpad")

    val Telpad = K3LayerId("telpad")

    val Numrow = K3LayerId("numrow")
    // Japanese specific kana layout for mozc, could rename it to something like 'base-alt' or 'base-non-latin' to make it more generic, falls back to base if not found
    // completely optional, this is just defined in the code because mozc needs it, non mozc keyboards don't need this layer
    val Kana = K3LayerId("kana")
}

// realistically, this is every Latin text layer, not every text layer
fun K3LayerId.isTextLayer(): Boolean {
    return this == ImeLayerIds.Base || this == ImeLayerIds.Shift || this == ImeLayerIds.Caps
}
