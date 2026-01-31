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

#ifndef LATINIME_TYPING_SCORING_H
#define LATINIME_TYPING_SCORING_H

#include "defines.h"
#include "suggest/core/dictionary/error_type_utils.h"
#include "suggest/core/policy/scoring.h"
#include "suggest/core/session/dic_traverse_session.h"
#include "suggest/policyimpl/typing/scoring_params.h"

namespace latinime {

class DicNode;
class DicTraverseSession;

class TypingScoring : public Scoring {
 public:
    static const TypingScoring *getInstance() { return &sInstance; }

    AK_FORCE_INLINE void getMostProbableString(const DicTraverseSession *const traverseSession,
            const float weightOfLangModelVsSpatialModel,
            SuggestionResults *const outSuggestionResults) const {}

    AK_FORCE_INLINE float getAdjustedWeightOfLangModelVsSpatialModel(
            DicTraverseSession *const traverseSession, DicNode *const terminals,
            const int size) const {
        return 1.0f;
    }

    AK_FORCE_INLINE int calculateFinalScore(const float compoundDistance, const int inputSize,
            const ErrorTypeUtils::ErrorType containedErrorTypes, const bool forceCommit,
            const bool boostExactMatches, const bool hasProbabilityZero) const {
        const float maxDistance = ScoringParams::DISTANCE_WEIGHT_LANGUAGE
                + static_cast<float>(inputSize) * ScoringParams::TYPING_MAX_OUTPUT_SCORE_PER_INPUT;
        float score = ScoringParams::TYPING_BASE_OUTPUT_SCORE - compoundDistance / maxDistance;
        if (forceCommit) {
            score += ScoringParams::AUTOCORRECT_OUTPUT_THRESHOLD;
        }
        if (hasProbabilityZero) {
            // Previously, when both legitimate 0-frequency words (such as distracters) and
            // offensive words were encoded in the same way, distracters would never show up
            // when the user blocked offensive words (the default setting, as well as the
            // setting for regression tests).
            //
            // When b/11031090 was fixed and a separate encoding was used for offensive words,
            // 0-frequency words would no longer be blocked when they were an "exact match"
            // (where case mismatches and accent mismatches would be considered an "exact
            // match"). The exact match boosting functionality meant that, for example, when
            // the user typed "mt" they would be suggested the word "Mt", although they most
            // probably meant to type "my".
            //
            // For this reason, we introduced this change, which does the following:
            // * Defines the "perfect match" as a really exact match, with no room for case or
            // accent mismatches
            // * When the target word has probability zero (as "Mt" does, because it is a
            // distracter), ONLY boost its score if it is a perfect match.
            //
            // By doing this, when the user types "mt", the word "Mt" will NOT be boosted, and
            // they will get "my". However, if the user makes an explicit effort to type "Mt",
            // we do boost the word "Mt" so that the user's input is not autocorrected to "My".
            if (boostExactMatches && ErrorTypeUtils::isPerfectMatch(containedErrorTypes)) {
                score += ScoringParams::PERFECT_MATCH_PROMOTION;
            }
        } else {
            if (boostExactMatches && ErrorTypeUtils::isExactMatch(containedErrorTypes)) {
                score += ScoringParams::EXACT_MATCH_PROMOTION;
                if ((ErrorTypeUtils::MATCH_WITH_WRONG_CASE & containedErrorTypes) != 0) {
                    score -= ScoringParams::CASE_ERROR_PENALTY_FOR_EXACT_MATCH;
                }
                if ((ErrorTypeUtils::MATCH_WITH_MISSING_ACCENT & containedErrorTypes) != 0) {
                    score -= ScoringParams::ACCENT_ERROR_PENALTY_FOR_EXACT_MATCH;
                }
                if ((ErrorTypeUtils::MATCH_WITH_DIGRAPH & containedErrorTypes) != 0) {
                    score -= ScoringParams::DIGRAPH_PENALTY_FOR_EXACT_MATCH;
                }
            }
        }
        return static_cast<int>(score * SUGGEST_INTERFACE_OUTPUT_SCALE);
    }

    AK_FORCE_INLINE float getDoubleLetterDemotionDistanceCost(
            const DicNode *const terminalDicNode) const {
        return 0.0f;
    }

    AK_FORCE_INLINE bool autoCorrectsToMultiWordSuggestionIfTop() const {
        return true;
    }

    AK_FORCE_INLINE bool sameAsTyped(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const {
        return traverseSession->getProximityInfoState(0)->sameAsTyped(
                dicNode->getOutputWordBuf(), dicNode->getNodeCodePointCount());
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(TypingScoring);
    static const TypingScoring sInstance;

    TypingScoring() {}
    ~TypingScoring() {}
};
} // namespace latinime
#endif // LATINIME_TYPING_SCORING_H
