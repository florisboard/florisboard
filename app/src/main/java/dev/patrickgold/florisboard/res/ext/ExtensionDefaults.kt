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

import dev.patrickgold.florisboard.common.curlyFormat

object ExtensionDefaults {
    private const val ID_IMPORT = "local.imported.{groupName}.{extensionName}"

    const val FILE_EXTENSION = "flex"
    const val MANIFEST_FILE_NAME = "extension.json"

    fun createIdForImport(
        groupName: String,
        extensionName: String = System.currentTimeMillis().toString(),
    ) = ID_IMPORT.curlyFormat("groupName" to groupName, "extensionName" to extensionName)

    fun createFlexName(id: String) = "$id.$FILE_EXTENSION"
}
