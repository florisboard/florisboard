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

#include "suggest/core/suggest.h"

#include "dictionary/interface/dictionary_structure_with_buffer_policy.h"
#include "dictionary/property/word_attributes.h"
#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/dicnode/dic_node_priority_queue.h"
#include "suggest/core/dicnode/dic_node_vector.h"
#include "suggest/core/dictionary/dictionary.h"
#include "suggest/core/dictionary/digraph_utils.h"
#include "suggest/core/layout/proximity_info.h"
#include "suggest/core/policy/traversal.h"
#include "suggest/core/policy/weighting.h"
#include "suggest/core/result/suggestions_output_utils.h"
#include "suggest/core/session/dic_traverse_session.h"
#include "suggest/core/suggest_options.h"
#include "utils/profiler.h"

namespace latinime {

// Initialization of class constants.
const int Suggest::MIN_CONTINUOUS_SUGGESTION_INPUT_SIZE = 2;

/**
 * Returns a set of suggestions for the given input touch points. The commitPoint argument indicates
 * whether to prematurely commit the suggested words up to the given point for sentence-level
 * suggestion.
 *
 * Note: Currently does not support concurrent calls across threads. Continuous suggestion is
 * automatically activated for sequential calls that share the same starting input.
 * TODO: Stop detecting continuous suggestion. Start using traverseSession instead.
 */
void Suggest::getSuggestions(ProximityInfo *pInfo, void *traverseSession,
        int *inputXs, int *inputYs, int *times, int *pointerIds, int *inputCodePoints,
        int inputSize, const float weightOfLangModelVsSpatialModel,
        SuggestionResults *const outSuggestionResults) const {
    PROF_INIT;
    PROF_TIMER_START(0);
    const float maxSpatialDistance = TRAVERSAL->getMaxSpatialDistance();
    DicTraverseSession *tSession = static_cast<DicTraverseSession *>(traverseSession);
    tSession->setupForGetSuggestions(pInfo, inputCodePoints, inputSize, inputXs, inputYs, times,
            pointerIds, maxSpatialDistance, TRAVERSAL->getMaxPointerCount());
    // TODO: Add the way to evaluate cache

    initializeSearch(tSession);
    PROF_TIMER_END(0);
    PROF_TIMER_START(1);

    // keep expanding search dicNodes until all have terminated.
    while (tSession->getDicTraverseCache()->activeSize() > 0) {
        expandCurrentDicNodes(tSession);
        tSession->getDicTraverseCache()->advanceActiveDicNodes();
        tSession->getDicTraverseCache()->advanceInputIndex(inputSize);
    }
    PROF_TIMER_END(1);
    PROF_TIMER_START(2);
    SuggestionsOutputUtils::outputSuggestions(
            SCORING, tSession, weightOfLangModelVsSpatialModel, outSuggestionResults);
    PROF_TIMER_END(2);
}

/**
 * Initializes the search at the root of the lexicon trie. Note that when possible the search will
 * continue suggestion from where it left off during the last call.
 */
void Suggest::initializeSearch(DicTraverseSession *traverseSession) const {
    if (!traverseSession->getProximityInfoState(0)->isUsed()) {
        return;
    }

    if (traverseSession->getInputSize() > MIN_CONTINUOUS_SUGGESTION_INPUT_SIZE
            && traverseSession->isContinuousSuggestionPossible()) {
        // Continue suggestion
        traverseSession->getDicTraverseCache()->continueSearch();
    } else {
        // Restart recognition at the root.
        traverseSession->resetCache(TRAVERSAL->getMaxCacheSize(traverseSession->getInputSize(),
                traverseSession->getSuggestOptions()->weightForLocale()),
                TRAVERSAL->getTerminalCacheSize());
        // Create a new dic node here
        DicNode rootNode;
        DicNodeUtils::initAsRoot(traverseSession->getDictionaryStructurePolicy(),
                traverseSession->getPrevWordIds(), &rootNode);
        traverseSession->getDicTraverseCache()->copyPushActive(&rootNode);
    }
}

/**
 * Expands the dicNodes in the current search priority queue by advancing to the possible child
 * nodes based on the next touch point(s) (or no touch points for lookahead)
 */
void Suggest::expandCurrentDicNodes(DicTraverseSession *traverseSession) const {
    const int inputSize = traverseSession->getInputSize();
    DicNodeVector childDicNodes(TRAVERSAL->getDefaultExpandDicNodeSize());
    DicNode correctionDicNode;

    // TODO: Find more efficient caching
    const bool shouldDepthLevelCache = TRAVERSAL->shouldDepthLevelCache(traverseSession);
    if (shouldDepthLevelCache) {
        traverseSession->getDicTraverseCache()->updateLastCachedInputIndex();
    }
    if (DEBUG_CACHE) {
        AKLOGI("expandCurrentDicNodes depth level cache = %d, inputSize = %d",
                shouldDepthLevelCache, inputSize);
    }
    while (traverseSession->getDicTraverseCache()->activeSize() > 0) {
        DicNode dicNode;
        traverseSession->getDicTraverseCache()->popActive(&dicNode);
        if (dicNode.isTotalInputSizeExceedingLimit()) {
            return;
        }
        childDicNodes.clear();
        const int point0Index = dicNode.getInputIndex(0);
        const bool canDoLookAheadCorrection =
                TRAVERSAL->canDoLookAheadCorrection(traverseSession, &dicNode);
        const bool isLookAheadCorrection = canDoLookAheadCorrection
                && traverseSession->getDicTraverseCache()->
                        isLookAheadCorrectionInputIndex(static_cast<int>(point0Index));
        const bool isCompletion = dicNode.isCompletion(inputSize);

        const bool shouldNodeLevelCache =
                TRAVERSAL->shouldNodeLevelCache(traverseSession, &dicNode);
        if (shouldDepthLevelCache || shouldNodeLevelCache) {
            if (DEBUG_CACHE) {
                dicNode.dump("PUSH_CACHE");
            }
            traverseSession->getDicTraverseCache()->copyPushContinue(&dicNode);
            dicNode.setCached();
        }

        if (dicNode.isInDigraph()) {
            // Finish digraph handling if the node is in the middle of a digraph expansion.
            processDicNodeAsDigraph(traverseSession, &dicNode);
        } else if (isLookAheadCorrection) {
            // The algorithm maintains a small set of "deferred" nodes that have not consumed the
            // latest touch point yet. These are needed to apply look-ahead correction operations
            // that require special handling of the latest touch point. For example, with insertions
            // (e.g., "thiis" -> "this") the latest touch point should not be consumed at all.
            processDicNodeAsTransposition(traverseSession, &dicNode);
            processDicNodeAsInsertion(traverseSession, &dicNode);
        } else { // !isLookAheadCorrection
            // Only consider typing error corrections if the normalized compound distance is
            // below a spatial distance threshold.
            // NOTE: the threshold may need to be updated if scoring model changes.
            // TODO: Remove. Do not prune node here.
            const bool allowsErrorCorrections = TRAVERSAL->allowsErrorCorrections(&dicNode);
            // Process for handling space substitution (e.g., hevis => he is)
            if (TRAVERSAL->isSpaceSubstitutionTerminal(traverseSession, &dicNode)) {
                createNextWordDicNode(traverseSession, &dicNode, true /* spaceSubstitution */);
            }

            DicNodeUtils::getAllChildDicNodes(
                    &dicNode, traverseSession->getDictionaryStructurePolicy(), &childDicNodes);

            const int childDicNodesSize = childDicNodes.getSizeAndLock();
            for (int i = 0; i < childDicNodesSize; ++i) {
                DicNode *const childDicNode = childDicNodes[i];
                if (isCompletion) {
                    // Handle forward lookahead when the lexicon letter exceeds the input size.
                    processDicNodeAsMatch(traverseSession, childDicNode);
                    continue;
                }
                if (DigraphUtils::hasDigraphForCodePoint(
                        traverseSession->getDictionaryStructurePolicy()
                                ->getHeaderStructurePolicy(),
                        childDicNode->getNodeCodePoint())) {
                    correctionDicNode.initByCopy(childDicNode);
                    correctionDicNode.advanceDigraphIndex();
                    processDicNodeAsDigraph(traverseSession, &correctionDicNode);
                }
                if (TRAVERSAL->isOmission(traverseSession, &dicNode, childDicNode,
                        allowsErrorCorrections)) {
                    // TODO: (Gesture) Change weight between omission and substitution errors
                    // TODO: (Gesture) Terminal node should not be handled as omission
                    correctionDicNode.initByCopy(childDicNode);
                    processDicNodeAsOmission(traverseSession, &correctionDicNode);
                }
                const ProximityType proximityType = TRAVERSAL->getProximityType(
                        traverseSession, &dicNode, childDicNode);
                switch (proximityType) {
                    // TODO: Consider the difference of proximityType here
                    case MATCH_CHAR:
                    case PROXIMITY_CHAR:
                        processDicNodeAsMatch(traverseSession, childDicNode);
                        break;
                    case ADDITIONAL_PROXIMITY_CHAR:
                        if (allowsErrorCorrections) {
                            processDicNodeAsAdditionalProximityChar(traverseSession, &dicNode,
                                    childDicNode);
                        }
                        break;
                    case SUBSTITUTION_CHAR:
                        if (allowsErrorCorrections) {
                            processDicNodeAsSubstitution(traverseSession, &dicNode, childDicNode);
                        }
                        break;
                    case UNRELATED_CHAR:
                        // Just drop this dicNode and do nothing.
                        break;
                    default:
                        // Just drop this dicNode and do nothing.
                        break;
                }
            }

            // Push the dicNode for look-ahead correction
            if (allowsErrorCorrections && canDoLookAheadCorrection) {
                traverseSession->getDicTraverseCache()->copyPushNextActive(&dicNode);
            }
        }
    }
}

void Suggest::processTerminalDicNode(
        DicTraverseSession *traverseSession, DicNode *dicNode) const {
    if (dicNode->getCompoundDistance() >= static_cast<float>(MAX_VALUE_FOR_WEIGHTING)) {
        return;
    }
    if (!dicNode->isTerminalDicNode()) {
        return;
    }
    if (dicNode->shouldBeFilteredBySafetyNetForBigram()) {
        return;
    }
    if (!dicNode->hasMatchedOrProximityCodePoints()) {
        return;
    }
    // Create a non-cached node here.
    DicNode terminalDicNode(*dicNode);
    if (TRAVERSAL->needsToTraverseAllUserInput()
            && dicNode->getInputIndex(0) < traverseSession->getInputSize()) {
        Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_TERMINAL_INSERTION, traverseSession, 0,
                &terminalDicNode, traverseSession->getMultiBigramMap());
    }
    Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_TERMINAL, traverseSession, 0,
            &terminalDicNode, traverseSession->getMultiBigramMap());
    traverseSession->getDicTraverseCache()->copyPushTerminal(&terminalDicNode);
}

