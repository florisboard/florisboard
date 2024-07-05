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
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.theme.ThemeExtensionComponent
import dev.patrickgold.florisboard.lib.ValidationRule
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.value.SnyggDpShapeValue
import org.florisboard.lib.snygg.value.SnyggPercentShapeValue
import org.florisboard.lib.snygg.value.SnyggSolidColorValue
import dev.patrickgold.florisboard.lib.validate
import org.florisboard.lib.snygg.value.SnyggVarValue

object ExtensionValidation {
    private val MetaIdRegex = """^[a-z][a-z0-9_]*(\.[a-z0-9][a-z0-9_]*)*${'$'}""".toRegex()
    private val ComponentIdRegex = """^[a-z][a-z0-9_]*${'$'}""".toRegex()
    private val ThemeComponentStylesheetPathRegex = """^[^:*<>"']*${'$'}""".toRegex()

    val MetaId = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "id"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_package_name)
                MetaIdRegex.matches(str) -> resultValid()
                else -> resultInvalid(error = R.string.ext__validation__error_package_name, "id_regex" to MetaIdRegex)
            }
        }
    }

    val MetaVersion = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "version"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_version)
                else -> resultValid()
            }
        }
    }

    val MetaTitle = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "title"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_title)
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
                maintainers.isEmpty() -> resultInvalid(error = R.string.ext__validation__enter_maintainer)
                else -> resultValid()
            }
        }
    }

    val MetaLicense = ValidationRule<String> {
        forKlass = ExtensionMeta::class
        forProperty = "license"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_license)
                else -> resultValid()
            }
        }
    }

    val ComponentId = ValidationRule<String> {
        forKlass = ExtensionComponent::class
        forProperty = "id"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_component_id)
                !ComponentIdRegex.matches(str) -> resultInvalid(error = R.string.ext__validation__error_component_id, "component_id_regex" to ComponentIdRegex)
                else -> resultValid()
            }
        }
    }

    val ComponentLabel = ValidationRule<String> {
        forKlass = ExtensionComponent::class
        forProperty = "label"
        validator { str ->
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_component_label)
                str.trimmedLength() > 30 -> resultValid(hint = R.string.ext__validation__hint_component_label_to_long)
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
                authors.isEmpty() -> resultInvalid(error = R.string.ext__validation__error_author)
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
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__error_stylesheet_path_blank)
                !ThemeComponentStylesheetPathRegex.matches(str) -> {
                    resultInvalid(error = R.string.ext__validation__error_stylesheet_path, "stylesheet_path_regex" to ThemeComponentStylesheetPathRegex)
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
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_property)
                str == "-" || str.startsWith("--") -> resultValid()
                !SnyggVarValue.VariableNameRegex.matches(str) -> {
                    resultInvalid(error = R.string.ext__validation__error_property, "variable_name_regex" to SnyggVarValue.VariableNameRegex)
                }
                else -> resultValid(hint = R.string.ext__validation__hint_property)
            }
        }
    }

    val SnyggSolidColorValue = ValidationRule<String> {
        forKlass = SnyggSolidColorValue::class
        forProperty = "color"
        validator { input ->
            val str = input.trim()
            when {
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_color)
                org.florisboard.lib.snygg.value.SnyggSolidColorValue.deserialize(str).isFailure -> {
                    resultInvalid(error = R.string.ext__validation__error_color)
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
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_dp_size)
                floatValue == null -> resultInvalid(error = R.string.ext__validation__enter_valid_number)
                floatValue < 0f -> resultInvalid(error = R.string.ext__validation__enter_positive_number)
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
                str.isBlank() -> resultInvalid(error = R.string.ext__validation__enter_percent_size)
                intValue == null -> resultInvalid(error = R.string.ext__validation__enter_valid_number)
                intValue < 0 || intValue > 100 -> resultInvalid(error = R.string.ext__validation__enter_number_between_0_100)
                intValue > 50 -> resultValid(hint = R.string.ext__validation__hint_value_above_50_percent)
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
