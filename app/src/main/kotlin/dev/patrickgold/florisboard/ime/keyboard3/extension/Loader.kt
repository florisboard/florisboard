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

package dev.patrickgold.florisboard.ime.keyboard3.extension

import android.net.Uri
import dev.patrickgold.florisboard.ime.io.FlorisRef
import dev.patrickgold.florisboard.ime.io.Storage
import dev.patrickgold.florisboard.ime.io.readText
import dev.patrickgold.florisboard.ime.keyboard3.ImeController
import dev.patrickgold.florisboard.lib.devtools.flogError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.k3lp.K3ImportResolver
import org.k3lp.K3lp
import org.k3lp.K3lpResult
import org.k3lp.lib.meta.source.SourceFileRef
import org.k3lp.lib.meta.source.TextSourceFile
import org.k3lp.lib.text.K3Descriptor
import org.k3lp.lib.text.WillRequireMigrationToRichErrors
import org.k3lp.model.K3ImpliedImports

private val SCOPE_FOUNDATION = K3Descriptor("fl", "ext", "org.florisboard.k3.foundation")

// TODO this is just a placeholder testing code, must be replaced by proper logic & extension-aware logic
@OptIn(WillRequireMigrationToRichErrors::class)
suspend fun loadFoundationKeyboard(imeController: ImeController, storage: Storage) {
    suspend fun loadAssetFile(path: String): TextSourceFile {
        val xml = withContext(Dispatchers.IO) {
            val ref = FlorisRef.assets("extensions/org.florisboard.k3.foundation/$path")
            storage.readText(ref)
        }
        val sourceFile = TextSourceFile(object : SourceFileRef {
            override fun toString(): String {
                return "toString()"
            }
        }, xml)
        return sourceFile
    }
    val importResolver = K3ImportResolver { path, _ ->
        val relPath = "import/" + runCatching { Uri.parse(path).path?.removePrefix("/") ?: "" }.getOrDefault(path)
        loadAssetFile(relPath)
    }
    val impliedImports = K3ImpliedImports(
        displays = listOf("flex://org.florisboard.k3.foundation/displays-implied.xml"),
        keys = listOf("flex://org.florisboard.k3.foundation/keys-implied.xml"),
    )
    val result = K3lp.compile(loadAssetFile("keyboard/qwertz.xml"), importResolver, impliedImports)
    for (report in result.reports) {
        flogError { report.cause?.stackTraceToString() ?: "" }
    }
    if (result is K3lpResult.Success) {
        imeController.updateState {
            switchModel(result.data)
        }
    }
}
