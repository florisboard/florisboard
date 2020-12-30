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

package dev.patrickgold.florisboard.ime.popup

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.patrickgold.florisboard.ime.extension.Asset
import dev.patrickgold.florisboard.ime.text.key.KeyTypeAdapter
import dev.patrickgold.florisboard.ime.text.key.KeyVariationAdapter

class PopupMappingAsset(
    type: String,
    name: String,
    authors: List<String>,
    val mapping: PopupMapping
) : Asset(type, name, authors) {
    companion object {
        fun empty() = PopupMappingAsset("", "", listOf(), mapOf())

        fun fromJsonAssetFile(context: Context, path: String): PopupMappingAsset? {
            return try {
                val raw = context.assets.open(path).bufferedReader().use { it.readText() }
                fromJsonString(raw)
            } catch (e: Exception) {
                null
            }
        }

        fun fromJsonString(json: String): PopupMappingAsset? {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .add(KeyTypeAdapter())
                .add(KeyVariationAdapter())
                .build()
            val layoutAdapter = moshi.adapter(PopupMappingAsset::class.java)
            return layoutAdapter.fromJson(json)
        }
    }
}
