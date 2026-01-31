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

#ifndef LATINIME_SCORING_PARAMS_H
#define LATINIME_SCORING_PARAMS_H

#include "defines.h"

namespace latinime {

class ScoringParams {
 public:
    // Fixed model parameters
    static const float MAX_SPATIAL_DISTANCE;
    static const int THRESHOLD_NEXT_WORD_PROBABILITY;
    static const int THRESHOLD_NEXT_WORD_PROBABILITY_FOR_CAPPED;
    static const float AUTOCORRECT_OUTPUT_THRESHOLD;
    static const int MAX_CACHE_DIC_NODE_SIZE;
    static const int MAX_CACHE_DIC_NODE_SIZE_FOR_SINGLE_POINT;
    static const int MAX_CACHE_DIC_NODE_SIZE_FOR_LOW_PROBABILITY_LOCALE;
    static const int THRESHOLD_SHORT_WORD_LENGTH;

    static const float EXACT_MATCH_PROMOTION;
    static const float PERFECT_MATCH_PROMOTION;
    static const float CASE_ERROR_PENALTY_FOR_EXACT_MATCH;
    static const float ACCENT_ERROR_PENALTY_FOR_EXACT_MATCH;
    static const float DIGRAPH_PENALTY_FOR_EXACT_MATCH;

    // Numerically optimized parameters (currently for tap typing only).
    // TODO: add ability to modify these constants programmatically.
    // TODO: explore optimization of gesture parameters.
    static const float DISTANCE_WEIGHT_LENGTH;
    static const float PROXIMITY_COST;
    static const float FIRST_CHAR_PROXIMITY_COST;
    static const float FIRST_PROXIMITY_COST;
    static const float INTENTIONAL_OMISSION_COST;
    static const float OMISSION_COST;
    static const float OMISSION_COST_SAME_CHAR;
    static const float OMISSION_COST_FIRST_CHAR;
    static const float INSERTION_COST;
    static const float TERMINAL_INSERTION_COST;
    static const float INSERTION_COST_SAME_CHAR;
    static const float INSERTION_COST_PROXIMITY_CHAR;
    static const float INSERTION_COST_FIRST_CHAR;
    static const float TRANSPOSITION_COST;
    static const float SPACE_SUBSTITUTION_COST;
    static const float SPACE_OMISSION_COST;
    static const float ADDITIONAL_PROXIMITY_COST;
    static const float SUBSTITUTION_COST;
    static const float COST_SECOND_OR_LATER_WORD_FIRST_CHAR_UPPERCASE;
    static const float DISTANCE_WEIGHT_LANGUAGE;
    static const float COST_FIRST_COMPLETION;
    static const float COST_COMPLETION;
    static const float HAS_PROXIMITY_TERMINAL_COST;
    static const float HAS_EDIT_CORRECTION_TERMINAL_COST;
    static const float HAS_MULTI_WORD_TERMINAL_COST;
    static const float TYPING_BASE_OUTPUT_SCORE;
    static const float TYPING_MAX_OUTPUT_SCORE_PER_INPUT;
    static const float NORMALIZED_SPATIAL_DISTANCE_THRESHOLD_FOR_EDIT;
    static const float LOCALE_WEIGHT_THRESHOLD_FOR_SPACE_SUBSTITUTION;
    static const float LOCALE_WEIGHT_THRESHOLD_FOR_SPACE_OMISSION;
    static const float LOCALE_WEIGHT_THRESHOLD_FOR_SMALL_CACHE_SIZE;

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(ScoringParams);
};
} // namespace latinime
#endif // LATINIME_SCORING_PARAMS_H
