/*
 * Copyright (C) 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "dictionary/structure/v4/content/dynamic_language_model_probability_utils.h"

namespace latinime {

// Used to provide stable probabilities even if the user's input count is small.
const int DynamicLanguageModelProbabilityUtils::ASSUMED_MIN_COUNTS[] = {8192, 2, 2, 1};

// Encoded backoff weights.
// Note that we give positive values for trigrams and quadgrams that means the weight is more than
// 1.
// TODO: Apply backoff for main dictionaries and quit giving a positive backoff weight.
const int DynamicLanguageModelProbabilityUtils::ENCODED_BACKOFF_WEIGHTS[] = {-32, -4, 2, 8};

// This value is used to remove too old entries from the dictionary.
const int DynamicLanguageModelProbabilityUtils::DURATION_TO_DISCARD_ENTRY_IN_SECONDS =
        300 * 24 * 60 * 60; // 300 days

} // namespace latinime
