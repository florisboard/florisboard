/*
 * Copyright (C) 2022 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.ext

import androidx.core.text.trimmedLength
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.lib.ValidationRule
import dev.patrickgold.florisboard.lib.snygg.SnyggStylesheet
import dev.patrickgold.florisboard.lib.snygg.value.SnyggDpShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggPercentShapeValue
import dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.validate

// TODO: (priority=medium)
//  make all strings available for localize
object ExtensionValidation {
    private val MetaIdRegex = """^[a-z][a-z0-9_]*(\.[a-z0-9][a-z0-9_]*)*${'$'}""".toRegex()
    private val ComponentIdRegex = """^[a-z][a-z0-9_]*${'$'}""".toRegex()
    private val ThemeComponentStylesheetPathRegex = """^[^:*<>"']*${'$'}""".toRegex()
    val ThemeComponentVariableNameRegex = """^[a-zA-Z0-9-_]+${'$'}""".toRegex()

    val MetaId = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "id"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a package name")
                MetaIdRegex.matches(str) -> resultValid()
                else -> resultInvalid("Package name does not match regex $MetaIdRegex")
            }
        }
    }

    val MetaVersion = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "version"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a version")
                else -> resultValid()
            }
        }
    }

    val MetaTitle = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "title"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a title")
                else -> resultValid()
            }
        }
    }

    val MetaMaintainers = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "maintainers"
        validator { str ->
            val maintainers = str.lines().filter { it.isNotBlank() }
            when {
                maintainers.isEmpty() -> resultInvalid(error = "Please enter at least one valid maintainer")
                else -> resultValid()
            }
        }
    }

    val MetaLicense = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "license"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a license identifier")
                else -> resultValid()
            }
        }
    }

    val ComponentId = ValidationRule<String> {
        forKlass = ExtensionComponent::class
        forProperty = "id"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a component ID")
                !ComponentIdRegex.matches(str) -> resultInvalid(error = "Please enter a component ID matching $ComponentIdRegex")
                else -> resultValid()
            }
        }
    }

    val ComponentLabel = ValidationRule<String> {
        forKlass = ExtensionComponent::class
        forProperty = "label"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a component label")
                str.trimmedLength() > 30 -> resultValid(hint = "Your component label is quite long, which may lead to clipping in the UI")
                else -> resultValid()
            }
        }
    }

    val ComponentAuthors = ValidationRule<String> {
        forKlass = ExtensionComponent::class
        forProperty = "authors"
        validator { str ->
            val authors = str.lines().filter { it.isNotBlank() }
            when {
                authors.isEmpty() -> resultInvalid(error = "Please enter at least one valid author")
                else -> resultValid()
            }
        }
    }

    val ThemeComponentStylesheetPath = ValidationRule<String> {
        forKlass = ThemeExtensionComponent::class
        forProperty = "stylesheetPath"
        validator { str ->
            when {
                str.isEmpty() -> resultValid()
                str.isBlank() -> resultInvalid(error = "The stylesheet path must not be blank")
                !ThemeComponentStylesheetPathRegex.matches(str) -> {
                    resultInvalid(error = "Please enter a valid stylesheet path matching $ThemeComponentStylesheetPathRegex")
                }
                else -> resultValid()
            }
        }
    }

    val ThemeComponentVariableName = ValidationRule<String> {
        forKlass = SnyggStylesheet::class
        forProperty = "propertyName"
        validator { input ->
            val str = input.trim()
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a variable name")
                str == "-" || str.startsWith("--") -> resultValid()
                !ThemeComponentVariableNameRegex.matches(str) -> {
                    resultInvalid(error = "Please enter a valid variable name matching $ThemeComponentVariableNameRegex")
                }
                else -> resultValid(hint = "By convention a FlorisCSS variable name starts with two dashes (--)")
            }
        }
    }

    val SnyggSolidColorValue = ValidationRule<String> {
        forKlass = SnyggSolidColorValue::class
        forProperty = "color"
        validator { input ->
            val str = input.trim()
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a color string")
                dev.patrickgold.florisboard.lib.snygg.value.SnyggSolidColorValue.deserialize(str).isFailure -> {
                    resultInvalid(error = "Please enter a valid color string")
                }
                else -> resultValid()
            }
        }
    }

    val SnyggDpShapeValue = ValidationRule<String> {
        forKlass = SnyggDpShapeValue::class
        forProperty = "corner"
        validator { str ->
            val floatValue = str.toFloatOrNull()
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a dp size")
                floatValue == null -> resultInvalid(error = "Please enter a valid number")
                floatValue < 0f -> resultInvalid(error = "Please enter a positive number (>=0)")
                else -> resultValid()
            }
        }
    }

    val SnyggPercentShapeValue = ValidationRule<String> {
        forKlass = SnyggPercentShapeValue::class
        forProperty = "corner"
        validator { str ->
            val intValue = str.toIntOrNull()
            when {
                str.isBlank() -> resultInvalid(error = "Please enter a percent size")
                intValue == null -> resultInvalid(error = "Please enter a valid number")
                intValue < 0 || intValue > 100 -> resultInvalid(error = "Please enter a positive number between 0 and 100")
                intValue > 50 -> resultValid(hint = "Any value above 50% will behave as if you set 50%, consider lowering your percent size")
                else -> resultValid()
            }
        }
    }
}

fun ExtensionMeta.validate(): Boolean {
    return with(ExtensionValidation) {
        validate(MetaId, id).isValid() &&
            validate(MetaVersion, version).isValid() &&
            validate(MetaTitle, title).isValid() &&
            validate(MetaMaintainers, maintainers.joinToString("\n")).isValid() &&
            validate(MetaLicense, license).isValid()
    }
}
