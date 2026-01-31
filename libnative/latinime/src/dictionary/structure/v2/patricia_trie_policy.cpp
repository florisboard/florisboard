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

#include "dictionary/structure/v2/patricia_trie_policy.h"

#include "defines.h"
#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/dicnode/dic_node_vector.h"
#include "dictionary/interface/ngram_listener.h"
#include "dictionary/property/ngram_context.h"
#include "dictionary/structure/pt_common/dynamic_pt_reading_helper.h"
#include "dictionary/structure/pt_common/patricia_trie_reading_utils.h"
#include "dictionary/utils/binary_dictionary_bigrams_iterator.h"
#include "dictionary/utils/multi_bigram_map.h"
#include "dictionary/utils/probability_utils.h"
#include "utils/char_utils.h"

namespace latinime {

void PatriciaTriePolicy::createAndGetAllChildDicNodes(const DicNode *const dicNode,
        DicNodeVector *const childDicNodes) const {
    if (!dicNode->hasChildren()) {
        return;
    }
    int nextPos = dicNode->getChildrenPtNodeArrayPos();
    if (!isValidPos(nextPos)) {
        AKLOGE("Children PtNode array position is invalid. pos: %d, dict size: %zd",
                nextPos, mBuffer.size());
        mIsCorrupted = true;
        ASSERT(false);
        return;
    }
    const int childCount = PatriciaTrieReadingUtils::getPtNodeArraySizeAndAdvancePosition(
            mBuffer.data(), &nextPos);
    for (int i = 0; i < childCount; i++) {
        if (!isValidPos(nextPos)) {
            AKLOGE("Child PtNode position is invalid. pos: %d, dict size: %zd, childCount: %d / %d",
                    nextPos, mBuffer.size(), i, childCount);
            mIsCorrupted = true;
            ASSERT(false);
            return;
        }
        nextPos = createAndGetLeavingChildNode(dicNode, nextPos, childDicNodes);
    }
}

int PatriciaTriePolicy::getCodePointsAndReturnCodePointCount(const int wordId,
        const int maxCodePointCount, int *const outCodePoints) const {
    return getCodePointsAndProbabilityAndReturnCodePointCount(wordId, maxCodePointCount,
            outCodePoints, nullptr /* outUnigramProbability */);
}
// This retrieves code points and the probability of the word by its id.
// Due to the fact that words are ordered in the dictionary in a strict breadth-first order,
// it is possible to check for this with advantageous complexity. For each PtNode array, we search
// for PtNodes with children and compare the children position with the position we look for.
// When we shoot the position we look for, it means the word we look for is in the children
// of the previous PtNode. The only tricky part is the fact that if we arrive at the end of a
// PtNode array with the last PtNode's children position still less than what we are searching for,
// we must descend the last PtNode's children (for example, if the word we are searching for starts
// with a z, it's the last PtNode of the root array, so all children addresses will be smaller
// than the position we look for, and we have to descend the z PtNode).
/* Parameters :
 * wordId: Id of the word we are searching for.
 * outCodePoints: an array to write the found word, with MAX_WORD_LENGTH size.
 * outUnigramProbability: a pointer to an int to write the probability into.
 * Return value : the code point count, of 0 if the word was not found.
 */
// TODO: Split this function to be more readable
int PatriciaTriePolicy::getCodePointsAndProbabilityAndReturnCodePointCount(
        const int wordId, const int maxCodePointCount, int *const outCodePoints,
        int *const outUnigramProbability) const {
    const int ptNodePos = getTerminalPtNodePosFromWordId(wordId);
    int pos = getRootPosition();
    int wordPos = 0;
    const int *const codePointTable = mHeaderPolicy.getCodePointTable();
    if (outUnigramProbability) {
        *outUnigramProbability = NOT_A_PROBABILITY;
    }
    // One iteration of the outer loop iterates through PtNode arrays. As stated above, we will
    // only traverse PtNodes that are actually a part of the terminal we are searching, so each
    // time we enter this loop we are one depth level further than last time.
    // The only reason we count PtNodes is because we want to reduce the probability of infinite
    // looping in case there is a bug. Since we know there is an upper bound to the depth we are
    // supposed to traverse, it does not hurt to count iterations.
    for (int loopCount = maxCodePointCount; loopCount > 0; --loopCount) {
        int lastCandidatePtNodePos = 0;
        // Let's loop through PtNodes in this PtNode array searching for either the terminal
        // or one of its ascendants.
        if (!isValidPos(pos)) {
            AKLOGE("PtNode array position is invalid. pos: %d, dict size: %zd",
                    pos, mBuffer.size());
            mIsCorrupted = true;
            ASSERT(false);
            return 0;
        }
        for (int ptNodeCount = PatriciaTrieReadingUtils::getPtNodeArraySizeAndAdvancePosition(
                mBuffer.data(), &pos); ptNodeCount > 0; --ptNodeCount) {
            const int startPos = pos;
            if (!isValidPos(pos)) {
                AKLOGE("PtNode position is invalid. pos: %d, dict size: %zd", pos, mBuffer.size());
                mIsCorrupted = true;
                ASSERT(false);
                return 0;
            }
            const PatriciaTrieReadingUtils::NodeFlags flags =
                    PatriciaTrieReadingUtils::getFlagsAndAdvancePosition(mBuffer.data(), &pos);
            const int character = PatriciaTrieReadingUtils::getCodePointAndAdvancePosition(
                    mBuffer.data(), codePointTable, &pos);
            if (ptNodePos == startPos) {
                // We found the position. Copy the rest of the code points in the buffer and return
                // the length.
                outCodePoints[wordPos] = character;
                if (PatriciaTrieReadingUtils::hasMultipleChars(flags)) {
                    int nextChar = PatriciaTrieReadingUtils::getCodePointAndAdvancePosition(
                            mBuffer.data(), codePointTable, &pos);
                    // We count code points in order to avoid infinite loops if the file is broken
                    // or if there is some other bug
                    int charCount = maxCodePointCount;
                    while (NOT_A_CODE_POINT != nextChar && --charCount > 0) {
                        outCodePoints[++wordPos] = nextChar;
                        nextChar = PatriciaTrieReadingUtils::getCodePointAndAdvancePosition(
                                mBuffer.data(), codePointTable, &pos);
                    }
                }
                if (outUnigramProbability) {
                    *outUnigramProbability =
                            PatriciaTrieReadingUtils::readProbabilityAndAdvancePosition(
                                    mBuffer.data(), &pos);
                }
                return ++wordPos;
            }
            // We need to skip past this PtNode, so skip any remaining code points after the
            // first and possibly the probability.
            if (PatriciaTrieReadingUtils::hasMultipleChars(flags)) {
                PatriciaTrieReadingUtils::skipCharacters(mBuffer.data(), flags, MAX_WORD_LENGTH,
                        codePointTable, &pos);
            }
            if (PatriciaTrieReadingUtils::isTerminal(flags)) {
                PatriciaTrieReadingUtils::readProbabilityAndAdvancePosition(mBuffer.data(), &pos);
            }
            // The fact that this PtNode has children is very important. Since we already know
            // that this PtNode does not match, if it has no children we know it is irrelevant
            // to what we are searching for.
            const bool hasChildren = PatriciaTrieReadingUtils::hasChildrenInFlags(flags);
            // We will write in `found' whether we have passed the children position we are
            // searching for. For example if we search for "beer", the children of b are less
            // than the address we are searching for and the children of c are greater. When we
            // come here for c, we realize this is too big, and that we should descend b.
            bool found;
            if (hasChildren) {
                int currentPos = pos;
                // Here comes the tricky part. First, read the children position.
                const int childrenPos = PatriciaTrieReadingUtils
                        ::readChildrenPositionAndAdvancePosition(mBuffer.data(), flags,
                                &currentPos);
                if (childrenPos > ptNodePos) {
                    // If the children pos is greater than the position, it means the previous
                    // PtNode, which position is stored in lastCandidatePtNodePos, was the right
                    // one.
                    found = true;
                } else if (1 >= ptNodeCount) {
                    // However if we are on the LAST PtNode of this array, and we have NOT shot the
                    // position we should descend THIS PtNode. So we trick the
                    // lastCandidatePtNodePos so that we will descend this PtNode, not the previous
                    // one.
                    lastCandidatePtNodePos = startPos;
                    found = true;
                } else {
                    // Else, we should continue looking.
                    found = false;
                }
            } else {
                // Even if we don't have children here, we could still be on the last PtNode of
                // this array. If this is the case, we should descend the last PtNode that had
                // children, and their position is already in lastCandidatePtNodePos.
                found = (1 >= ptNodeCount);
            }

            if (found) {
                // Okay, we found the PtNode we should descend. Its position is in
                // the lastCandidatePtNodePos variable, so we just re-read it.
                if (0 != lastCandidatePtNodePos) {
                    const PatriciaTrieReadingUtils::NodeFlags lastFlags =
                            PatriciaTrieReadingUtils::getFlagsAndAdvancePosition(
                                    mBuffer.data(), &lastCandidatePtNodePos);
                    const int lastChar = PatriciaTrieReadingUtils::getCodePointAndAdvancePosition(
                            mBuffer.data(), codePointTable, &lastCandidatePtNodePos);
                    // We copy all the characters in this PtNode to the buffer
                    outCodePoints[wordPos] = lastChar;
                    if (PatriciaTrieReadingUtils::hasMultipleChars(lastFlags)) {
                        int nextChar = PatriciaTrieReadingUtils::getCodePointAndAdvancePosition(
                                mBuffer.data(), codePointTable, &lastCandidatePtNodePos);
                        int charCount = maxCodePointCount;
                        while (-1 != nextChar && --charCount > 0) {
                            outCodePoints[++wordPos] = nextChar;
                            nextChar = PatriciaTrieReadingUtils::getCodePointAndAdvancePosition(
                                    mBuffer.data(), codePointTable, &lastCandidatePtNodePos);
                        }
                    }
                    ++wordPos;
                    // Now we only need to branch to the children address. Skip the probability if
                    // it's there, read pos, and break to resume the search at pos.
                    if (PatriciaTrieReadingUtils::isTerminal(lastFlags)) {
                        PatriciaTrieReadingUtils::readProbabilityAndAdvancePosition(mBuffer.data(),
                                &lastCandidatePtNodePos);
                    }
                    pos = PatriciaTrieReadingUtils::readChildrenPositionAndAdvancePosition(
                            mBuffer.data(), lastFlags, &lastCandidatePtNodePos);
                    break;
                } else {
                    // Here is a little tricky part: we come here if we found out that all children
                    // addresses in this PtNode are bigger than the address we are searching for.
                    // Should we conclude the word is not in the dictionary? No! It could still be
                    // one of the remaining PtNodes in this array, so we have to keep looking in
                    // this array until we find it (or we realize it's not there either, in which
                    // case it's actually not in the dictionary). Pass the end of this PtNode,
                    // ready to start the next one.
                    if (PatriciaTrieReadingUtils::hasChildrenInFlags(flags)) {
                        PatriciaTrieReadingUtils::readChildrenPositionAndAdvancePosition(
                                mBuffer.data(), flags, &pos);
                    }
                    if (PatriciaTrieReadingUtils::hasShortcutTargets(flags)) {
                        mShortcutListPolicy.skipAllShortcuts(&pos);
                    }
                    if (PatriciaTrieReadingUtils::hasBigrams(flags)) {
                        if (!mBigramListPolicy.skipAllBigrams(&pos)) {
                            AKLOGE("Cannot skip bigrams. BufSize: %zd, pos: %d.", mBuffer.size(),
                                    pos);
                            mIsCorrupted = true;
                            ASSERT(false);
                            return 0;
                        }
                    }
                }
            } else {
                // If we did not find it, we should record the last children address for the next
                // iteration.
                if (hasChildren) lastCandidatePtNodePos = startPos;
                // Now skip the end of this PtNode (children pos and the attributes if any) so that
                // our pos is after the end of this PtNode, at the start of the next one.
                if (PatriciaTrieReadingUtils::hasChildrenInFlags(flags)) {
                    PatriciaTrieReadingUtils::readChildrenPositionAndAdvancePosition(
                            mBuffer.data(), flags, &pos);
                }
                if (PatriciaTrieReadingUtils::hasShortcutTargets(flags)) {
                    mShortcutListPolicy.skipAllShortcuts(&pos);
                }
                if (PatriciaTrieReadingUtils::hasBigrams(flags)) {
                    if (!mBigramListPolicy.skipAllBigrams(&pos)) {
                        AKLOGE("Cannot skip bigrams. BufSize: %zd, pos: %d.", mBuffer.size(), pos);
                        mIsCorrupted = true;
                        ASSERT(false);
                        return 0;
                    }
                }
            }

        }
    }
    // If we have looked through all the PtNodes and found no match, the ptNodePos is
    // not the position of a terminal in this dictionary.
    return 0;
}

// This function gets the position of the terminal PtNode of the exact matching word in the
// dictionary. If no match is found, it returns NOT_A_WORD_ID.
int PatriciaTriePolicy::getWordId(const CodePointArrayView wordCodePoints,
        const bool forceLowerCaseSearch) const {
    DynamicPtReadingHelper readingHelper(&mPtNodeReader, &mPtNodeArrayReader);
    readingHelper.initWithPtNodeArrayPos(getRootPosition());
    const int ptNodePos = readingHelper.getTerminalPtNodePositionOfWord(wordCodePoints.data(),
            wordCodePoints.size(), forceLowerCaseSearch);
    if (readingHelper.isError()) {
        mIsCorrupted = true;
        AKLOGE("Dictionary reading error in getWordId().");
    }
    return getWordIdFromTerminalPtNodePos(ptNodePos);
}

const WordAttributes PatriciaTriePolicy::getWordAttributesInContext(
        const WordIdArrayView prevWordIds, const int wordId,
        MultiBigramMap *const multiBigramMap) const {
    if (wordId == NOT_A_WORD_ID) {
        return WordAttributes();
    }
    const int ptNodePos = getTerminalPtNodePosFromWordId(wordId);
    const PtNodeParams ptNodeParams =
            mPtNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos);
    if (multiBigramMap) {
        const int probability =  multiBigramMap->getBigramProbability(this /* structurePolicy */,
                prevWordIds, wordId, ptNodeParams.getProbability());
        return getWordAttributes(probability, ptNodeParams);
    }
    if (!prevWordIds.empty()) {
        const int bigramProbability = getProbabilityOfWord(prevWordIds, wordId);
        if (bigramProbability != NOT_A_PROBABILITY) {
            return getWordAttributes(bigramProbability, ptNodeParams);
        }
    }
    return getWordAttributes(getProbability(ptNodeParams.getProbability(), NOT_A_PROBABILITY),
            ptNodeParams);
}

