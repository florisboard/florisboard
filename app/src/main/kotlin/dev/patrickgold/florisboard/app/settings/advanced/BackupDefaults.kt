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

package dev.patrickgold.florisboard.app.settings.advanced

import dev.patrickgold.florisboard.BuildConfig

object BackupDefaults {
    const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.file"
    const val METADATA_JSON_NAME = "backup_metadata.json"
    const val CLIPBOARD_TEXT_ITEMS_JSON_NAME = "clipboard_text_items.json"
    const val CLIPBOARD_IMAGES_JSON_NAME = "clipboard_images.json"
    const val CLIPBOARD_VIDEO_JSON_NAME = "clipboard_video.json"
    const val FILES_DIR = "files"

    const val PACKAGE_NAME_PREFIX = "dev.patrickgold.florisboard"

    const val RESTORE_MIN_VERSION_CODE = 64
    const val RESTORE_ARCHIVE_FILE_NAME = "backup.zip"
    const val RESTORE_ARCHIVE_DIR_NAME = "backup.zip.d"

    const val OLD_EXT_VERSION_CODE_CUTOFF = 119 // v0.6.0-alpha02
    const val OLD_EXT_IME_THEME_PATH = "ime/theme"

    fun defaultFileName(metadata: BackupArchiveMetadata): String {
        return "backup_${metadata.packageName}_${metadata.versionCode}_${metadata.timestamp}.zip"
    }

    enum class Destination {
        FILE_SYS,
        SHARE_INTENT;
    }
}
