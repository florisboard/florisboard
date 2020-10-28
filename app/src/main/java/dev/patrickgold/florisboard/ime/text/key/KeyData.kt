/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.key

/**
 * Data class which describes a single key and its variants.
 *
 * @property code The UTF-8 encoded code of the character. The code defined here is used as the
 *  data passed to the system.
 * @property label The string used to display the key in the UI. Is not used for the actual data
 *  passed to the system. Should normally be the exact same as the [code]. Defaults to an empty
 *  string.
 * @property hintedNumber The hinted number which will be dynamically inserted into the long-press
 * [popup]. Leave null to disable the hinted popup for this key. The visibility of the hinted number
 *  is controlled by the preferences. Defaults to null.
 * @property hintedSymbol The hinted symbol which will be dynamically inserted into the long-press
 * [popup]. Leave null to disable the hinted popup for this key. The visibility of the hinted symbol
 *  is controlled by the preferences. Defaults to null.
 * @property popup List of keys which will be accessible while long pressing the key. Defaults to
 *  an empty list (no extended popup).
 * @property type The type of the key. Some actions require both [code] and [type] to match in order
 *  to be successfully executed. Defaults to [KeyType.CHARACTER].
 * @property variation Controls if the key should only be shown in some contexts (e.g.: url input)
 *  or if the key should always be visible. Defaults to [KeyVariation.ALL].
 */
data class KeyData(
    var code: Int,
    var label: String = "",
    var hintedNumber: KeyData? = null,
    var hintedSymbol: KeyData? = null,
    var popup: MutableList<KeyData> = mutableListOf(),
    var type: KeyType = KeyType.CHARACTER,
    var variation: KeyVariation = KeyVariation.ALL
)