/**
 * Adds the expanded dicNode to the next search priority queue. Also creates an additional next word
 * (by the space omission error correction) search path if input dicNode is on a terminal.
 */
void Suggest::processExpandedDicNode(
        DicTraverseSession *traverseSession, DicNode *dicNode) const {
    processTerminalDicNode(traverseSession, dicNode);
    if (dicNode->getCompoundDistance() < static_cast<float>(MAX_VALUE_FOR_WEIGHTING)) {
        if (TRAVERSAL->isSpaceOmissionTerminal(traverseSession, dicNode)) {
            createNextWordDicNode(traverseSession, dicNode, false /* spaceSubstitution */);
        }
        const int allowsLookAhead = !(dicNode->hasMultipleWords()
                && dicNode->isCompletion(traverseSession->getInputSize()));
        if (dicNode->hasChildren() && allowsLookAhead) {
            traverseSession->getDicTraverseCache()->copyPushNextActive(dicNode);
        }
    }
}

void Suggest::processDicNodeAsMatch(DicTraverseSession *traverseSession,
        DicNode *childDicNode) const {
    weightChildNode(traverseSession, childDicNode);
    processExpandedDicNode(traverseSession, childDicNode);
}

void Suggest::processDicNodeAsAdditionalProximityChar(DicTraverseSession *traverseSession,
        DicNode *dicNode, DicNode *childDicNode) const {
    // Note: Most types of corrections don't need to look up the bigram information since they do
    // not treat the node as a terminal. There is no need to pass the bigram map in these cases.
    Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_ADDITIONAL_PROXIMITY,
            traverseSession, dicNode, childDicNode, 0 /* multiBigramMap */);
    processExpandedDicNode(traverseSession, childDicNode);
}