const WordAttributes PatriciaTriePolicy::getWordAttributes(const int probability,
        const PtNodeParams &ptNodeParams) const {
    return WordAttributes(probability, false /* isBlacklisted */, ptNodeParams.isNotAWord(),
            ptNodeParams.isPossiblyOffensive());
}

int PatriciaTriePolicy::getProbability(const int unigramProbability,
        const int bigramProbability) const {
    // Due to space constraints, the probability for bigrams is approximate - the lower the unigram
    // probability, the worse the precision. The theoritical maximum error in resulting probability
    // is 8 - although in the practice it's never bigger than 3 or 4 in very bad cases. This means
    // that sometimes, we'll see some bigrams interverted here, but it can't get too bad.
    if (unigramProbability == NOT_A_PROBABILITY) {
        return NOT_A_PROBABILITY;
    } else if (bigramProbability == NOT_A_PROBABILITY) {
        return ProbabilityUtils::backoff(unigramProbability);
    } else {
        return ProbabilityUtils::computeProbabilityForBigram(unigramProbability,
                bigramProbability);
    }
}

int PatriciaTriePolicy::getProbabilityOfWord(const WordIdArrayView prevWordIds,
        const int wordId) const {
    if (wordId == NOT_A_WORD_ID) {
        return NOT_A_PROBABILITY;
    }
    const int ptNodePos = getTerminalPtNodePosFromWordId(wordId);
    const PtNodeParams ptNodeParams =
            mPtNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos);
    if (ptNodeParams.isNotAWord()) {
        // If this is not a word, it should behave as having no probability outside of the
        // suggestion process (where it should be used for shortcuts).
        return NOT_A_PROBABILITY;
    }
    if (!prevWordIds.empty()) {
        const int bigramsPosition = getBigramsPositionOfPtNode(
                getTerminalPtNodePosFromWordId(prevWordIds[0]));
        BinaryDictionaryBigramsIterator bigramsIt(&mBigramListPolicy, bigramsPosition);
        while (bigramsIt.hasNext()) {
            bigramsIt.next();
            if (bigramsIt.getBigramPos() == ptNodePos
                    && bigramsIt.getProbability() != NOT_A_PROBABILITY) {
                return getProbability(ptNodeParams.getProbability(), bigramsIt.getProbability());
            }
        }
        return NOT_A_PROBABILITY;
    }
    return getProbability(ptNodeParams.getProbability(), NOT_A_PROBABILITY);
}

