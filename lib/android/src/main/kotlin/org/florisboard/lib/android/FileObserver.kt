/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.android

import android.os.FileObserver
import java.io.File

/**
 * Create a new file observer for a certain file or directory. Monitoring does not start on creation! You must call
 * startWatching() before you will receive events.
 *
 * @param file The file or directory to monitor.
 * @param mask The event or events (added together) to watch for.
 * @param onEvent The event handler which gets executed for any new events.
 *
 * @see android.os.FileObserver
 */
fun FileObserver(file: File, mask: Int, onEvent: (event: Int, path: String?) -> Unit): FileObserver {
    return if (AndroidVersion.ATLEAST_API29_Q) {
        object : FileObserver(file, mask) {
            override fun onEvent(event: Int, path: String?) = onEvent(event, path)
        }
    } else {
        @Suppress("DEPRECATION")
        object : FileObserver(file.absolutePath, mask) {
            override fun onEvent(event: Int, path: String?) = onEvent(event, path)
        }
    }
}
