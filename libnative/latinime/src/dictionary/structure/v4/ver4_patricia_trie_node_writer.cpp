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

#include "dictionary/structure/v4/ver4_patricia_trie_node_writer.h"

#include "dictionary/header/header_policy.h"
#include "dictionary/property/unigram_property.h"
#include "dictionary/structure/pt_common/dynamic_pt_reading_utils.h"
#include "dictionary/structure/pt_common/dynamic_pt_writing_utils.h"
#include "dictionary/structure/pt_common/patricia_trie_reading_utils.h"
#include "dictionary/structure/v4/content/probability_entry.h"
#include "dictionary/structure/v4/shortcut/ver4_shortcut_list_policy.h"
#include "dictionary/structure/v4/ver4_patricia_trie_node_reader.h"
#include "dictionary/structure/v4/ver4_dict_buffers.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "dictionary/utils/forgetting_curve_utils.h"

namespace latinime {

const int Ver4PatriciaTrieNodeWriter::CHILDREN_POSITION_FIELD_SIZE = 3;

bool Ver4PatriciaTrieNodeWriter::markPtNodeAsDeleted(
        const PtNodeParams *const toBeUpdatedPtNodeParams) {
    int pos = toBeUpdatedPtNodeParams->getHeadPos();
    const bool usesAdditionalBuffer = mTrieBuffer->isInAdditionalBuffer(pos);
    const uint8_t *const dictBuf = mTrieBuffer->getBuffer(usesAdditionalBuffer);
    if (usesAdditionalBuffer) {
        pos -= mTrieBuffer->getOriginalBufferSize();
    }
    // Read original flags
    const PatriciaTrieReadingUtils::NodeFlags originalFlags =
            PatriciaTrieReadingUtils::getFlagsAndAdvancePosition(dictBuf, &pos);
    const PatriciaTrieReadingUtils::NodeFlags updatedFlags =
            DynamicPtReadingUtils::updateAndGetFlags(originalFlags, false /* isMoved */,
                    true /* isDeleted */, false /* willBecomeNonTerminal */);
    int writingPos = toBeUpdatedPtNodeParams->getHeadPos();
    // Update flags.
    if (!DynamicPtWritingUtils::writeFlagsAndAdvancePosition(mTrieBuffer, updatedFlags,
            &writingPos)) {
        return false;
    }
    if (toBeUpdatedPtNodeParams->isTerminal()) {
        // The PtNode is a terminal. Delete entry from the terminal position lookup table.
        return mBuffers->getMutableTerminalPositionLookupTable()->setTerminalPtNodePosition(
                toBeUpdatedPtNodeParams->getTerminalId(), NOT_A_DICT_POS /* ptNodePos */);
    } else {
        return true;
    }
}

// TODO: Quit using bigramLinkedNodePos.
bool Ver4PatriciaTrieNodeWriter::markPtNodeAsMoved(
        const PtNodeParams *const toBeUpdatedPtNodeParams,
        const int movedPos, const int bigramLinkedNodePos) {
    int pos = toBeUpdatedPtNodeParams->getHeadPos();
    const bool usesAdditionalBuffer = mTrieBuffer->isInAdditionalBuffer(pos);
    const uint8_t *const dictBuf = mTrieBuffer->getBuffer(usesAdditionalBuffer);
    if (usesAdditionalBuffer) {
        pos -= mTrieBuffer->getOriginalBufferSize();
    }
    // Read original flags
    const PatriciaTrieReadingUtils::NodeFlags originalFlags =
            PatriciaTrieReadingUtils::getFlagsAndAdvancePosition(dictBuf, &pos);
    const PatriciaTrieReadingUtils::NodeFlags updatedFlags =
            DynamicPtReadingUtils::updateAndGetFlags(originalFlags, true /* isMoved */,
                    false /* isDeleted */, false /* willBecomeNonTerminal */);
    int writingPos = toBeUpdatedPtNodeParams->getHeadPos();
    // Update flags.
    if (!DynamicPtWritingUtils::writeFlagsAndAdvancePosition(mTrieBuffer, updatedFlags,
            &writingPos)) {
        return false;
    }
    // Update moved position, which is stored in the parent offset field.
    if (!DynamicPtWritingUtils::writeParentPosOffsetAndAdvancePosition(
            mTrieBuffer, movedPos, toBeUpdatedPtNodeParams->getHeadPos(), &writingPos)) {
        return false;
    }
    if (toBeUpdatedPtNodeParams->hasChildren()) {
        // Update children's parent position.
        mReadingHelper.initWithPtNodeArrayPos(toBeUpdatedPtNodeParams->getChildrenPos());
        while (!mReadingHelper.isEnd()) {
            const PtNodeParams childPtNodeParams(mReadingHelper.getPtNodeParams());
            int parentOffsetFieldPos = childPtNodeParams.getHeadPos()
                    + DynamicPtWritingUtils::NODE_FLAG_FIELD_SIZE;
            if (!DynamicPtWritingUtils::writeParentPosOffsetAndAdvancePosition(
                    mTrieBuffer, bigramLinkedNodePos, childPtNodeParams.getHeadPos(),
                    &parentOffsetFieldPos)) {
                // Parent offset cannot be written because of a bug or a broken dictionary; thus,
                // we give up to update dictionary.
                return false;
            }
            mReadingHelper.readNextSiblingNode(childPtNodeParams);
        }
    }
    return true;
}

bool Ver4PatriciaTrieNodeWriter::markPtNodeAsWillBecomeNonTerminal(
        const PtNodeParams *const toBeUpdatedPtNodeParams) {
    int pos = toBeUpdatedPtNodeParams->getHeadPos();
    const bool usesAdditionalBuffer = mTrieBuffer->isInAdditionalBuffer(pos);
    const uint8_t *const dictBuf = mTrieBuffer->getBuffer(usesAdditionalBuffer);
    if (usesAdditionalBuffer) {
        pos -= mTrieBuffer->getOriginalBufferSize();
    }
    // Read original flags
    const PatriciaTrieReadingUtils::NodeFlags originalFlags =
            PatriciaTrieReadingUtils::getFlagsAndAdvancePosition(dictBuf, &pos);
    const PatriciaTrieReadingUtils::NodeFlags updatedFlags =
            DynamicPtReadingUtils::updateAndGetFlags(originalFlags, false /* isMoved */,
                    false /* isDeleted */, true /* willBecomeNonTerminal */);
    if (!mBuffers->getMutableTerminalPositionLookupTable()->setTerminalPtNodePosition(
            toBeUpdatedPtNodeParams->getTerminalId(), NOT_A_DICT_POS /* ptNodePos */)) {
        AKLOGE("Cannot update terminal position lookup table. terminal id: %d",
                toBeUpdatedPtNodeParams->getTerminalId());
        return false;
    }
    // Update flags.
    int writingPos = toBeUpdatedPtNodeParams->getHeadPos();
    return DynamicPtWritingUtils::writeFlagsAndAdvancePosition(mTrieBuffer, updatedFlags,
            &writingPos);
}

bool Ver4PatriciaTrieNodeWriter::updatePtNodeUnigramProperty(
        const PtNodeParams *const toBeUpdatedPtNodeParams,
        const UnigramProperty *const unigramProperty) {
    // Update probability and historical information.
    // TODO: Update other information in the unigram property.
    if (!toBeUpdatedPtNodeParams->isTerminal()) {
        return false;
    }
    const ProbabilityEntry probabilityEntryOfUnigramProperty = ProbabilityEntry(unigramProperty);
    return mBuffers->getMutableLanguageModelDictContent()->setProbabilityEntry(
            toBeUpdatedPtNodeParams->getTerminalId(), &probabilityEntryOfUnigramProperty);
}

bool Ver4PatriciaTrieNodeWriter::updatePtNodeProbabilityAndGetNeedsToKeepPtNodeAfterGC(
        const PtNodeParams *const toBeUpdatedPtNodeParams, bool *const outNeedsToKeepPtNode) {
    if (!toBeUpdatedPtNodeParams->isTerminal()) {
        AKLOGE("updatePtNodeProbabilityAndGetNeedsToSaveForGC is called for non-terminal PtNode.");
        return false;
    }
    const ProbabilityEntry originalProbabilityEntry =
            mBuffers->getLanguageModelDictContent()->getProbabilityEntry(
                    toBeUpdatedPtNodeParams->getTerminalId());
    if (originalProbabilityEntry.isValid()) {
        *outNeedsToKeepPtNode = true;
        return true;
    }
    if (!markPtNodeAsWillBecomeNonTerminal(toBeUpdatedPtNodeParams)) {
        AKLOGE("Cannot mark PtNode as willBecomeNonTerminal.");
        return false;
    }
    *outNeedsToKeepPtNode = false;
    return true;
}

bool Ver4PatriciaTrieNodeWriter::updateChildrenPosition(
        const PtNodeParams *const toBeUpdatedPtNodeParams, const int newChildrenPosition) {
    int childrenPosFieldPos = toBeUpdatedPtNodeParams->getChildrenPosFieldPos();
    return DynamicPtWritingUtils::writeChildrenPositionAndAdvancePosition(mTrieBuffer,
            newChildrenPosition, &childrenPosFieldPos);
}

bool Ver4PatriciaTrieNodeWriter::updateTerminalId(const PtNodeParams *const toBeUpdatedPtNodeParams,
        const int newTerminalId) {
    return mTrieBuffer->writeUint(newTerminalId, Ver4DictConstants::TERMINAL_ID_FIELD_SIZE,
            toBeUpdatedPtNodeParams->getTerminalIdFieldPos());
}

bool Ver4PatriciaTrieNodeWriter::writePtNodeAndAdvancePosition(
        const PtNodeParams *const ptNodeParams, int *const ptNodeWritingPos) {
    return writePtNodeAndGetTerminalIdAndAdvancePosition(ptNodeParams, 0 /* outTerminalId */,
            ptNodeWritingPos);
}

bool Ver4PatriciaTrieNodeWriter::writeNewTerminalPtNodeAndAdvancePosition(
        const PtNodeParams *const ptNodeParams, const UnigramProperty *const unigramProperty,
        int *const ptNodeWritingPos) {
    int terminalId = Ver4DictConstants::NOT_A_TERMINAL_ID;
    if (!writePtNodeAndGetTerminalIdAndAdvancePosition(ptNodeParams, &terminalId,
            ptNodeWritingPos)) {
        return false;
    }
    // Write probability.
    ProbabilityEntry newProbabilityEntry;
    const ProbabilityEntry probabilityEntryOfUnigramProperty = ProbabilityEntry(unigramProperty);
    return mBuffers->getMutableLanguageModelDictContent()->setProbabilityEntry(
            terminalId, &probabilityEntryOfUnigramProperty);
}

// TODO: Support counting ngram entries.
bool Ver4PatriciaTrieNodeWriter::addNgramEntry(const WordIdArrayView prevWordIds, const int wordId,
        const NgramProperty *const ngramProperty, bool *const outAddedNewBigram) {
    LanguageModelDictContent *const languageModelDictContent =
            mBuffers->getMutableLanguageModelDictContent();
    const ProbabilityEntry probabilityEntry =
            languageModelDictContent->getNgramProbabilityEntry(prevWordIds, wordId);
    const ProbabilityEntry probabilityEntryOfNgramProperty(ngramProperty);
    if (!languageModelDictContent->setNgramProbabilityEntry(
            prevWordIds, wordId, &probabilityEntryOfNgramProperty)) {
        AKLOGE("Cannot add new ngram entry. prevWordId[0]: %d, prevWordId.size(): %zd, wordId: %d",
                prevWordIds[0], prevWordIds.size(), wordId);
        return false;
    }
    if (!probabilityEntry.isValid() && outAddedNewBigram) {
        *outAddedNewBigram = true;
    }
    return true;
}

bool Ver4PatriciaTrieNodeWriter::removeNgramEntry(const WordIdArrayView prevWordIds,
        const int wordId) {
    LanguageModelDictContent *const languageModelDictContent =
            mBuffers->getMutableLanguageModelDictContent();
    return languageModelDictContent->removeNgramProbabilityEntry(prevWordIds, wordId);
}

// TODO: Remove when we stop supporting v402 format.
bool Ver4PatriciaTrieNodeWriter::updateAllBigramEntriesAndDeleteUselessEntries(
            const PtNodeParams *const sourcePtNodeParams, int *const outBigramEntryCount) {
    // Do nothing.
    return true;
}

bool Ver4PatriciaTrieNodeWriter::updateAllPositionFields(
        const PtNodeParams *const toBeUpdatedPtNodeParams,
        const DictPositionRelocationMap *const dictPositionRelocationMap,
        int *const outBigramEntryCount) {
    int parentPos = toBeUpdatedPtNodeParams->getParentPos();
    if (parentPos != NOT_A_DICT_POS) {
        PtNodeWriter::PtNodePositionRelocationMap::const_iterator it =
                dictPositionRelocationMap->mPtNodePositionRelocationMap.find(parentPos);
        if (it != dictPositionRelocationMap->mPtNodePositionRelocationMap.end()) {
            parentPos = it->second;
        }
    }
    int writingPos = toBeUpdatedPtNodeParams->getHeadPos()
            + DynamicPtWritingUtils::NODE_FLAG_FIELD_SIZE;
    // Write updated parent offset.
    if (!DynamicPtWritingUtils::writeParentPosOffsetAndAdvancePosition(mTrieBuffer,
            parentPos, toBeUpdatedPtNodeParams->getHeadPos(), &writingPos)) {
        return false;
    }

    // Updates children position.
    int childrenPos = toBeUpdatedPtNodeParams->getChildrenPos();
    if (childrenPos != NOT_A_DICT_POS) {
        PtNodeWriter::PtNodeArrayPositionRelocationMap::const_iterator it =
                dictPositionRelocationMap->mPtNodeArrayPositionRelocationMap.find(childrenPos);
        if (it != dictPositionRelocationMap->mPtNodeArrayPositionRelocationMap.end()) {
            childrenPos = it->second;
        }
    }
    if (!updateChildrenPosition(toBeUpdatedPtNodeParams, childrenPos)) {
        return false;
    }
    return true;
}

bool Ver4PatriciaTrieNodeWriter::addShortcutTarget(const PtNodeParams *const ptNodeParams,
        const int *const targetCodePoints, const int targetCodePointCount,
        const int shortcutProbability) {
    if (!mShortcutPolicy->addNewShortcut(ptNodeParams->getTerminalId(),
            targetCodePoints, targetCodePointCount, shortcutProbability)) {
        AKLOGE("Cannot add new shortcut entry. terminalId: %d", ptNodeParams->getTerminalId());
        return false;
    }
    return true;
}

bool Ver4PatriciaTrieNodeWriter::writePtNodeAndGetTerminalIdAndAdvancePosition(
        const PtNodeParams *const ptNodeParams, int *const outTerminalId,
        int *const ptNodeWritingPos) {
    const int nodePos = *ptNodeWritingPos;
    // Write placeholder flags. The Node flags are updated with appropriate flags at the last step of the
    // PtNode writing.
    if (!DynamicPtWritingUtils::writeFlagsAndAdvancePosition(mTrieBuffer,
            0 /* nodeFlags */, ptNodeWritingPos)) {
        return false;
    }
    // Calculate a parent offset and write the offset.
    if (!DynamicPtWritingUtils::writeParentPosOffsetAndAdvancePosition(mTrieBuffer,
            ptNodeParams->getParentPos(), nodePos, ptNodeWritingPos)) {
        return false;
    }
    // Write code points
    if (!DynamicPtWritingUtils::writeCodePointsAndAdvancePosition(mTrieBuffer,
            ptNodeParams->getCodePoints(), ptNodeParams->getCodePointCount(), ptNodeWritingPos)) {
        return false;
    }
    int terminalId = Ver4DictConstants::NOT_A_TERMINAL_ID;
    if (!ptNodeParams->willBecomeNonTerminal()) {
        if (ptNodeParams->getTerminalId() != Ver4DictConstants::NOT_A_TERMINAL_ID) {
            terminalId = ptNodeParams->getTerminalId();
        } else if (ptNodeParams->isTerminal()) {
            // Write terminal information using a new terminal id.
            // Get a new unused terminal id.
            terminalId = mBuffers->getTerminalPositionLookupTable()->getNextTerminalId();
        }
    }
    const int isTerminal = terminalId != Ver4DictConstants::NOT_A_TERMINAL_ID;
    if (isTerminal) {
        // Update the lookup table.
        if (!mBuffers->getMutableTerminalPositionLookupTable()->setTerminalPtNodePosition(
                terminalId, nodePos)) {
            return false;
        }
        // Write terminal Id.
        if (!mTrieBuffer->writeUintAndAdvancePosition(terminalId,
                Ver4DictConstants::TERMINAL_ID_FIELD_SIZE, ptNodeWritingPos)) {
            return false;
        }
        if (outTerminalId) {
            *outTerminalId = terminalId;
        }
    }
    // Write children position
    if (!DynamicPtWritingUtils::writeChildrenPositionAndAdvancePosition(mTrieBuffer,
            ptNodeParams->getChildrenPos(), ptNodeWritingPos)) {
        return false;
    }
    return updatePtNodeFlags(nodePos, isTerminal,
            ptNodeParams->getCodePointCount() > 1 /* hasMultipleChars */);
}

bool Ver4PatriciaTrieNodeWriter::updatePtNodeFlags(const int ptNodePos, const bool isTerminal,
        const bool hasMultipleChars) {
    // Create node flags and write them.
    PatriciaTrieReadingUtils::NodeFlags nodeFlags =
            PatriciaTrieReadingUtils::createAndGetFlags(false /* isNotAWord */,
                    false /* isPossiblyOffensive */, isTerminal, false /* hasShortcutTargets */,
                    false /* hasBigrams */, hasMultipleChars, CHILDREN_POSITION_FIELD_SIZE);
    if (!DynamicPtWritingUtils::writeFlags(mTrieBuffer, nodeFlags, ptNodePos)) {
        AKLOGE("Cannot write PtNode flags. flags: %x, pos: %d", nodeFlags, ptNodePos);
        return false;
    }
    return true;
}

} // namespace latinime
