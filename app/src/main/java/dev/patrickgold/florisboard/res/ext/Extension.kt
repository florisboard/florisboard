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

import java.io.Closeable
import java.io.File

/**
 * An extension container holding a parsed config, a working directory file
 * object as well as a reference to the original flex file.
 *
 * @property config The parsed config of this extension.
 * @property workingDir The working directory, used as a cache and as a staging
 *  area for modifications to extension files.
 * @property flexFile Optional, defines where the original flex file is stored.
 */
open class Extension<C : ExtensionConfig>(
    val config: C,
    val workingDir: File,
    val flexFile: File?
) : Closeable {

    /**
     * Closes the extension and deletes the temporary files. After invoking this
     * method, this object and its cache files must never be touched again.
     */
    override fun close() {
        workingDir.deleteRecursively()
    }
}
