/*
 * Copyright (C) 2023 Patrick Goldinger
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

package dev.patrickgold.florisboard.plugin

import android.content.Context
import androidx.annotation.XmlRes
import org.xmlpull.v1.XmlPullParser

data class FlorisPluginMetadata(
    val id: String,
    val version: String? = null,
    val title: ValueOrRef<String>? = null,
    val description: ValueOrRef<String>? = null,
    val maintainers: ValueOrRef<List<String>>? = null,
    val license: ValueOrRef<String>? = null,
    val homepage: ValueOrRef<String>? = null,
    val issueTracker: ValueOrRef<String>? = null,
    val privacyPolicy: ValueOrRef<String>? = null,
    var spellingConfig: SpellingConfig? = null,
    var suggestionConfig: SuggestionConfig? = null,
) {
    class SpellingConfig

    class SuggestionConfig

    fun toString(packageContext: Context): String {
        if (id.isBlank()) return "FlorisPluginMetadata { invalid }"
        return """
            FlorisPluginMetadata {
                id=$id
                version=$version
                title=${title?.get(packageContext)}
                description=${description?.get(packageContext)}
                maintainers=${maintainers?.get(packageContext)}
                license=${license?.get(packageContext)}
                homepage=${homepage?.get(packageContext)}
                issueTracker=${issueTracker?.get(packageContext)}
                privacyPolicy=${privacyPolicy?.get(packageContext)}
                spellingConfig=$spellingConfig
                suggestionConfig=$suggestionConfig
            }
        """.trimIndent()
    }

    companion object {
        private const val FL_NAMESPACE_URL = "https://schemas.florisboard.org/plugin"

        fun parseFromXml(packageContext: Context, @XmlRes id: Int): FlorisPluginMetadata {
            val parser = packageContext.resources.getXml(id)
            parser.next()

            fun attrOrNull(name: String): String? {
                return try {
                    parser.getAttributeValue(FL_NAMESPACE_URL, name)?.takeIf { it.isNotBlank() }
                } catch (_: IndexOutOfBoundsException) {
                    null
                }
            }

            var pluginMetadata: FlorisPluginMetadata? = null
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG) {
                    eventType = parser.next()
                    continue
                }
                when (parser.name) {
                    "plugin" -> {
                        pluginMetadata = FlorisPluginMetadata(
                            id = attrOrNull("id") ?: throw IllegalStateException("Missing required attribute 'fl:id'"),
                            version = attrOrNull("version"),
                            title = strOrRefOf(attrOrNull("title")),
                            description = strOrRefOf(attrOrNull("description")),
                            maintainers = strListOrRefOf(attrOrNull("maintainers")),
                            license = strOrRefOf(attrOrNull("license")),
                            homepage = strOrRefOf(attrOrNull("homepage")),
                            issueTracker = strOrRefOf(attrOrNull("issueTracker")),
                            privacyPolicy = strOrRefOf(attrOrNull("privacyPolicy")),
                        )
                    }
                    "spelling" -> pluginMetadata?.spellingConfig = SpellingConfig()
                    "suggestion" -> pluginMetadata?.suggestionConfig = SuggestionConfig()
                }
                eventType = parser.next()
            }

            return pluginMetadata ?: throw IllegalStateException("Invalid XML structure")
        }
    }
}
