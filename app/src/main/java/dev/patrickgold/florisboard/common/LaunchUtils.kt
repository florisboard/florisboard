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

package dev.patrickgold.florisboard.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

fun launchUrl(context: Context, url: String) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(url)
    )
    context.startActivity(intent)
}

fun launchUrl(context: Context, @StringRes url: Int) {
    launchUrl(context, context.getString(url))
}

fun launchUrl(context: Context, @StringRes url: Int, params: Array<out String>) {
    launchUrl(context, context.getString(url, *params))
}

inline fun <T : Any> launchActivity(context: Context, kClass: KClass<T>, intentModifier: (Intent) -> Unit = { }) {
    contract {
        callsInPlace(intentModifier, InvocationKind.EXACTLY_ONCE)
    }
    val intent = Intent(context, kClass.java)
    intentModifier(intent)
    context.startActivity(intent)
}