void Suggest::processDicNodeAsSubstitution(DicTraverseSession *traverseSession,
        DicNode *dicNode, DicNode *childDicNode) const {
    Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_SUBSTITUTION, traverseSession,
            dicNode, childDicNode, 0 /* multiBigramMap */);
    processExpandedDicNode(traverseSession, childDicNode);
}

// Process the DicNode codepoint as a digraph. This means that composite glyphs like the German
// u-umlaut is expanded to the transliteration "ue". Note that this happens in parallel with
// the normal non-digraph traversal, so both "uber" and "ueber" can be corrected to "[u-umlaut]ber".
void Suggest::processDicNodeAsDigraph(DicTraverseSession *traverseSession,
        DicNode *childDicNode) const {
    weightChildNode(traverseSession, childDicNode);
    childDicNode->advanceDigraphIndex();
    processExpandedDicNode(traverseSession, childDicNode);
}

/**
 * Handle the dicNode as an omission error (e.g., ths => this). Skip the current letter and consider
 * matches for all possible next letters. Note that just skipping the current letter without any
 * other conditions tends to flood the search DicNodes cache with omission DicNodes. Instead, check
 * the possible *next* letters after the omission to better limit search to plausible omissions.
 * Note that apostrophes are handled as omissions.
 */
void Suggest::processDicNodeAsOmission(
        DicTraverseSession *traverseSession, DicNode *dicNode) const {
    DicNodeVector childDicNodes;
    DicNodeUtils::getAllChildDicNodes(
            dicNode, traverseSession->getDictionaryStructurePolicy(), &childDicNodes);

    const int size = childDicNodes.getSizeAndLock();
    for (int i = 0; i < size; i++) {
        DicNode *const childDicNode = childDicNodes[i];
        // Treat this word as omission
        Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_OMISSION, traverseSession,
                dicNode, childDicNode, 0 /* multiBigramMap */);
        weightChildNode(traverseSession, childDicNode);
        if (!TRAVERSAL->isPossibleOmissionChildNode(traverseSession, dicNode, childDicNode)) {
            continue;
        }
        processExpandedDicNode(traverseSession, childDicNode);
    }
}

