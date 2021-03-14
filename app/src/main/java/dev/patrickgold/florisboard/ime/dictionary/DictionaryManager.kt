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

import android.content.Context
import dev.patrickgold.florisboard.ime.extension.AssetRef
import timber.log.Timber

/**
 * TODO: document
 */
class DictionaryManager private constructor(private val applicationContext: Context) {
    private val dictionaryCache: MutableMap<String, Dictionary<String, Int>> = mutableMapOf()

    companion object {
        private var defaultInstance: DictionaryManager? = null

        fun init(applicationContext: Context): DictionaryManager {
            val instance = DictionaryManager(applicationContext)
            defaultInstance = instance
            return instance
        }

        fun default(): DictionaryManager {
            val instance = defaultInstance
            if (instance != null) {
                return instance
            } else {
                throw UninitializedPropertyAccessException(
                    "${DictionaryManager::class.simpleName} has not been initialized previously. Make sure to call init(applicationContext) before using default()."
                )
            }
        }
    }

    fun loadDictionary(ref: AssetRef): Result<Dictionary<String, Int>> {
        dictionaryCache[ref.toString()]?.let {
            return Result.success(it)
        }
        if (ref.path.endsWith(".flict")) {
            // Assume this is a Flictionary
            Flictionary.load(applicationContext, ref).onSuccess { flict ->
                dictionaryCache[ref.toString()] = flict
                return Result.success(flict)
            }.onFailure { err ->
                Timber.i(err)
                return Result.failure(err)
            }
        } else {
            return Result.failure(Exception("Unable to determine supported type for given AssetRef!"))
        }
        return Result.failure(Exception("If this message is ever thrown, something is completely broken..."))
    }
}
