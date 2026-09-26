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

package dev.patrickgold.florisboard.ime.extension

import android.net.Uri
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.ime.io.Storage
import org.k3lp.lib.meta.report.Report
import org.k3lp.lib.meta.report.ReportLevel

class ExtensionIndex(
    val storage: Storage,
    val extensions: Map<String, Extension<*>> = emptyMap(),
    val reports: List<Report>,
) {
    val isValid: Boolean = reports.none { it.level == ReportLevel.ERROR }

    /**
     * Generates an update url for [Extension] lists.
     *
     * @param version the version of the api path
     * @param host the host for the addons store
     * @return the Url
     */
    internal fun generateUpdateUrl(
        version: String = BuildConfig.FLADDONS_API_VERSION,
        host: String = BuildConfig.FLADDONS_STORE_URL,
    ): String {
        return Uri.Builder().run {
            scheme("https")
            authority(host)
            appendPath("check-updates")
            // TODO: Uncomment when version is supported by the addons store api
            //appendPath(version)
            encodedFragment(
                buildString {
                    append("data={")
                    for ((index, extension) in extensions.values.withIndex()) {
                        if (index > 0) {
                            append(",")
                        }
                        append(extension.manifest.meta.getUpdateJsonPair())
                    }
                    append("}")
                }
            )
        }.build().toString()
    }
}