void PatriciaTriePolicy::iterateNgramEntries(const WordIdArrayView prevWordIds,
        NgramListener *const listener) const {
    if (prevWordIds.empty()) {
        return;
    }
    const int bigramsPosition = getBigramsPositionOfPtNode(
            getTerminalPtNodePosFromWordId(prevWordIds[0]));
    BinaryDictionaryBigramsIterator bigramsIt(&mBigramListPolicy, bigramsPosition);
    while (bigramsIt.hasNext()) {
        bigramsIt.next();
        listener->onVisitEntry(bigramsIt.getProbability(),
                getWordIdFromTerminalPtNodePos(bigramsIt.getBigramPos()));
    }
}

BinaryDictionaryShortcutIterator PatriciaTriePolicy::getShortcutIterator(const int wordId) const {
    const int shortcutPos = getShortcutPositionOfPtNode(getTerminalPtNodePosFromWordId(wordId));
    return BinaryDictionaryShortcutIterator(&mShortcutListPolicy, shortcutPos);
}

int PatriciaTriePolicy::getShortcutPositionOfPtNode(const int ptNodePos) const {
    if (ptNodePos == NOT_A_DICT_POS) {
        return NOT_A_DICT_POS;
    }
    return mPtNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos).getShortcutPos();
}

int PatriciaTriePolicy::getBigramsPositionOfPtNode(const int ptNodePos) const {
    if (ptNodePos == NOT_A_DICT_POS) {
        return NOT_A_DICT_POS;
    }
    return mPtNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos).getBigramsPos();
}

