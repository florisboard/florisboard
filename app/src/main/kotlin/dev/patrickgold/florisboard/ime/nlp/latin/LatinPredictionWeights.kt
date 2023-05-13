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

package dev.patrickgold.florisboard.ime.nlp.latin

import kotlinx.serialization.Serializable

@Serializable
data class LatinPredictionLookupWeights(
    val maxCostSum: Double,
    val costIsEqual: Double,
    val costIsEqualIgnoringCase: Double,
    val costInsert: Double,
    val costInsertStartOfStr: Double,
    val costDelete: Double,
    val costDeleteStartOfStr: Double,
    val costSubstitute: Double,
    val costSubstituteInProximity: Double,
    val costSubstituteStartOfStr: Double,
    val costTranspose: Double,
)

@Serializable
data class LatinPredictionTrainingWeights(
    val usageBonus: Int,
    val usageReductionOthers: Int,
)

@Serializable
data class LatinPredictionWeights(
    val lookup: LatinPredictionLookupWeights,
    val training: LatinPredictionTrainingWeights,
)
