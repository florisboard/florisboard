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

private const val FallbackValue = "(unspecified)"

data class FlorisPluginMetadata(
    val id: String = FallbackValue,
    val version: String = FallbackValue,
    val title: ValueOrRef<String> = ValueOrRef.value(FallbackValue),
    val shortDescription: ValueOrRef<String>? = null,
    val longDescription: ValueOrRef<String>? = null,
    val maintainers: ValueOrRef<List<String>>? = null,
    val homepage: ValueOrRef<String>? = null,
    val issueTracker: ValueOrRef<String>? = null,
    val privacyPolicy: ValueOrRef<String>? = null,
    val license: ValueOrRef<String>? = null,
    val settingsActivity: String? = null,
    var spellingConfig: FlorisPluginFeature.SpellingConfig? = null,
    var suggestionConfig: FlorisPluginFeature.SuggestionConfig? = null,
) {
    fun features(): List<FlorisPluginFeature> {
        return buildList {
            spellingConfig?.let { add(it) }
            suggestionConfig?.let { add(it) }
        }
    }

    fun toString(packageContext: Context): String {
        if (id.isBlank()) return "FlorisPluginMetadata { invalid }"
        return """
            FlorisPluginMetadata {
                id=$id
                version=$version
                title=${title.get(packageContext)}
                shortDescription=${shortDescription?.get(packageContext)}
                longDescription=${longDescription?.get(packageContext)}
                maintainers=${maintainers?.get(packageContext)}
                homepage=${homepage?.get(packageContext)}
                issueTracker=${issueTracker?.get(packageContext)}
                privacyPolicy=${privacyPolicy?.get(packageContext)}
                license=${license?.get(packageContext)}
                settingsActivity=$settingsActivity
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

            fun attr(name: String): String {
                return attrOrNull(name).takeIf { !it.isNullOrBlank() }
                    ?: throw IllegalStateException("Missing required attribute 'fl:$name'")
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
                            id = attr("id"),
                            version = attr("version"),
                            title = strOrRefOf(attr("title"))!!,
                            shortDescription = strOrRefOf(attrOrNull("shortDescription")),
                            longDescription = strOrRefOf(attrOrNull("longDescription")),
                            maintainers = strListOrRefOf(attrOrNull("maintainers")),
                            homepage = strOrRefOf(attrOrNull("homepage")),
                            issueTracker = strOrRefOf(attrOrNull("issueTracker")),
                            privacyPolicy = strOrRefOf(attrOrNull("privacyPolicy")),
                            license = strOrRefOf(attrOrNull("license")),
                            settingsActivity = attrOrNull("settingsActivity"),
                        )
                    }
                    "spelling" -> pluginMetadata?.spellingConfig = FlorisPluginFeature.SpellingConfig
                    "suggestion" -> pluginMetadata?.suggestionConfig = FlorisPluginFeature.SuggestionConfig
                }
                eventType = parser.next()
            }

            return pluginMetadata ?: throw IllegalStateException("Invalid XML structure")
        }
    }
}

sealed interface FlorisPluginFeature {
    object SpellingConfig : FlorisPluginFeature
    object SuggestionConfig : FlorisPluginFeature
}
