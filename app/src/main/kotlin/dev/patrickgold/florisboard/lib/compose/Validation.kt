/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.lib.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.lib.ValidationResult

@Composable
fun Validation(
    showValidationErrors: Boolean,
    validationResult: ValidationResult?,
) {
    if (showValidationErrors) {
        if (validationResult is ValidationResult.Valid && validationResult.hasHintMessage()) {
            Text(
                text = validationResult.hintMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f),
            )
        }
        if (validationResult is ValidationResult.Invalid && validationResult.hasErrorMessage()) {
            Text(
                text = validationResult.errorMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