/**
 * Handle the dicNode as an insertion error (e.g., thiis => this). Skip the current touch point and
 * consider matches for the next touch point.
 */
void Suggest::processDicNodeAsInsertion(DicTraverseSession *traverseSession,
        DicNode *dicNode) const {
    const int16_t pointIndex = dicNode->getInputIndex(0);
    DicNodeVector childDicNodes;
    DicNodeUtils::getAllChildDicNodes(dicNode, traverseSession->getDictionaryStructurePolicy(),
            &childDicNodes);
    const int size = childDicNodes.getSizeAndLock();
    for (int i = 0; i < size; i++) {
        if (traverseSession->getProximityInfoState(0)->getPrimaryCodePointAt(pointIndex + 1)
                != childDicNodes[i]->getNodeCodePoint()) {
            continue;
        }
        DicNode *const childDicNode = childDicNodes[i];
        Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_INSERTION, traverseSession,
                dicNode, childDicNode, 0 /* multiBigramMap */);
        processExpandedDicNode(traverseSession, childDicNode);
    }
}

/**
 * Handle the dicNode as a transposition error (e.g., thsi => this). Swap the next two touch points.
 */
void Suggest::processDicNodeAsTransposition(DicTraverseSession *traverseSession,
        DicNode *dicNode) const {
    const int16_t pointIndex = dicNode->getInputIndex(0);
    DicNodeVector childDicNodes1;
    DicNodeVector childDicNodes2;
    DicNodeUtils::getAllChildDicNodes(dicNode, traverseSession->getDictionaryStructurePolicy(),
            &childDicNodes1);
    const int childSize1 = childDicNodes1.getSizeAndLock();
    for (int i = 0; i < childSize1; i++) {
        const ProximityType matchedId1 = traverseSession->getProximityInfoState(0)
                ->getProximityType(pointIndex + 1, childDicNodes1[i]->getNodeCodePoint(),
                        true /* checkProximityChars */);
        if (!ProximityInfoUtils::isMatchOrProximityChar(matchedId1)) {
            continue;
        }
        if (childDicNodes1[i]->hasChildren()) {
            childDicNodes2.clear();
            DicNodeUtils::getAllChildDicNodes(childDicNodes1[i],
                    traverseSession->getDictionaryStructurePolicy(), &childDicNodes2);
            const int childSize2 = childDicNodes2.getSizeAndLock();
            for (int j = 0; j < childSize2; j++) {
                DicNode *const childDicNode2 = childDicNodes2[j];
                const ProximityType matchedId2 = traverseSession->getProximityInfoState(0)
                        ->getProximityType(pointIndex, childDicNode2->getNodeCodePoint(),
                                true /* checkProximityChars */);
                if (!ProximityInfoUtils::isMatchOrProximityChar(matchedId2)) {
                    continue;
                }
                Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_TRANSPOSITION,
                        traverseSession, childDicNodes1[i], childDicNode2, 0 /* multiBigramMap */);
                processExpandedDicNode(traverseSession, childDicNode2);
            }
        }
    }
}

