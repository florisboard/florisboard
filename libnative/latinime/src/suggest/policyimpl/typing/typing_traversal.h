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

#ifndef LATINIME_TYPING_TRAVERSAL_H
#define LATINIME_TYPING_TRAVERSAL_H

#include <cstdint>

#include "defines.h"
#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/dicnode/dic_node_vector.h"
#include "suggest/core/layout/proximity_info_state.h"
#include "suggest/core/layout/proximity_info_utils.h"
#include "suggest/core/policy/traversal.h"
#include "suggest/core/session/dic_traverse_session.h"
#include "suggest/core/suggest_options.h"
#include "suggest/policyimpl/typing/scoring_params.h"
#include "utils/char_utils.h"

namespace latinime {
class TypingTraversal : public Traversal {
 public:
    static const TypingTraversal *getInstance() { return &sInstance; }

    AK_FORCE_INLINE int getMaxPointerCount() const {
        return MAX_POINTER_COUNT;
    }

    AK_FORCE_INLINE bool allowsErrorCorrections(const DicNode *const dicNode) const {
        return dicNode->getNormalizedSpatialDistance()
                < ScoringParams::NORMALIZED_SPATIAL_DISTANCE_THRESHOLD_FOR_EDIT;
    }

    AK_FORCE_INLINE bool isOmission(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode, const DicNode *const childDicNode,
            const bool allowsErrorCorrections) const {
        if (!CORRECT_OMISSION) {
            return false;
        }
        // Note: Always consider intentional omissions (like apostrophes) since they are common.
        const bool canConsiderOmission =
                allowsErrorCorrections || childDicNode->canBeIntentionalOmission();
        if (!canConsiderOmission) {
            return false;
        }
        const int inputSize = traverseSession->getInputSize();
        // TODO: Don't refer to isCompletion?
        if (dicNode->isCompletion(inputSize)) {
            return false;
        }
        if (dicNode->canBeIntentionalOmission()) {
            return true;
        }
        const int point0Index = dicNode->getInputIndex(0);
        const int currentBaseLowerCodePoint =
                CharUtils::toBaseLowerCase(childDicNode->getNodeCodePoint());
        const int typedBaseLowerCodePoint =
                CharUtils::toBaseLowerCase(traverseSession->getProximityInfoState(0)
                        ->getPrimaryCodePointAt(point0Index));
        return (currentBaseLowerCodePoint != typedBaseLowerCodePoint);
    }

    AK_FORCE_INLINE bool isSpaceSubstitutionTerminal(
            const DicTraverseSession *const traverseSession, const DicNode *const dicNode) const {
        if (!CORRECT_NEW_WORD_SPACE_SUBSTITUTION) {
            return false;
        }
        if (traverseSession->getSuggestOptions()->weightForLocale()
                < ScoringParams::LOCALE_WEIGHT_THRESHOLD_FOR_SPACE_SUBSTITUTION) {
            // Space substitution is heavy, so we skip doing it if the weight for this language
            // is low because we anticipate the suggestions out of this dictionary are not for
            // the language the user intends to type in.
            return false;
        }
        if (!canDoLookAheadCorrection(traverseSession, dicNode)) {
            return false;
        }
        const int point0Index = dicNode->getInputIndex(0);
        return dicNode->isTerminalDicNode()
                && traverseSession->getProximityInfoState(0)->
                        hasSpaceProximity(point0Index);
    }

    AK_FORCE_INLINE bool isSpaceOmissionTerminal(
            const DicTraverseSession *const traverseSession, const DicNode *const dicNode) const {
        if (!CORRECT_NEW_WORD_SPACE_OMISSION) {
            return false;
        }
        if (traverseSession->getSuggestOptions()->weightForLocale()
                < ScoringParams::LOCALE_WEIGHT_THRESHOLD_FOR_SPACE_OMISSION) {
            // Space omission is heavy, so we skip doing it if the weight for this language
            // is low because we anticipate the suggestions out of this dictionary are not for
            // the language the user intends to type in.
            return false;
        }
        const int inputSize = traverseSession->getInputSize();
        // TODO: Don't refer to isCompletion?
        if (dicNode->isCompletion(inputSize)) {
            return false;
        }
        if (!dicNode->isTerminalDicNode()) {
            return false;
        }
        const int16_t pointIndex = dicNode->getInputIndex(0);
        return pointIndex <= inputSize && !dicNode->isTotalInputSizeExceedingLimit()
                && !dicNode->shouldBeFilteredBySafetyNetForBigram();
    }