int PatriciaTriePolicy::createAndGetLeavingChildNode(const DicNode *const dicNode,
        const int ptNodePos, DicNodeVector *childDicNodes) const {
    PatriciaTrieReadingUtils::NodeFlags flags;
    int mergedNodeCodePointCount = 0;
    int mergedNodeCodePoints[MAX_WORD_LENGTH];
    int probability = NOT_A_PROBABILITY;
    int childrenPos = NOT_A_DICT_POS;
    int shortcutPos = NOT_A_DICT_POS;
    int bigramPos = NOT_A_DICT_POS;
    int siblingPos = NOT_A_DICT_POS;
    const int *const codePointTable = mHeaderPolicy.getCodePointTable();
    PatriciaTrieReadingUtils::readPtNodeInfo(mBuffer.data(), ptNodePos, &mShortcutListPolicy,
            &mBigramListPolicy, codePointTable, &flags, &mergedNodeCodePointCount,
            mergedNodeCodePoints, &probability, &childrenPos, &shortcutPos, &bigramPos,
            &siblingPos);
    // Skip PtNodes don't start with Unicode code point because they represent non-word information.
    if (CharUtils::isInUnicodeSpace(mergedNodeCodePoints[0])) {
        const int wordId = PatriciaTrieReadingUtils::isTerminal(flags) ? ptNodePos : NOT_A_WORD_ID;
        childDicNodes->pushLeavingChild(dicNode, childrenPos, wordId,
                CodePointArrayView(mergedNodeCodePoints, mergedNodeCodePointCount));
    }
    return siblingPos;
}

