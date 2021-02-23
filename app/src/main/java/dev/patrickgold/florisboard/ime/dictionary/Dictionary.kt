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

package dev.patrickgold.florisboard.ime.dictionary

import dev.patrickgold.florisboard.ime.extension.Asset
import dev.patrickgold.florisboard.ime.nlp.LanguageModel
import dev.patrickgold.florisboard.ime.nlp.MutableLanguageModel
import dev.patrickgold.florisboard.ime.nlp.Token
import dev.patrickgold.florisboard.ime.nlp.WeightedToken

/**
 * Standardized dictionary interface for interacting with dictionaries.
 */
interface Dictionary<T : Any, F : Number> : LanguageModel<T, F>, Asset {
    fun getDate(): Long

    fun getVersion(): Int
}

interface MutableDictionary<T : Any, F : Number> : MutableLanguageModel<T, F>, Dictionary<T, F> {
    fun setDate(date: Int)

    fun setVersion(version: Int)
}
