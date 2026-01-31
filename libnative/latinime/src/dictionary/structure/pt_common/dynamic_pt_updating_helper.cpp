/*
 * Copyright (C) 2013, The Android Open Source Project
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

#include "dictionary/structure/pt_common/dynamic_pt_updating_helper.h"

#include "dictionary/property/unigram_property.h"
#include "dictionary/structure/pt_common/dynamic_pt_reading_helper.h"
#include "dictionary/structure/pt_common/dynamic_pt_writing_utils.h"
#include "dictionary/structure/pt_common/patricia_trie_reading_utils.h"
#include "dictionary/structure/pt_common/pt_node_reader.h"
#include "dictionary/structure/pt_common/pt_node_writer.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

const int DynamicPtUpdatingHelper::CHILDREN_POSITION_FIELD_SIZE = 3;

bool DynamicPtUpdatingHelper::addUnigramWord(DynamicPtReadingHelper *const readingHelper,
        const CodePointArrayView wordCodePoints, const UnigramProperty *const unigramProperty,
        bool *const outAddedNewUnigram) {
    int parentPos = NOT_A_DICT_POS;
    while (!readingHelper->isEnd()) {
        const PtNodeParams ptNodeParams(readingHelper->getPtNodeParams());
        if (!ptNodeParams.isValid()) {
            break;
        }
        const size_t matchedCodePointCount = readingHelper->getPrevTotalCodePointCount();
        if (!readingHelper->isMatchedCodePoint(ptNodeParams, 0 /* index */,
                wordCodePoints[matchedCodePointCount])) {
            // The first code point is different from target code point. Skip this node and read
            // the next sibling node.
            readingHelper->readNextSiblingNode(ptNodeParams);
            continue;
        }
        // Check following merged node code points.
        const size_t nodeCodePointCount = ptNodeParams.getCodePointArrayView().size();
        for (size_t j = 1; j < nodeCodePointCount; ++j) {
            const size_t nextIndex = matchedCodePointCount + j;
            if (nextIndex >= wordCodePoints.size()
                    || !readingHelper->isMatchedCodePoint(ptNodeParams, j,
                            wordCodePoints[matchedCodePointCount + j])) {
                *outAddedNewUnigram = true;
                return reallocatePtNodeAndAddNewPtNodes(&ptNodeParams, j, unigramProperty,
                        wordCodePoints.skip(matchedCodePointCount));
            }
        }
        // All characters are matched.
        if (wordCodePoints.size() == readingHelper->getTotalCodePointCount(ptNodeParams)) {
            return setPtNodeProbability(&ptNodeParams, unigramProperty, outAddedNewUnigram);
        }
        if (!ptNodeParams.hasChildren()) {
            *outAddedNewUnigram = true;
            return createChildrenPtNodeArrayAndAChildPtNode(&ptNodeParams, unigramProperty,
                    wordCodePoints.skip(readingHelper->getTotalCodePointCount(ptNodeParams)));
        }
        // Advance to the children nodes.
        parentPos = ptNodeParams.getHeadPos();
        readingHelper->readChildNode(ptNodeParams);
    }
    if (readingHelper->isError()) {
        // The dictionary is invalid.
        return false;
    }
    int pos = readingHelper->getPosOfLastForwardLinkField();
    *outAddedNewUnigram = true;
    return createAndInsertNodeIntoPtNodeArray(parentPos,
            wordCodePoints.skip(readingHelper->getPrevTotalCodePointCount()), unigramProperty,
            &pos);
}

