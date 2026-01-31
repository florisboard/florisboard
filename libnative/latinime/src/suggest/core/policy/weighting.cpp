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

#include "suggest/core/policy/weighting.h"

#include "defines.h"
#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/dicnode/dic_node_profiler.h"
#include "suggest/core/dicnode/dic_node_utils.h"
#include "suggest/core/dictionary/error_type_utils.h"
#include "suggest/core/session/dic_traverse_session.h"

namespace latinime {

class MultiBigramMap;

static inline void profile(const CorrectionType correctionType, DicNode *const node) {
#if DEBUG_DICT
    switch (correctionType) {
    case CT_OMISSION:
        PROF_OMISSION(node->mProfiler);
        return;
    case CT_ADDITIONAL_PROXIMITY:
        PROF_ADDITIONAL_PROXIMITY(node->mProfiler);
        return;
    case CT_SUBSTITUTION:
        PROF_SUBSTITUTION(node->mProfiler);
        return;
    case CT_NEW_WORD_SPACE_OMISSION:
        PROF_NEW_WORD(node->mProfiler);
        return;
    case CT_MATCH:
        PROF_MATCH(node->mProfiler);
        return;
    case CT_COMPLETION:
        PROF_COMPLETION(node->mProfiler);
        return;
    case CT_TERMINAL:
        PROF_TERMINAL(node->mProfiler);
        return;
    case CT_TERMINAL_INSERTION:
        PROF_TERMINAL_INSERTION(node->mProfiler);
        return;
    case CT_NEW_WORD_SPACE_SUBSTITUTION:
        PROF_SPACE_SUBSTITUTION(node->mProfiler);
        return;
    case CT_INSERTION:
        PROF_INSERTION(node->mProfiler);
        return;
    case CT_TRANSPOSITION:
        PROF_TRANSPOSITION(node->mProfiler);
        return;
    default:
        // do nothing
        return;
    }
#else
    // do nothing
#endif
}

/* static */ void Weighting::addCostAndForwardInputIndex(const Weighting *const weighting,
        const CorrectionType correctionType, const DicTraverseSession *const traverseSession,
        const DicNode *const parentDicNode, DicNode *const dicNode,
        MultiBigramMap *const multiBigramMap) {
    const int inputSize = traverseSession->getInputSize();
    DicNode_InputStateG inputStateG;
    inputStateG.mNeedsToUpdateInputStateG = false; // Don't use input info by default
    const float spatialCost = Weighting::getSpatialCost(weighting, correctionType,
            traverseSession, parentDicNode, dicNode, &inputStateG);
    const float languageCost = Weighting::getLanguageCost(weighting, correctionType,
            traverseSession, parentDicNode, dicNode, multiBigramMap);
    const ErrorTypeUtils::ErrorType errorType = weighting->getErrorType(correctionType,
            traverseSession, parentDicNode, dicNode);
    profile(correctionType, dicNode);
    if (inputStateG.mNeedsToUpdateInputStateG) {
        dicNode->updateInputIndexG(&inputStateG);
    } else {
        dicNode->forwardInputIndex(0, getForwardInputCount(correctionType),
                (correctionType == CT_TRANSPOSITION));
    }
    dicNode->addCost(spatialCost, languageCost, weighting->needsToNormalizeCompoundDistance(),
            inputSize, errorType);
    if (CT_NEW_WORD_SPACE_OMISSION == correctionType) {
        // When we are on a terminal, we save the current distance for evaluating
        // when to auto-commit partial suggestions.
        dicNode->saveNormalizedCompoundDistanceAfterFirstWordIfNoneYet();
    }
}

/* static */ float Weighting::getSpatialCost(const Weighting *const weighting,
        const CorrectionType correctionType, const DicTraverseSession *const traverseSession,
        const DicNode *const parentDicNode, const DicNode *const dicNode,
        DicNode_InputStateG *const inputStateG) {
    switch(correctionType) {
    case CT_OMISSION:
        return weighting->getOmissionCost(parentDicNode, dicNode);
    case CT_ADDITIONAL_PROXIMITY:
        // only used for typing
        // TODO: Quit calling getMatchedCost().
        return weighting->getAdditionalProximityCost()
                + weighting->getMatchedCost(traverseSession, dicNode, inputStateG);
    case CT_SUBSTITUTION:
        // only used for typing
        // TODO: Quit calling getMatchedCost().
        return weighting->getSubstitutionCost()
                + weighting->getMatchedCost(traverseSession, dicNode, inputStateG);
    case CT_NEW_WORD_SPACE_OMISSION:
        return weighting->getSpaceOmissionCost(traverseSession, dicNode, inputStateG);
    case CT_MATCH:
        return weighting->getMatchedCost(traverseSession, dicNode, inputStateG);
    case CT_COMPLETION:
        return weighting->getCompletionCost(traverseSession, dicNode);
    case CT_TERMINAL:
        return weighting->getTerminalSpatialCost(traverseSession, dicNode);
    case CT_TERMINAL_INSERTION:
        return weighting->getTerminalInsertionCost(traverseSession, dicNode);
    case CT_NEW_WORD_SPACE_SUBSTITUTION:
        return weighting->getSpaceSubstitutionCost(traverseSession, dicNode);
    case CT_INSERTION:
        return weighting->getInsertionCost(traverseSession, parentDicNode, dicNode);
    case CT_TRANSPOSITION:
        return weighting->getTranspositionCost(traverseSession, parentDicNode, dicNode);
    default:
        return 0.0f;
    }
}

/* static */ float Weighting::getLanguageCost(const Weighting *const weighting,
        const CorrectionType correctionType, const DicTraverseSession *const traverseSession,
        const DicNode *const parentDicNode, const DicNode *const dicNode,
        MultiBigramMap *const multiBigramMap) {
    switch(correctionType) {
    case CT_OMISSION:
        return 0.0f;
    case CT_SUBSTITUTION:
        return 0.0f;
    case CT_NEW_WORD_SPACE_OMISSION:
        return weighting->getNewWordBigramLanguageCost(
                traverseSession, parentDicNode, multiBigramMap);
    case CT_MATCH:
        return 0.0f;
    case CT_COMPLETION:
        return 0.0f;
    case CT_TERMINAL: {
        const float languageImprobability =
                DicNodeUtils::getBigramNodeImprobability(
                        traverseSession->getDictionaryStructurePolicy(), dicNode, multiBigramMap);
        return weighting->getTerminalLanguageCost(traverseSession, dicNode, languageImprobability);
    }
    case CT_TERMINAL_INSERTION:
        return 0.0f;
    case CT_NEW_WORD_SPACE_SUBSTITUTION:
        return weighting->getNewWordBigramLanguageCost(
                traverseSession, parentDicNode, multiBigramMap);
    case CT_INSERTION:
        return 0.0f;
    case CT_TRANSPOSITION:
        return 0.0f;
    default:
        return 0.0f;
    }
}

/* static */ int Weighting::getForwardInputCount(const CorrectionType correctionType) {
    switch(correctionType) {
        case CT_OMISSION:
            return 0;
        case CT_ADDITIONAL_PROXIMITY:
            return 1;
        case CT_SUBSTITUTION:
            return 1;
        case CT_NEW_WORD_SPACE_OMISSION:
            return 0;
        case CT_MATCH:
            return 1;
        case CT_COMPLETION:
            return 1;
        case CT_TERMINAL:
            return 0;
        case CT_TERMINAL_INSERTION:
            return 1;
        case CT_NEW_WORD_SPACE_SUBSTITUTION:
            return 1;
        case CT_INSERTION:
            return 2; /* look ahead + skip the current char */
        case CT_TRANSPOSITION:
            return 2; /* look ahead + skip the current char */
        default:
            return 0;
    }
}
}  // namespace latinime