    AK_FORCE_INLINE bool shouldDepthLevelCache(
            const DicTraverseSession *const traverseSession) const {
        const int inputSize = traverseSession->getInputSize();
        return traverseSession->isCacheBorderForTyping(inputSize);
    }

    AK_FORCE_INLINE bool shouldNodeLevelCache(
            const DicTraverseSession *const traverseSession, const DicNode *const dicNode) const {
        return false;
    }

    AK_FORCE_INLINE bool canDoLookAheadCorrection(
            const DicTraverseSession *const traverseSession, const DicNode *const dicNode) const {
        const int inputSize = traverseSession->getInputSize();
        return dicNode->canDoLookAheadCorrection(inputSize);
    }

    AK_FORCE_INLINE ProximityType getProximityType(
            const DicTraverseSession *const traverseSession, const DicNode *const dicNode,
            const DicNode *const childDicNode) const {
        return traverseSession->getProximityInfoState(0)->getProximityType(
                dicNode->getInputIndex(0), childDicNode->getNodeCodePoint(),
                true /* checkProximityChars */);
    }

    AK_FORCE_INLINE bool needsToTraverseAllUserInput() const {
        return true;
    }

    AK_FORCE_INLINE float getMaxSpatialDistance() const {
        return ScoringParams::MAX_SPATIAL_DISTANCE;
    }

    AK_FORCE_INLINE int getDefaultExpandDicNodeSize() const {
        return DicNodeVector::DEFAULT_NODES_SIZE_FOR_OPTIMIZATION;
    }

    AK_FORCE_INLINE int getMaxCacheSize(const int inputSize, const float weightForLocale) const {
        if (inputSize <= 1) {
            return ScoringParams::MAX_CACHE_DIC_NODE_SIZE_FOR_SINGLE_POINT;
        }
        if (weightForLocale < ScoringParams::LOCALE_WEIGHT_THRESHOLD_FOR_SMALL_CACHE_SIZE) {
            return ScoringParams::MAX_CACHE_DIC_NODE_SIZE_FOR_LOW_PROBABILITY_LOCALE;
        }
        return ScoringParams::MAX_CACHE_DIC_NODE_SIZE;
    }

    AK_FORCE_INLINE int getTerminalCacheSize() const {
        return MAX_RESULTS;
    }

    AK_FORCE_INLINE bool isPossibleOmissionChildNode(
            const DicTraverseSession *const traverseSession, const DicNode *const parentDicNode,
            const DicNode *const dicNode) const {
        const ProximityType proximityType =
                getProximityType(traverseSession, parentDicNode, dicNode);
        if (!ProximityInfoUtils::isMatchOrProximityChar(proximityType)) {
            return false;
        }
        return true;
    }

    AK_FORCE_INLINE bool isGoodToTraverseNextWord(const DicNode *const dicNode,
            const int probability) const {
        if (probability < ScoringParams::THRESHOLD_NEXT_WORD_PROBABILITY) {
            return false;
        }
        const bool shortCappedWord = dicNode->getNodeCodePointCount()
                < ScoringParams::THRESHOLD_SHORT_WORD_LENGTH && dicNode->isFirstCharUppercase();
        return !shortCappedWord
                || probability >= ScoringParams::THRESHOLD_NEXT_WORD_PROBABILITY_FOR_CAPPED;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(TypingTraversal);
    static const bool CORRECT_OMISSION;
    static const bool CORRECT_NEW_WORD_SPACE_SUBSTITUTION;
    static const bool CORRECT_NEW_WORD_SPACE_OMISSION;
    static const TypingTraversal sInstance;

    TypingTraversal() {}
    ~TypingTraversal() {}
};
} // namespace latinime
#endif // LATINIME_TYPING_TRAVERSAL_H
