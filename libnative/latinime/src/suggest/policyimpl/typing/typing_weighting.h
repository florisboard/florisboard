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

#ifndef LATINIME_TYPING_WEIGHTING_H
#define LATINIME_TYPING_WEIGHTING_H

#include "defines.h"
#include "suggest/core/dicnode/dic_node_utils.h"
#include "suggest/core/dictionary/error_type_utils.h"
#include "suggest/core/layout/touch_position_correction_utils.h"
#include "suggest/core/policy/weighting.h"
#include "suggest/core/session/dic_traverse_session.h"
#include "suggest/policyimpl/typing/scoring_params.h"
#include "utils/char_utils.h"

namespace latinime {

class DicNode;
struct DicNode_InputStateG;
class MultiBigramMap;

class TypingWeighting : public Weighting {
 public:
    static const TypingWeighting *getInstance() { return &sInstance; }

 protected:
    float getTerminalSpatialCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const {
        float cost = 0.0f;
        if (dicNode->hasMultipleWords()) {
            cost += ScoringParams::HAS_MULTI_WORD_TERMINAL_COST;
        }
        if (dicNode->getProximityCorrectionCount() > 0) {
            cost += ScoringParams::HAS_PROXIMITY_TERMINAL_COST;
        }
        if (dicNode->getEditCorrectionCount() > 0) {
            cost += ScoringParams::HAS_EDIT_CORRECTION_TERMINAL_COST;
        }
        return cost;
    }

    float getOmissionCost(const DicNode *const parentDicNode, const DicNode *const dicNode) const {
        const bool isZeroCostOmission = parentDicNode->isZeroCostOmission();
        const bool isIntentionalOmission = parentDicNode->canBeIntentionalOmission();
        const bool sameCodePoint = dicNode->isSameNodeCodePoint(parentDicNode);
        // If the traversal omitted the first letter then the dicNode should now be on the second.
        const bool isFirstLetterOmission = dicNode->getNodeCodePointCount() == 2;
        float cost = 0.0f;
        if (isZeroCostOmission) {
            cost = 0.0f;
        } else if (isIntentionalOmission) {
            cost = ScoringParams::INTENTIONAL_OMISSION_COST;
        } else if (isFirstLetterOmission) {
            cost = ScoringParams::OMISSION_COST_FIRST_CHAR;
        } else {
            cost = sameCodePoint ? ScoringParams::OMISSION_COST_SAME_CHAR
                    : ScoringParams::OMISSION_COST;
        }
        return cost;
    }

    float getMatchedCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode, DicNode_InputStateG *inputStateG) const {
        const int pointIndex = dicNode->getInputIndex(0);
        const float normalizedSquaredLength = traverseSession->getProximityInfoState(0)
                ->getPointToKeyLength(pointIndex,
                        CharUtils::toBaseLowerCase(dicNode->getNodeCodePoint()));
        const float normalizedDistance = TouchPositionCorrectionUtils::getSweetSpotFactor(
                traverseSession->isTouchPositionCorrectionEnabled(), normalizedSquaredLength);
        const float weightedDistance = ScoringParams::DISTANCE_WEIGHT_LENGTH * normalizedDistance;

        const bool isFirstChar = pointIndex == 0;
        const bool isProximity = isProximityDicNode(traverseSession, dicNode);
        float cost = isProximity ? (isFirstChar ? ScoringParams::FIRST_CHAR_PROXIMITY_COST
                : ScoringParams::PROXIMITY_COST) : 0.0f;
        if (isProximity && dicNode->getProximityCorrectionCount() == 0) {
            cost += ScoringParams::FIRST_PROXIMITY_COST;
        }
        if (dicNode->getNodeCodePointCount() == 2) {
            // At the second character of the current word, we check if the first char is uppercase
            // and the word is a second or later word of a multiple word suggestion. We demote it
            // if so.
            const bool isSecondOrLaterWordFirstCharUppercase =
                    dicNode->hasMultipleWords() && dicNode->isFirstCharUppercase();
            if (isSecondOrLaterWordFirstCharUppercase) {
                cost += ScoringParams::COST_SECOND_OR_LATER_WORD_FIRST_CHAR_UPPERCASE;
            }
        }
        return weightedDistance + cost;
    }

