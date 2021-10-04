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

package dev.patrickgold.florisboard.res.ext

import android.content.Context
import dev.patrickgold.florisboard.appContext
import dev.patrickgold.florisboard.assetManager
import dev.patrickgold.florisboard.res.FlorisRef

class ExtensionManager(context: Context) {
    private val appContext by context.appContext()
    private val assetManager by context.assetManager()

    private val extensions: MutableMap<FlorisRef, Extension> = mutableMapOf()

    init {
        indexExtensions()
    }

    private fun indexExtensions() {

    }
}