const WordProperty PatriciaTriePolicy::getWordProperty(
        const CodePointArrayView wordCodePoints) const {
    const int wordId = getWordId(wordCodePoints, false /* forceLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) {
        AKLOGE("getWordProperty was called for invalid word.");
        return WordProperty();
    }
    const int ptNodePos = getTerminalPtNodePosFromWordId(wordId);
    const PtNodeParams ptNodeParams =
            mPtNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos);
    // Fetch bigram information.
    std::vector<NgramProperty> ngrams;
    const int bigramListPos = getBigramsPositionOfPtNode(ptNodePos);
    int bigramWord1CodePoints[MAX_WORD_LENGTH];
    BinaryDictionaryBigramsIterator bigramsIt(&mBigramListPolicy, bigramListPos);
    while (bigramsIt.hasNext()) {
        // Fetch the next bigram information and forward the iterator.
        bigramsIt.next();
        // Skip the entry if the entry has been deleted. This never happens for ver2 dicts.
        if (bigramsIt.getBigramPos() != NOT_A_DICT_POS) {
            int word1Probability = NOT_A_PROBABILITY;
            const int word1CodePointCount = getCodePointsAndProbabilityAndReturnCodePointCount(
                    getWordIdFromTerminalPtNodePos(bigramsIt.getBigramPos()), MAX_WORD_LENGTH,
                    bigramWord1CodePoints, &word1Probability);
            const int probability = getProbability(word1Probability, bigramsIt.getProbability());
            ngrams.emplace_back(
                    NgramContext(wordCodePoints.data(), wordCodePoints.size(),
                            ptNodeParams.representsBeginningOfSentence()),
                    CodePointArrayView(bigramWord1CodePoints, word1CodePointCount).toVector(),
                    probability, HistoricalInfo());
        }
    }
    // Fetch shortcut information.
    std::vector<UnigramProperty::ShortcutProperty> shortcuts;
    int shortcutPos = getShortcutPositionOfPtNode(ptNodePos);
    if (shortcutPos != NOT_A_DICT_POS) {
        int shortcutTargetCodePoints[MAX_WORD_LENGTH];
        ShortcutListReadingUtils::getShortcutListSizeAndForwardPointer(mBuffer, &shortcutPos);
        bool hasNext = true;
        while (hasNext) {
            const ShortcutListReadingUtils::ShortcutFlags shortcutFlags =
                    ShortcutListReadingUtils::getFlagsAndForwardPointer(mBuffer, &shortcutPos);
            hasNext = ShortcutListReadingUtils::hasNext(shortcutFlags);
            const int shortcutTargetLength = ShortcutListReadingUtils::readShortcutTarget(
                    mBuffer, MAX_WORD_LENGTH, shortcutTargetCodePoints, &shortcutPos);
            const int shortcutProbability =
                    ShortcutListReadingUtils::getProbabilityFromFlags(shortcutFlags);
            shortcuts.emplace_back(
                    CodePointArrayView(shortcutTargetCodePoints, shortcutTargetLength).toVector(),
                    shortcutProbability);
        }
    }
    const UnigramProperty unigramProperty(ptNodeParams.representsBeginningOfSentence(),
            ptNodeParams.isNotAWord(), ptNodeParams.isPossiblyOffensive(),
            ptNodeParams.getProbability(), HistoricalInfo(), std::move(shortcuts));
    return WordProperty(wordCodePoints.toVector(), unigramProperty, ngrams);
}

int PatriciaTriePolicy::getNextWordAndNextToken(const int token, int *const outCodePoints,
        int *const outCodePointCount) {
    *outCodePointCount = 0;
    if (token == 0) {
        // Start iterating the dictionary.
        mTerminalPtNodePositionsForIteratingWords.clear();
        DynamicPtReadingHelper::TraversePolicyToGetAllTerminalPtNodePositions traversePolicy(
                &mTerminalPtNodePositionsForIteratingWords);
        DynamicPtReadingHelper readingHelper(&mPtNodeReader, &mPtNodeArrayReader);
        readingHelper.initWithPtNodeArrayPos(getRootPosition());
        readingHelper.traverseAllPtNodesInPostorderDepthFirstManner(&traversePolicy);
    }
    const int terminalPtNodePositionsVectorSize =
            static_cast<int>(mTerminalPtNodePositionsForIteratingWords.size());
    if (token < 0 || token >= terminalPtNodePositionsVectorSize) {
        AKLOGE("Given token %d is invalid.", token);
        return 0;
    }
    const int terminalPtNodePos = mTerminalPtNodePositionsForIteratingWords[token];
    *outCodePointCount = getCodePointsAndReturnCodePointCount(
            getWordIdFromTerminalPtNodePos(terminalPtNodePos), MAX_WORD_LENGTH, outCodePoints);
    const int nextToken = token + 1;
    if (nextToken >= terminalPtNodePositionsVectorSize) {
        // All words have been iterated.
        mTerminalPtNodePositionsForIteratingWords.clear();
        return 0;
    }
    return nextToken;
}

int PatriciaTriePolicy::getWordIdFromTerminalPtNodePos(const int ptNodePos) const {
    return ptNodePos == NOT_A_DICT_POS ? NOT_A_WORD_ID : ptNodePos;
}

int PatriciaTriePolicy::getTerminalPtNodePosFromWordId(const int wordId) const {
    return wordId == NOT_A_WORD_ID ? NOT_A_DICT_POS : wordId;
}

bool PatriciaTriePolicy::isValidPos(const int pos) const {
    return pos >= 0 && pos < static_cast<int>(mBuffer.size());
}

} // namespace latinime
