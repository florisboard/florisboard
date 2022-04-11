/*
 * Copyright (C) 2022 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.lib

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.kotlin.CurlyArg
import dev.patrickgold.florisboard.lib.kotlin.curlyFormat
import kotlin.contracts.contract
import kotlin.reflect.KClass

sealed class ValidationResult {
    companion object {
        fun resultValid(): ValidationResult {
            return Valid()
        }

        fun resultValid(hint: String): ValidationResult {
            return Valid(hintMessageStr = hint)
        }

        fun resultValid(@StringRes hint: Int): ValidationResult {
            return Valid(hintMessageId = hint)
        }

        fun resultValid(@StringRes hint: Int, vararg args: CurlyArg): ValidationResult {
            return Valid(hintMessageId = hint, args = args.toList())
        }

        fun resultInvalid(error: String): ValidationResult {
            return Invalid(errorMessageStr = error)
        }

        fun resultInvalid(@StringRes error: Int): ValidationResult {
            return Invalid(errorMessageId = error)
        }

        fun resultInvalid(@StringRes error: Int, vararg args: CurlyArg): ValidationResult {
            return Invalid(errorMessageId = error, args = args.toList())
        }
    }

    data class Valid(
        @StringRes private val hintMessageId: Int? = null,
        private val hintMessageStr: String? = null,
        private val args: List<CurlyArg> = emptyList(),
    ) : ValidationResult() {

        fun hasHintMessage(): Boolean {
            return hintMessageId != null || hintMessageStr != null
        }

        @Composable
        fun hintMessage(): String {
            return when {
                hintMessageId != null -> stringRes(hintMessageId).curlyFormat(args)
                hintMessageStr != null -> hintMessageStr.curlyFormat(args)
                else -> ""
            }
        }
    }

    data class Invalid(
        @StringRes private val errorMessageId: Int? = null,
        private val errorMessageStr: String? = null,
        private val args: List<CurlyArg> = emptyList(),
    ) : ValidationResult() {

        fun hasErrorMessage(): Boolean {
            return errorMessageId != null || errorMessageStr != null
        }

        @Composable
        fun errorMessage(): String {
            return when {
                errorMessageId != null -> stringRes(errorMessageId).curlyFormat(args)
                errorMessageStr != null -> errorMessageStr.curlyFormat(args)
                else -> ""
            }
        }
    }

    fun isValid(): Boolean {
        contract {
            returns(true) implies (this@ValidationResult is Valid)
        }
        return this is Valid
    }

    fun isInvalid(): Boolean {
        contract {
            returns(true) implies (this@ValidationResult is Invalid)
        }
        return this is Invalid
    }
}

@Composable
fun <T : Any> rememberValidationResult(rule: ValidationRule<T>, value: T): ValidationResult {
    return remember(value) {
        rule.validator.invoke(ValidationResult.Companion, value)
    }
}

data class ValidationRule<T : Any>(
    val klass: KClass<*>,
    val propertyName: String,
    val validator: ValidationResult.Companion.(T) -> ValidationResult,
)

class ValidationRuleBuilder<T : Any> {
    var forKlass: KClass<*>? = null
    var forProperty: String? = null

    private var validator: (ValidationResult.Companion.(T) -> ValidationResult)? = null
    fun validator(validator: ValidationResult.Companion.(T) -> ValidationResult) {
        this.validator = validator
    }

    fun build() = ValidationRule(forKlass!!, forProperty!!, validator!!)
}

@Suppress("FunctionName")
fun <T : Any> ValidationRule(scope: ValidationRuleBuilder<T>.() -> Unit): ValidationRule<T> {
    val builder = ValidationRuleBuilder<T>()
    scope(builder)
    return builder.build()
}

fun <T : Any> validate(rule: ValidationRule<T>, value: T): ValidationResult {
    return rule.validator.invoke(ValidationResult.Companion, value)
}
