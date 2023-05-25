/*
 * Copyright (C) 2023 Patrick Goldinger
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

package dev.patrickgold.florisboard.plugin

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class ValueOrRef<V>(val refRetriever: (Resources, Int) -> V) {
    class Value<V>(val value: V, refRetriever: (Resources, Int) -> V) : ValueOrRef<V>(refRetriever)
    class Ref<V>(val id: Int, refRetriever: (Resources, Int) -> V) : ValueOrRef<V>(refRetriever)

    fun get(packageContext: Context): V {
        return when (this) {
            is Value -> value
            is Ref -> refRetriever(packageContext.resources, id)
        }
    }

    fun getOrNull(packageContext: Context): V? {
        return try {
            get(packageContext)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    override fun toString(): String {
        return when (this) {
            is Value -> "ValueOrRef.Value { value=$value }"
            is Ref -> "ValueOrRef.Ref { id=$id }"
        }
    }

    companion object {
        inline fun <reified V> value(value: V) = Value(value, getRetriever())

        inline fun <reified V> ref(id: Int) = Ref<V>(id, getRetriever())

        inline fun <reified V> getRetriever(): (Resources, Int) -> V {
            return when (V::class) {
                String::class -> { resources, id -> resources.getString(id) as V }
                List::class -> { resources, id -> resources.getStringArray(id).toList() as V }
                else -> throw IllegalArgumentException("Unsupported type: ${V::class}")
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun strOrRefOf(rawValue: String?): ValueOrRef<String>? {
    contract {
        returnsNotNull() implies (rawValue != null)
    }

    if (rawValue == null) return null
    return if (rawValue.startsWith("@")) {
        val id = rawValue.substring(1).toInt()
        ValueOrRef.ref(id)
    } else {
        ValueOrRef.value(rawValue)
    }
}

@OptIn(ExperimentalContracts::class)
fun strListOrRefOf(rawValue: String?): ValueOrRef<List<String>>? {
    contract {
        returnsNotNull() implies (rawValue != null)
    }

    if (rawValue == null) return null
    return if (rawValue.startsWith("@")) {
        val id = rawValue.substring(1).toInt()
        ValueOrRef.ref(id)
    } else {
        ValueOrRef.value(rawValue.split(";").map { it.trim() }.filter { it.isNotBlank() })
    }
}