    bool isProximityDicNode(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const {
        const int pointIndex = dicNode->getInputIndex(0);
        const int primaryCodePoint = CharUtils::toBaseLowerCase(
                traverseSession->getProximityInfoState(0)->getPrimaryCodePointAt(pointIndex));
        const int dicNodeChar = CharUtils::toBaseLowerCase(dicNode->getNodeCodePoint());
        return primaryCodePoint != dicNodeChar;
    }

    float getTranspositionCost(const DicTraverseSession *const traverseSession,
            const DicNode *const parentDicNode, const DicNode *const dicNode) const {
        const int16_t parentPointIndex = parentDicNode->getInputIndex(0);
        const int prevCodePoint = parentDicNode->getNodeCodePoint();
        const float distance1 = traverseSession->getProximityInfoState(0)->getPointToKeyLength(
                parentPointIndex + 1, CharUtils::toBaseLowerCase(prevCodePoint));
        const int codePoint = dicNode->getNodeCodePoint();
        const float distance2 = traverseSession->getProximityInfoState(0)->getPointToKeyLength(
                parentPointIndex, CharUtils::toBaseLowerCase(codePoint));
        const float distance = distance1 + distance2;
        const float weightedLengthDistance =
                distance * ScoringParams::DISTANCE_WEIGHT_LENGTH;
        return ScoringParams::TRANSPOSITION_COST + weightedLengthDistance;
    }

    float getInsertionCost(const DicTraverseSession *const traverseSession,
            const DicNode *const parentDicNode, const DicNode *const dicNode) const {
        const int16_t insertedPointIndex = parentDicNode->getInputIndex(0);
        const int prevCodePoint = traverseSession->getProximityInfoState(0)->getPrimaryCodePointAt(
                insertedPointIndex);
        const int currentCodePoint = dicNode->getNodeCodePoint();
        const bool sameCodePoint = prevCodePoint == currentCodePoint;
        const bool existsAdjacentProximityChars = traverseSession->getProximityInfoState(0)
                ->existsAdjacentProximityChars(insertedPointIndex);
        const float dist = traverseSession->getProximityInfoState(0)->getPointToKeyLength(
                insertedPointIndex + 1, CharUtils::toBaseLowerCase(dicNode->getNodeCodePoint()));
        const float weightedDistance = dist * ScoringParams::DISTANCE_WEIGHT_LENGTH;
        const bool singleChar = dicNode->getNodeCodePointCount() == 1;
        float cost = (singleChar ? ScoringParams::INSERTION_COST_FIRST_CHAR : 0.0f);
        if (sameCodePoint) {
            cost += ScoringParams::INSERTION_COST_SAME_CHAR;
        } else if (existsAdjacentProximityChars) {
            cost += ScoringParams::INSERTION_COST_PROXIMITY_CHAR;
        } else {
            cost += ScoringParams::INSERTION_COST;
        }
        return cost + weightedDistance;
    }

    float getSpaceOmissionCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode, DicNode_InputStateG *inputStateG) const {
        const float cost = ScoringParams::SPACE_OMISSION_COST;
        return cost * traverseSession->getMultiWordCostMultiplier();
    }

    float getNewWordBigramLanguageCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode,
            MultiBigramMap *const multiBigramMap) const {
        return DicNodeUtils::getBigramNodeImprobability(
                traverseSession->getDictionaryStructurePolicy(),
                dicNode, multiBigramMap) * ScoringParams::DISTANCE_WEIGHT_LANGUAGE;
    }

    float getCompletionCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const {
        // The auto completion starts when the input index is same as the input size
        const bool firstCompletion = dicNode->getInputIndex(0)
                == traverseSession->getInputSize();
        // TODO: Change the cost for the first completion for the gesture?
        const float cost = firstCompletion ? ScoringParams::COST_FIRST_COMPLETION
                : ScoringParams::COST_COMPLETION;
        return cost;
    }

    float getTerminalLanguageCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode, const float dicNodeLanguageImprobability) const {
        return dicNodeLanguageImprobability * ScoringParams::DISTANCE_WEIGHT_LANGUAGE;
    }

    float getTerminalInsertionCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const {
        const int inputIndex = dicNode->getInputIndex(0);
        const int inputSize = traverseSession->getInputSize();
        ASSERT(inputIndex < inputSize);
        // TODO: Implement more efficient logic
        return  ScoringParams::TERMINAL_INSERTION_COST * (inputSize - inputIndex);
    }

    AK_FORCE_INLINE bool needsToNormalizeCompoundDistance() const {
        return false;
    }

    AK_FORCE_INLINE float getAdditionalProximityCost() const {
        return ScoringParams::ADDITIONAL_PROXIMITY_COST;
    }

    AK_FORCE_INLINE float getSubstitutionCost() const {
        return ScoringParams::SUBSTITUTION_COST;
    }

    AK_FORCE_INLINE float getSpaceSubstitutionCost(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const {
        const int inputIndex = dicNode->getInputIndex(0);
        const float distanceToSpaceKey = traverseSession->getProximityInfoState(0)
                ->getPointToKeyLength(inputIndex, KEYCODE_SPACE);
        const float cost = ScoringParams::SPACE_SUBSTITUTION_COST * distanceToSpaceKey;
        return cost * traverseSession->getMultiWordCostMultiplier();
    }

    ErrorTypeUtils::ErrorType getErrorType(const CorrectionType correctionType,
            const DicTraverseSession *const traverseSession,
            const DicNode *const parentDicNode, const DicNode *const dicNode) const;

 private:
    DISALLOW_COPY_AND_ASSIGN(TypingWeighting);
    static const TypingWeighting sInstance;

    TypingWeighting() {}
    ~TypingWeighting() {}
};
} // namespace latinime
#endif // LATINIME_TYPING_WEIGHTING_H
