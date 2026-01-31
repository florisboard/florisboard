/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "utils/autocorrection_threshold_utils.h"

#include <algorithm>
#include <cmath>

#include "defines.h"
#include "suggest/policyimpl/utils/edit_distance.h"
#include "suggest/policyimpl/utils/damerau_levenshtein_edit_distance_policy.h"

namespace latinime {

const int AutocorrectionThresholdUtils::MAX_INITIAL_SCORE = 255;
const int AutocorrectionThresholdUtils::TYPED_LETTER_MULTIPLIER = 2;
const int AutocorrectionThresholdUtils::FULL_WORD_MULTIPLIER = 2;

/* static */ int AutocorrectionThresholdUtils::editDistance(const int *before,
        const int beforeLength, const int *after, const int afterLength) {
    const DamerauLevenshteinEditDistancePolicy daemaruLevenshtein(
            before, beforeLength, after, afterLength);
    return static_cast<int>(EditDistance::getEditDistance(&daemaruLevenshtein));
}

// In dictionary.cpp, getSuggestion() method,
// When USE_SUGGEST_INTERFACE_FOR_TYPING is true:
//
//   // TODO: Revise the following logic thoroughly by referring to the logic
//   // marked as "Otherwise" below.
//   SUGGEST_INTERFACE_OUTPUT_SCALE was multiplied to the original suggestion scores to convert
//   them to integers.
//     score = (int)((original score) * SUGGEST_INTERFACE_OUTPUT_SCALE)
//   Undo the scaling here to recover the original score.
//     normalizedScore = ((float)score) / SUGGEST_INTERFACE_OUTPUT_SCALE
//
// Otherwise: suggestion scores are computed using the below formula.
// original score
//  := powf(mTypedLetterMultiplier (this is defined 2),
//         (the number of matched characters between typed word and suggested word))
//     * (individual word's score which defined in the unigram dictionary,
//         and this score is defined in range [0, 255].)
// Then, the following processing is applied.
//     - If the dictionary word is matched up to the point of the user entry
//       (full match up to min(before.length(), after.length())
//       => Then multiply by FULL_MATCHED_WORDS_PROMOTION_RATE (this is defined 1.2)
//     - If the word is a true full match except for differences in accents or
//       capitalization, then treat it as if the score was 255.
//     - If before.length() == after.length()
//       => multiply by mFullWordMultiplier (this is defined 2))
// So, maximum original score is powf(2, min(before.length(), after.length())) * 255 * 2 * 1.2
// For historical reasons we ignore the 1.2 modifier (because the measure for a good
// autocorrection threshold was done at a time when it didn't exist). This doesn't change
// the result.
// So, we can normalize original score by dividing powf(2, min(b.l(),a.l())) * 255 * 2.

/* static */ float AutocorrectionThresholdUtils::calcNormalizedScore(const int *before,
        const int beforeLength, const int *after, const int afterLength, const int score) {
    if (0 == beforeLength || 0 == afterLength) {
        return 0.0f;
    }
    const int distance = editDistance(before, beforeLength, after, afterLength);
    int spaceCount = 0;
    for (int i = 0; i < afterLength; ++i) {
        if (after[i] == KEYCODE_SPACE) {
            ++spaceCount;
        }
    }

    if (spaceCount == afterLength) {
        return 0.0f;
    }

    if (score <= 0 || distance >= afterLength) {
        // normalizedScore must be 0.0f (the minimum value) if the score is less than or equal to 0,
        // or if the edit distance is larger than or equal to afterLength.
        return 0.0f;
    }
    // add a weight based on edit distance.
    const float weight = 1.0f - static_cast<float>(distance) / static_cast<float>(afterLength);

    // TODO: Revise the following logic thoroughly by referring to...
    if (true /* USE_SUGGEST_INTERFACE_FOR_TYPING */) {
        return (static_cast<float>(score) / SUGGEST_INTERFACE_OUTPUT_SCALE) * weight;
    }
    // ...this logic.
    const float maxScore = score >= S_INT_MAX ? static_cast<float>(S_INT_MAX)
            : static_cast<float>(MAX_INITIAL_SCORE)
                    * powf(static_cast<float>(TYPED_LETTER_MULTIPLIER),
                            static_cast<float>(std::min(beforeLength, afterLength - spaceCount)))
                    * static_cast<float>(FULL_WORD_MULTIPLIER);

    return (static_cast<float>(score) / maxScore) * weight;
}

} // namespace latinime
