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

package dev.patrickgold.florisboard.lib.android

typealias AndroidClipboardManager = android.content.ClipboardManager
// TODO: remove this once https://youtrack.jetbrains.com/issue/KT-34281 is fixed
typealias AndroidClipboardManager_OnPrimaryClipChangedListener = android.content.ClipboardManager.OnPrimaryClipChangedListener

fun AndroidClipboardManager.clearPrimaryClipAnyApi() {
    if (AndroidVersion.ATLEAST_API28_P) {
        this.clearPrimaryClip()
    } else {
        this.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
    }
}

fun AndroidClipboardManager.setOrClearPrimaryClip(clip: android.content.ClipData?) {
    if (clip != null) {
        this.setPrimaryClip(clip)
    } else {
        this.clearPrimaryClipAnyApi()
    }
}