/**
 * Weight child dicNode by aligning it to the key
 */
void Suggest::weightChildNode(DicTraverseSession *traverseSession, DicNode *dicNode) const {
    const int inputSize = traverseSession->getInputSize();
    if (dicNode->isCompletion(inputSize)) {
        Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_COMPLETION, traverseSession,
                0 /* parentDicNode */, dicNode, 0 /* multiBigramMap */);
    } else {
        Weighting::addCostAndForwardInputIndex(WEIGHTING, CT_MATCH, traverseSession,
                0 /* parentDicNode */, dicNode, 0 /* multiBigramMap */);
    }
}

/**
 * Creates a new dicNode that represents a space insertion at the end of the input dicNode. Also
 * incorporates the unigram / bigram score for the ending word into the new dicNode.
 */
void Suggest::createNextWordDicNode(DicTraverseSession *traverseSession, DicNode *dicNode,
        const bool spaceSubstitution) const {
    const WordAttributes wordAttributes =
            traverseSession->getDictionaryStructurePolicy()->getWordAttributesInContext(
                    dicNode->getPrevWordIds(), dicNode->getWordId(),
                    traverseSession->getMultiBigramMap());
    if (SuggestionsOutputUtils::shouldBlockWord(traverseSession->getSuggestOptions(),
            dicNode, wordAttributes, false /* isLastWord */)) {
        return;
    }

    if (!TRAVERSAL->isGoodToTraverseNextWord(dicNode, wordAttributes.getProbability())) {
        return;
    }

    // Create a non-cached node here.
    DicNode newDicNode;
    DicNodeUtils::initAsRootWithPreviousWord(
            traverseSession->getDictionaryStructurePolicy(), dicNode, &newDicNode);
    const CorrectionType correctionType = spaceSubstitution ?
            CT_NEW_WORD_SPACE_SUBSTITUTION : CT_NEW_WORD_SPACE_OMISSION;
    Weighting::addCostAndForwardInputIndex(WEIGHTING, correctionType, traverseSession, dicNode,
            &newDicNode, traverseSession->getMultiBigramMap());
    if (newDicNode.getCompoundDistance() < static_cast<float>(MAX_VALUE_FOR_WEIGHTING)) {
        // newDicNode is worth continuing to traverse.
        // CAVEAT: This pruning is important for speed. Remove this when we can afford not to prune
        // here because here is not the right place to do pruning. Pruning should take place only
        // in DicNodePriorityQueue.
        traverseSession->getDicTraverseCache()->copyPushNextActive(&newDicNode);
    }
}
} // namespace latinime
