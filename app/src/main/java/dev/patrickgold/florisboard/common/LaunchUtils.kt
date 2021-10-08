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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.debug.flogError
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

private const val URL_HTTP_PREFIX = "http://"
private const val URL_HTTPS_PREFIX = "https://"
private const val URL_MAILTO_PREFIX = "mailto:"

fun launchUrl(context: Context, url: String) {
    val link = when {
        url.startsWith(URL_HTTP_PREFIX) ||
            url.startsWith(URL_HTTPS_PREFIX) ||
            url.startsWith(URL_MAILTO_PREFIX) -> url
        else -> "$URL_HTTPS_PREFIX$url"
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        flogError { e.toString() }
        Toast.makeText(
            context,
            context.getString(R.string.general__no_browser_app_found_for_url).curlyFormat("url" to url),
            Toast.LENGTH_LONG,
        ).show()
    }
}

fun launchUrl(context: Context, @StringRes url: Int) {
    launchUrl(context, context.getString(url))
}

fun launchUrl(context: Context, @StringRes url: Int, vararg args: CurlyArg) {
    launchUrl(context, context.getString(url).curlyFormat(*args))
}

inline fun <T : Any> launchActivity(context: Context, kClass: KClass<T>, intentModifier: (Intent) -> Unit = { }) {
    contract {
        callsInPlace(intentModifier, InvocationKind.EXACTLY_ONCE)
    }
    try {
        val intent = Intent(context, kClass.java)
        intentModifier(intent)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        flogError { e.toString() }
        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
    }
}

inline fun launchActivity(context: Context, intentModifier: (Intent) -> Unit) {
    contract {
        callsInPlace(intentModifier, InvocationKind.EXACTLY_ONCE)
    }
    try {
        val intent = Intent()
        intentModifier(intent)
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        flogError { e.toString() }
        Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
    }
}
