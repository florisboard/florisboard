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
package com.google.android.apps.inputmethod.libs.mozc.session

object MozcJNI {
    init {
        System.loadLibrary("mozc")
    }

    @JvmStatic
    external fun initialize(): Boolean

    external fun evalCommand(commandBytes: ByteArray): ByteArray?

    external fun onPostLoad(
        userProfileDirectoryPath: String,
        dataFilePath: String
    ): Boolean

    val dataVersion: String?
        external get
}
