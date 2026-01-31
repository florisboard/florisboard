/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include "suggest/policyimpl/typing/scoring_params.h"

namespace latinime {
// TODO: RENAME all
const float ScoringParams::MAX_SPATIAL_DISTANCE = 1.0f;
const int ScoringParams::THRESHOLD_NEXT_WORD_PROBABILITY = 40;
const int ScoringParams::THRESHOLD_NEXT_WORD_PROBABILITY_FOR_CAPPED = 120;
const float ScoringParams::AUTOCORRECT_OUTPUT_THRESHOLD = 1.0f;

const float ScoringParams::EXACT_MATCH_PROMOTION = 1.1f;
const float ScoringParams::PERFECT_MATCH_PROMOTION = 1.1f;
const float ScoringParams::CASE_ERROR_PENALTY_FOR_EXACT_MATCH = 0.01f;
const float ScoringParams::ACCENT_ERROR_PENALTY_FOR_EXACT_MATCH = 0.02f;
const float ScoringParams::DIGRAPH_PENALTY_FOR_EXACT_MATCH = 0.03f;

// TODO: Unlimit max cache dic node size
const int ScoringParams::MAX_CACHE_DIC_NODE_SIZE = 170;
const int ScoringParams::MAX_CACHE_DIC_NODE_SIZE_FOR_SINGLE_POINT = 310;
const int ScoringParams::MAX_CACHE_DIC_NODE_SIZE_FOR_LOW_PROBABILITY_LOCALE = 50;
const int ScoringParams::THRESHOLD_SHORT_WORD_LENGTH = 4;

const float ScoringParams::DISTANCE_WEIGHT_LENGTH = 0.1524f;
const float ScoringParams::PROXIMITY_COST = 0.0694f;
const float ScoringParams::FIRST_CHAR_PROXIMITY_COST = 0.072f;
const float ScoringParams::FIRST_PROXIMITY_COST = 0.07788f;
const float ScoringParams::INTENTIONAL_OMISSION_COST = 0.1f;
const float ScoringParams::OMISSION_COST = 0.467f;
const float ScoringParams::OMISSION_COST_SAME_CHAR = 0.345f;
const float ScoringParams::OMISSION_COST_FIRST_CHAR = 0.5256f;
const float ScoringParams::INSERTION_COST = 0.7248f;
const float ScoringParams::TERMINAL_INSERTION_COST = 0.8128f;
const float ScoringParams::INSERTION_COST_SAME_CHAR = 0.5508f;
const float ScoringParams::INSERTION_COST_PROXIMITY_CHAR = 0.674f;
const float ScoringParams::INSERTION_COST_FIRST_CHAR = 0.639f;
const float ScoringParams::TRANSPOSITION_COST = 0.5608f;
const float ScoringParams::SPACE_SUBSTITUTION_COST = 0.33f;
const float ScoringParams::SPACE_OMISSION_COST = 0.1f;
const float ScoringParams::ADDITIONAL_PROXIMITY_COST = 0.37972f;
const float ScoringParams::SUBSTITUTION_COST = 0.3806f;
const float ScoringParams::COST_SECOND_OR_LATER_WORD_FIRST_CHAR_UPPERCASE = 0.3224f;
const float ScoringParams::DISTANCE_WEIGHT_LANGUAGE = 1.1214f;
const float ScoringParams::COST_FIRST_COMPLETION = 0.4836f;
const float ScoringParams::COST_COMPLETION = 0.00624f;
const float ScoringParams::HAS_PROXIMITY_TERMINAL_COST = 0.0683f;
const float ScoringParams::HAS_EDIT_CORRECTION_TERMINAL_COST = 0.0362f;
const float ScoringParams::HAS_MULTI_WORD_TERMINAL_COST = 0.3482f;
const float ScoringParams::TYPING_BASE_OUTPUT_SCORE = 1.0f;
const float ScoringParams::TYPING_MAX_OUTPUT_SCORE_PER_INPUT = 0.1f;
const float ScoringParams::NORMALIZED_SPATIAL_DISTANCE_THRESHOLD_FOR_EDIT = 0.095f;
const float ScoringParams::LOCALE_WEIGHT_THRESHOLD_FOR_SPACE_SUBSTITUTION = 0.99f;
const float ScoringParams::LOCALE_WEIGHT_THRESHOLD_FOR_SPACE_OMISSION = 0.99f;
const float ScoringParams::LOCALE_WEIGHT_THRESHOLD_FOR_SMALL_CACHE_SIZE = 0.99f;
} // namespace latinime