bool DynamicPtUpdatingHelper::addNgramEntry(const PtNodePosArrayView prevWordsPtNodePos,
        const int wordPos, const NgramProperty *const ngramProperty,
        bool *const outAddedNewEntry) {
    if (prevWordsPtNodePos.empty()) {
        return false;
    }
    ASSERT(prevWordsPtNodePos.size() <= MAX_PREV_WORD_COUNT_FOR_N_GRAM);
    int prevWordTerminalIds[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
    for (size_t i = 0; i < prevWordsPtNodePos.size(); ++i) {
        prevWordTerminalIds[i] = mPtNodeReader->fetchPtNodeParamsInBufferFromPtNodePos(
                prevWordsPtNodePos[i]).getTerminalId();
    }
    const WordIdArrayView prevWordIds(prevWordTerminalIds, prevWordsPtNodePos.size());
    const int wordId =
            mPtNodeReader->fetchPtNodeParamsInBufferFromPtNodePos(wordPos).getTerminalId();
    return mPtNodeWriter->addNgramEntry(prevWordIds, wordId, ngramProperty, outAddedNewEntry);
}

bool DynamicPtUpdatingHelper::removeNgramEntry(const PtNodePosArrayView prevWordsPtNodePos,
        const int wordPos) {
    if (prevWordsPtNodePos.empty()) {
        return false;
    }
    ASSERT(prevWordsPtNodePos.size() <= MAX_PREV_WORD_COUNT_FOR_N_GRAM);
    int prevWordTerminalIds[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
    for (size_t i = 0; i < prevWordsPtNodePos.size(); ++i) {
        prevWordTerminalIds[i] = mPtNodeReader->fetchPtNodeParamsInBufferFromPtNodePos(
                prevWordsPtNodePos[i]).getTerminalId();
    }
    const WordIdArrayView prevWordIds(prevWordTerminalIds, prevWordsPtNodePos.size());
    const int wordId =
            mPtNodeReader->fetchPtNodeParamsInBufferFromPtNodePos(wordPos).getTerminalId();
    return mPtNodeWriter->removeNgramEntry(prevWordIds, wordId);
}

bool DynamicPtUpdatingHelper::addShortcutTarget(const int wordPos,
        const CodePointArrayView targetCodePoints, const int shortcutProbability) {
    const PtNodeParams ptNodeParams(mPtNodeReader->fetchPtNodeParamsInBufferFromPtNodePos(wordPos));
    return mPtNodeWriter->addShortcutTarget(&ptNodeParams, targetCodePoints.data(),
            targetCodePoints.size(), shortcutProbability);
}

bool DynamicPtUpdatingHelper::createAndInsertNodeIntoPtNodeArray(const int parentPos,
        const CodePointArrayView ptNodeCodePoints, const UnigramProperty *const unigramProperty,
        int *const forwardLinkFieldPos) {
    const int newPtNodeArrayPos = mBuffer->getTailPosition();
    if (!DynamicPtWritingUtils::writeForwardLinkPositionAndAdvancePosition(mBuffer,
            newPtNodeArrayPos, forwardLinkFieldPos)) {
        return false;
    }
    return createNewPtNodeArrayWithAChildPtNode(parentPos, ptNodeCodePoints, unigramProperty);
}

bool DynamicPtUpdatingHelper::setPtNodeProbability(const PtNodeParams *const originalPtNodeParams,
        const UnigramProperty *const unigramProperty, bool *const outAddedNewUnigram) {
    if (originalPtNodeParams->isTerminal() && !originalPtNodeParams->isDeleted()) {
        // Overwrites the probability.
        *outAddedNewUnigram = false;
        return mPtNodeWriter->updatePtNodeUnigramProperty(originalPtNodeParams, unigramProperty);
    } else {
        // Make the node terminal and write the probability.
        *outAddedNewUnigram = true;
        const int movedPos = mBuffer->getTailPosition();
        int writingPos = movedPos;
        const PtNodeParams ptNodeParamsToWrite(getUpdatedPtNodeParams(originalPtNodeParams,
                unigramProperty->isNotAWord(), unigramProperty->isPossiblyOffensive(),
                true /* isTerminal */, originalPtNodeParams->getParentPos(),
                originalPtNodeParams->getCodePointArrayView(), unigramProperty->getProbability()));
        if (!mPtNodeWriter->writeNewTerminalPtNodeAndAdvancePosition(&ptNodeParamsToWrite,
                unigramProperty, &writingPos)) {
            return false;
        }
        if (!mPtNodeWriter->markPtNodeAsMoved(originalPtNodeParams, movedPos, movedPos)) {
            return false;
        }
    }
    return true;
}

bool DynamicPtUpdatingHelper::createChildrenPtNodeArrayAndAChildPtNode(
        const PtNodeParams *const parentPtNodeParams, const UnigramProperty *const unigramProperty,
        const CodePointArrayView codePoints) {
    const int newPtNodeArrayPos = mBuffer->getTailPosition();
    if (!mPtNodeWriter->updateChildrenPosition(parentPtNodeParams, newPtNodeArrayPos)) {
        return false;
    }
    return createNewPtNodeArrayWithAChildPtNode(parentPtNodeParams->getHeadPos(), codePoints,
            unigramProperty);
}

bool DynamicPtUpdatingHelper::createNewPtNodeArrayWithAChildPtNode(
        const int parentPtNodePos, const CodePointArrayView ptNodeCodePoints,
        const UnigramProperty *const unigramProperty) {
    int writingPos = mBuffer->getTailPosition();
    if (!DynamicPtWritingUtils::writePtNodeArraySizeAndAdvancePosition(mBuffer,
            1 /* arraySize */, &writingPos)) {
        return false;
    }
    const PtNodeParams ptNodeParamsToWrite(getPtNodeParamsForNewPtNode(
            unigramProperty->isNotAWord(), unigramProperty->isPossiblyOffensive(),
            true /* isTerminal */, parentPtNodePos, ptNodeCodePoints,
            unigramProperty->getProbability()));
    if (!mPtNodeWriter->writeNewTerminalPtNodeAndAdvancePosition(&ptNodeParamsToWrite,
            unigramProperty, &writingPos)) {
        return false;
    }
    if (!DynamicPtWritingUtils::writeForwardLinkPositionAndAdvancePosition(mBuffer,
            NOT_A_DICT_POS /* forwardLinkPos */, &writingPos)) {
        return false;
    }
    return true;
}

// Returns whether the dictionary updating was succeeded or not.
bool DynamicPtUpdatingHelper::reallocatePtNodeAndAddNewPtNodes(
        const PtNodeParams *const reallocatingPtNodeParams, const size_t overlappingCodePointCount,
        const UnigramProperty *const unigramProperty,
        const CodePointArrayView newPtNodeCodePoints) {
    // When addsExtraChild is true, split the reallocating PtNode and add new child.
    // Reallocating PtNode: abcde, newNode: abcxy.
    // abc (1st, not terminal) __ de (2nd)
    //                         \_ xy (extra child, terminal)
    // Otherwise, this method makes 1st part terminal and write information in unigramProperty.
    // Reallocating PtNode: abcde, newNode: abc.
    // abc (1st, terminal) __ de (2nd)
    const bool addsExtraChild = newPtNodeCodePoints.size() > overlappingCodePointCount;
    const int firstPartOfReallocatedPtNodePos = mBuffer->getTailPosition();
    int writingPos = firstPartOfReallocatedPtNodePos;
    // Write the 1st part of the reallocating node. The children position will be updated later
    // with actual children position.
    const CodePointArrayView firstPtNodeCodePoints =
            reallocatingPtNodeParams->getCodePointArrayView().limit(overlappingCodePointCount);
    if (addsExtraChild) {
        const PtNodeParams ptNodeParamsToWrite(getPtNodeParamsForNewPtNode(
                false /* isNotAWord */, false /* isPossiblyOffensive */, false /* isTerminal */,
                reallocatingPtNodeParams->getParentPos(), firstPtNodeCodePoints,
                NOT_A_PROBABILITY));
        if (!mPtNodeWriter->writePtNodeAndAdvancePosition(&ptNodeParamsToWrite, &writingPos)) {
            return false;
        }
    } else {
        const PtNodeParams ptNodeParamsToWrite(getPtNodeParamsForNewPtNode(
                unigramProperty->isNotAWord(), unigramProperty->isPossiblyOffensive(),
                true /* isTerminal */, reallocatingPtNodeParams->getParentPos(),
                firstPtNodeCodePoints, unigramProperty->getProbability()));
        if (!mPtNodeWriter->writeNewTerminalPtNodeAndAdvancePosition(&ptNodeParamsToWrite,
                unigramProperty, &writingPos)) {
            return false;
        }
    }
    const int actualChildrenPos = writingPos;
    // Create new children PtNode array.
    const size_t newPtNodeCount = addsExtraChild ? 2 : 1;
    if (!DynamicPtWritingUtils::writePtNodeArraySizeAndAdvancePosition(mBuffer,
            newPtNodeCount, &writingPos)) {
        return false;
    }
    // Write the 2nd part of the reallocating node.
    const int secondPartOfReallocatedPtNodePos = writingPos;
    const PtNodeParams childPartPtNodeParams(getUpdatedPtNodeParams(reallocatingPtNodeParams,
            reallocatingPtNodeParams->isNotAWord(), reallocatingPtNodeParams->isPossiblyOffensive(),
            reallocatingPtNodeParams->isTerminal(), firstPartOfReallocatedPtNodePos,
            reallocatingPtNodeParams->getCodePointArrayView().skip(overlappingCodePointCount),
            reallocatingPtNodeParams->getProbability()));
    if (!mPtNodeWriter->writePtNodeAndAdvancePosition(&childPartPtNodeParams, &writingPos)) {
        return false;
    }
    if (addsExtraChild) {
        const PtNodeParams extraChildPtNodeParams(getPtNodeParamsForNewPtNode(
                unigramProperty->isNotAWord(), unigramProperty->isPossiblyOffensive(),
                true /* isTerminal */, firstPartOfReallocatedPtNodePos,
                newPtNodeCodePoints.skip(overlappingCodePointCount),
                unigramProperty->getProbability()));
        if (!mPtNodeWriter->writeNewTerminalPtNodeAndAdvancePosition(&extraChildPtNodeParams,
                unigramProperty, &writingPos)) {
            return false;
        }
    }
    if (!DynamicPtWritingUtils::writeForwardLinkPositionAndAdvancePosition(mBuffer,
            NOT_A_DICT_POS /* forwardLinkPos */, &writingPos)) {
        return false;
    }
    // Update original reallocating PtNode as moved.
    if (!mPtNodeWriter->markPtNodeAsMoved(reallocatingPtNodeParams, firstPartOfReallocatedPtNodePos,
            secondPartOfReallocatedPtNodePos)) {
        return false;
    }
    // Load node info. Information of the 1st part will be fetched.
    const PtNodeParams ptNodeParams(
            mPtNodeReader->fetchPtNodeParamsInBufferFromPtNodePos(firstPartOfReallocatedPtNodePos));
    // Update children position.
    return mPtNodeWriter->updateChildrenPosition(&ptNodeParams, actualChildrenPos);
}

const PtNodeParams DynamicPtUpdatingHelper::getUpdatedPtNodeParams(
        const PtNodeParams *const originalPtNodeParams, const bool isNotAWord,
        const bool isPossiblyOffensive, const bool isTerminal, const int parentPos,
        const CodePointArrayView codePoints, const int probability) const {
    const PatriciaTrieReadingUtils::NodeFlags flags = PatriciaTrieReadingUtils::createAndGetFlags(
            isPossiblyOffensive, isNotAWord, isTerminal, false /* hasShortcutTargets */,
            false /* hasBigrams */, codePoints.size() > 1u /* hasMultipleChars */,
            CHILDREN_POSITION_FIELD_SIZE);
    return PtNodeParams(originalPtNodeParams, flags, parentPos, codePoints, probability);
}

const PtNodeParams DynamicPtUpdatingHelper::getPtNodeParamsForNewPtNode(const bool isNotAWord,
        const bool isPossiblyOffensive, const bool isTerminal, const int parentPos,
        const CodePointArrayView codePoints, const int probability) const {
    const PatriciaTrieReadingUtils::NodeFlags flags = PatriciaTrieReadingUtils::createAndGetFlags(
            isPossiblyOffensive, isNotAWord, isTerminal, false /* hasShortcutTargets */,
            false /* hasBigrams */, codePoints.size() > 1u /* hasMultipleChars */,
            CHILDREN_POSITION_FIELD_SIZE);
    return PtNodeParams(flags, parentPos, codePoints, probability);
}

} // namespace latinime
