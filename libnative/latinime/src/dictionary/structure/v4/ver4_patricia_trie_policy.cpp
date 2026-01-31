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

#include "dictionary/structure/v4/ver4_patricia_trie_policy.h"

#include <array>
#include <vector>

#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/dicnode/dic_node_vector.h"
#include "dictionary/interface/ngram_listener.h"
#include "dictionary/property/ngram_context.h"
#include "dictionary/property/ngram_property.h"
#include "dictionary/property/unigram_property.h"
#include "dictionary/property/word_property.h"
#include "dictionary/structure/pt_common/dynamic_pt_reading_helper.h"
#include "dictionary/structure/v4/ver4_patricia_trie_node_reader.h"
#include "dictionary/utils/forgetting_curve_utils.h"
#include "dictionary/utils/multi_bigram_map.h"
#include "dictionary/utils/probability_utils.h"
#include "utils/ngram_utils.h"

namespace latinime {

// Note that there are corresponding definitions in Java side in BinaryDictionaryTests and
// BinaryDictionaryDecayingTests.
const char *const Ver4PatriciaTriePolicy::UNIGRAM_COUNT_QUERY = "UNIGRAM_COUNT";
const char *const Ver4PatriciaTriePolicy::BIGRAM_COUNT_QUERY = "BIGRAM_COUNT";
const char *const Ver4PatriciaTriePolicy::MAX_UNIGRAM_COUNT_QUERY = "MAX_UNIGRAM_COUNT";
const char *const Ver4PatriciaTriePolicy::MAX_BIGRAM_COUNT_QUERY = "MAX_BIGRAM_COUNT";
const int Ver4PatriciaTriePolicy::MARGIN_TO_REFUSE_DYNAMIC_OPERATIONS = 1024;
const int Ver4PatriciaTriePolicy::MIN_DICT_SIZE_TO_REFUSE_DYNAMIC_OPERATIONS =
        Ver4DictConstants::MAX_DICTIONARY_SIZE - MARGIN_TO_REFUSE_DYNAMIC_OPERATIONS;

void Ver4PatriciaTriePolicy::createAndGetAllChildDicNodes(const DicNode *const dicNode,
        DicNodeVector *const childDicNodes) const {
    if (!dicNode->hasChildren()) {
        return;
    }
    DynamicPtReadingHelper readingHelper(&mNodeReader, &mPtNodeArrayReader);
    readingHelper.initWithPtNodeArrayPos(dicNode->getChildrenPtNodeArrayPos());
    while (!readingHelper.isEnd()) {
        const PtNodeParams ptNodeParams = readingHelper.getPtNodeParams();
        if (!ptNodeParams.isValid()) {
            break;
        }
        const bool isTerminal = ptNodeParams.isTerminal() && !ptNodeParams.isDeleted();
        const int wordId = isTerminal ? ptNodeParams.getTerminalId() : NOT_A_WORD_ID;
        childDicNodes->pushLeavingChild(dicNode, ptNodeParams.getChildrenPos(),
                wordId, ptNodeParams.getCodePointArrayView());
        readingHelper.readNextSiblingNode(ptNodeParams);
    }
    if (readingHelper.isError()) {
        mIsCorrupted = true;
        AKLOGE("Dictionary reading error in createAndGetAllChildDicNodes().");
    }
}

int Ver4PatriciaTriePolicy::getCodePointsAndReturnCodePointCount(const int wordId,
        const int maxCodePointCount, int *const outCodePoints) const {
    DynamicPtReadingHelper readingHelper(&mNodeReader, &mPtNodeArrayReader);
    const int ptNodePos =
            mBuffers->getTerminalPositionLookupTable()->getTerminalPtNodePosition(wordId);
    readingHelper.initWithPtNodePos(ptNodePos);
    const int codePointCount =  readingHelper.getCodePointsAndReturnCodePointCount(
            maxCodePointCount, outCodePoints);
    if (readingHelper.isError()) {
        mIsCorrupted = true;
        AKLOGE("Dictionary reading error in getCodePointsAndProbabilityAndReturnCodePointCount().");
    }
    return codePointCount;
}

int Ver4PatriciaTriePolicy::getWordId(const CodePointArrayView wordCodePoints,
        const bool forceLowerCaseSearch) const {
    DynamicPtReadingHelper readingHelper(&mNodeReader, &mPtNodeArrayReader);
    readingHelper.initWithPtNodeArrayPos(getRootPosition());
    const int ptNodePos = readingHelper.getTerminalPtNodePositionOfWord(wordCodePoints.data(),
            wordCodePoints.size(), forceLowerCaseSearch);
    if (readingHelper.isError()) {
        mIsCorrupted = true;
        AKLOGE("Dictionary reading error in createAndGetAllChildDicNodes().");
    }
    if (ptNodePos == NOT_A_DICT_POS) {
        return NOT_A_WORD_ID;
    }
    const PtNodeParams ptNodeParams = mNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos);
    if (ptNodeParams.isDeleted()) {
        return NOT_A_WORD_ID;
    }
    return ptNodeParams.getTerminalId();
}

const WordAttributes Ver4PatriciaTriePolicy::getWordAttributesInContext(
        const WordIdArrayView prevWordIds, const int wordId,
        MultiBigramMap *const multiBigramMap) const {
    if (wordId == NOT_A_WORD_ID) {
        return WordAttributes();
    }
    return mBuffers->getLanguageModelDictContent()->getWordAttributes(prevWordIds, wordId,
            false /* mustMatchAllPrevWords */, mHeaderPolicy);
}

int Ver4PatriciaTriePolicy::getProbabilityOfWord(const WordIdArrayView prevWordIds,
        const int wordId) const {
    if (wordId == NOT_A_WORD_ID || prevWordIds.contains(NOT_A_WORD_ID)) {
        return NOT_A_PROBABILITY;
    }
    const WordAttributes wordAttributes =
            mBuffers->getLanguageModelDictContent()->getWordAttributes(prevWordIds, wordId,
                    true /* mustMatchAllPrevWords */, mHeaderPolicy);
    if (wordAttributes.isBlacklisted() || wordAttributes.isNotAWord()) {
        return NOT_A_PROBABILITY;
    }
    return wordAttributes.getProbability();
}

BinaryDictionaryShortcutIterator Ver4PatriciaTriePolicy::getShortcutIterator(
        const int wordId) const {
    const int shortcutPos = getShortcutPositionOfWord(wordId);
    return BinaryDictionaryShortcutIterator(&mShortcutPolicy, shortcutPos);
}

void Ver4PatriciaTriePolicy::iterateNgramEntries(const WordIdArrayView prevWordIds,
        NgramListener *const listener) const {
    if (prevWordIds.empty()) {
        return;
    }
    const auto languageModelDictContent = mBuffers->getLanguageModelDictContent();
    for (size_t i = 1; i <= prevWordIds.size(); ++i) {
        for (const auto& entry : languageModelDictContent->getProbabilityEntries(
                prevWordIds.limit(i))) {
            const ProbabilityEntry &probabilityEntry = entry.getProbabilityEntry();
            if (!probabilityEntry.isValid()) {
                continue;
            }
            int probability = NOT_A_PROBABILITY;
            if (probabilityEntry.hasHistoricalInfo()) {
                // TODO: Quit checking count here.
                // If count <= 1, the word can be an invaild word. The actual probability should
                // be checked using getWordAttributesInContext() in onVisitEntry().
                probability = probabilityEntry.getHistoricalInfo()->getCount() <= 1 ?
                        NOT_A_PROBABILITY : 0;
            } else {
                probability = probabilityEntry.getProbability();
            }
            listener->onVisitEntry(probability, entry.getWordId());
        }
    }
}

int Ver4PatriciaTriePolicy::getShortcutPositionOfWord(const int wordId) const {
    if (wordId == NOT_A_WORD_ID) {
        return NOT_A_DICT_POS;
    }
    const int ptNodePos =
            mBuffers->getTerminalPositionLookupTable()->getTerminalPtNodePosition(wordId);
    const PtNodeParams ptNodeParams(mNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos));
    if (ptNodeParams.isDeleted()) {
        return NOT_A_DICT_POS;
    }
    return mBuffers->getShortcutDictContent()->getShortcutListHeadPos(
            ptNodeParams.getTerminalId());
}

bool Ver4PatriciaTriePolicy::addUnigramEntry(const CodePointArrayView wordCodePoints,
        const UnigramProperty *const unigramProperty) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: addUnigramEntry() is called for non-updatable dictionary.");
        return false;
    }
    if (mDictBuffer->getTailPosition() >= MIN_DICT_SIZE_TO_REFUSE_DYNAMIC_OPERATIONS) {
        AKLOGE("The dictionary is too large to dynamically update. Dictionary size: %d",
                mDictBuffer->getTailPosition());
        return false;
    }
    if (wordCodePoints.size() > MAX_WORD_LENGTH) {
        AKLOGE("The word is too long to insert to the dictionary, length: %zd",
                wordCodePoints.size());
        return false;
    }
    for (const auto &shortcut : unigramProperty->getShortcuts()) {
        if (shortcut.getTargetCodePoints()->size() > MAX_WORD_LENGTH) {
            AKLOGE("One of shortcut targets is too long to insert to the dictionary, length: %zd",
                    shortcut.getTargetCodePoints()->size());
            return false;
        }
    }
    DynamicPtReadingHelper readingHelper(&mNodeReader, &mPtNodeArrayReader);
    readingHelper.initWithPtNodeArrayPos(getRootPosition());
    bool addedNewUnigram = false;
    int codePointsToAdd[MAX_WORD_LENGTH];
    int codePointCountToAdd = wordCodePoints.size();
    memmove(codePointsToAdd, wordCodePoints.data(), sizeof(int) * codePointCountToAdd);
    if (unigramProperty->representsBeginningOfSentence()) {
        codePointCountToAdd = CharUtils::attachBeginningOfSentenceMarker(codePointsToAdd,
                codePointCountToAdd, MAX_WORD_LENGTH);
    }
    if (codePointCountToAdd <= 0) {
        return false;
    }
    const CodePointArrayView codePointArrayView(codePointsToAdd, codePointCountToAdd);
    if (mUpdatingHelper.addUnigramWord(&readingHelper, codePointArrayView, unigramProperty,
            &addedNewUnigram)) {
        if (addedNewUnigram && !unigramProperty->representsBeginningOfSentence()) {
            mEntryCounters.incrementNgramCount(NgramType::Unigram);
        }
        if (unigramProperty->getShortcuts().size() > 0) {
            // Add shortcut target.
            const int wordId = getWordId(codePointArrayView, false /* forceLowerCaseSearch */);
            if (wordId == NOT_A_WORD_ID) {
                AKLOGE("Cannot find word id to add shortcut target.");
                return false;
            }
            const int wordPos =
                    mBuffers->getTerminalPositionLookupTable()->getTerminalPtNodePosition(wordId);
            for (const auto &shortcut : unigramProperty->getShortcuts()) {
                if (!mUpdatingHelper.addShortcutTarget(wordPos,
                        CodePointArrayView(*shortcut.getTargetCodePoints()),
                        shortcut.getProbability())) {
                    AKLOGE("Cannot add new shortcut target. PtNodePos: %d, length: %zd, "
                            "probability: %d", wordPos, shortcut.getTargetCodePoints()->size(),
                            shortcut.getProbability());
                    return false;
                }
            }
        }
        return true;
    } else {
        return false;
    }
}

bool Ver4PatriciaTriePolicy::removeUnigramEntry(const CodePointArrayView wordCodePoints) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: removeUnigramEntry() is called for non-updatable dictionary.");
        return false;
    }
    const int wordId = getWordId(wordCodePoints, false /* forceLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) {
        return false;
    }
    const int ptNodePos =
            mBuffers->getTerminalPositionLookupTable()->getTerminalPtNodePosition(wordId);
    const PtNodeParams ptNodeParams = mNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(ptNodePos);
    if (!mNodeWriter.markPtNodeAsDeleted(&ptNodeParams)) {
        AKLOGE("Cannot remove unigram. ptNodePos: %d", ptNodePos);
        return false;
    }
    if (!mBuffers->getMutableLanguageModelDictContent()->removeProbabilityEntry(wordId)) {
        return false;
    }
    if (!ptNodeParams.representsNonWordInfo()) {
        mEntryCounters.decrementNgramCount(NgramType::Unigram);
    }
    return true;
}

bool Ver4PatriciaTriePolicy::addNgramEntry(const NgramProperty *const ngramProperty) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: addNgramEntry() is called for non-updatable dictionary.");
        return false;
    }
    if (mDictBuffer->getTailPosition() >= MIN_DICT_SIZE_TO_REFUSE_DYNAMIC_OPERATIONS) {
        AKLOGE("The dictionary is too large to dynamically update. Dictionary size: %d",
                mDictBuffer->getTailPosition());
        return false;
    }
    const NgramContext *const ngramContext = ngramProperty->getNgramContext();
    if (!ngramContext->isValid()) {
        AKLOGE("Ngram context is not valid for adding n-gram entry to the dictionary.");
        return false;
    }
    if (ngramProperty->getTargetCodePoints()->size() > MAX_WORD_LENGTH) {
        AKLOGE("The word is too long to insert the ngram to the dictionary. "
                "length: %zd", ngramProperty->getTargetCodePoints()->size());
        return false;
    }
    WordIdArray<MAX_PREV_WORD_COUNT_FOR_N_GRAM> prevWordIdArray;
    const WordIdArrayView prevWordIds = ngramContext->getPrevWordIds(this, &prevWordIdArray,
            false /* tryLowerCaseSearch */);
    if (prevWordIds.empty()) {
        return false;
    }
    for (size_t i = 0; i < prevWordIds.size(); ++i) {
        if (prevWordIds[i] != NOT_A_WORD_ID) {
            continue;
        }
        if (!ngramContext->isNthPrevWordBeginningOfSentence(i + 1 /* n */)) {
            return false;
        }
        const UnigramProperty beginningOfSentenceUnigramProperty(
                true /* representsBeginningOfSentence */, true /* isNotAWord */,
                false /* isBlacklisted */, false /* isPossiblyOffensive */,
                MAX_PROBABILITY /* probability */, HistoricalInfo());
        if (!addUnigramEntry(ngramContext->getNthPrevWordCodePoints(1 /* n */),
                &beginningOfSentenceUnigramProperty)) {
            AKLOGE("Cannot add unigram entry for the beginning-of-sentence.");
            return false;
        }
        // Refresh word ids.
        ngramContext->getPrevWordIds(this, &prevWordIdArray, false /* tryLowerCaseSearch */);
    }
    const int wordId = getWordId(CodePointArrayView(*ngramProperty->getTargetCodePoints()),
            false /* forceLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) {
        return false;
    }
    bool addedNewEntry = false;
    if (mNodeWriter.addNgramEntry(prevWordIds, wordId, ngramProperty, &addedNewEntry)) {
        if (addedNewEntry) {
            mEntryCounters.incrementNgramCount(
                    NgramUtils::getNgramTypeFromWordCount(prevWordIds.size() + 1));
        }
        return true;
    } else {
        return false;
    }
}

bool Ver4PatriciaTriePolicy::removeNgramEntry(const NgramContext *const ngramContext,
        const CodePointArrayView wordCodePoints) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: removeNgramEntry() is called for non-updatable dictionary.");
        return false;
    }
    if (mDictBuffer->getTailPosition() >= MIN_DICT_SIZE_TO_REFUSE_DYNAMIC_OPERATIONS) {
        AKLOGE("The dictionary is too large to dynamically update. Dictionary size: %d",
                mDictBuffer->getTailPosition());
        return false;
    }
    if (!ngramContext->isValid()) {
        AKLOGE("Ngram context is not valid for removing n-gram entry form the dictionary.");
        return false;
    }
    if (wordCodePoints.size() > MAX_WORD_LENGTH) {
        AKLOGE("word is too long to remove n-gram entry form the dictionary. length: %zd",
                wordCodePoints.size());
    }
    WordIdArray<MAX_PREV_WORD_COUNT_FOR_N_GRAM> prevWordIdArray;
    const WordIdArrayView prevWordIds = ngramContext->getPrevWordIds(this, &prevWordIdArray,
            false /* tryLowerCaseSerch */);
    if (prevWordIds.empty() || prevWordIds.contains(NOT_A_WORD_ID)) {
        return false;
    }
    const int wordId = getWordId(wordCodePoints, false /* forceLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) {
        return false;
    }
    if (mNodeWriter.removeNgramEntry(prevWordIds, wordId)) {
        mEntryCounters.decrementNgramCount(
                NgramUtils::getNgramTypeFromWordCount(prevWordIds.size() + 1));
        return true;
    } else {
        return false;
    }
}

bool Ver4PatriciaTriePolicy::updateEntriesForWordWithNgramContext(
        const NgramContext *const ngramContext, const CodePointArrayView wordCodePoints,
        const bool isValidWord, const HistoricalInfo historicalInfo) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: updateEntriesForWordWithNgramContext() is called for non-updatable "
                "dictionary.");
        return false;
    }
    const bool updateAsAValidWord = ngramContext->isNthPrevWordBeginningOfSentence(1 /* n */) ?
            false : isValidWord;
    int wordId = getWordId(wordCodePoints, false /* tryLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) {
        // The word is not in the dictionary.
        const UnigramProperty unigramProperty(false /* representsBeginningOfSentence */,
                false /* isNotAWord */, false /* isBlacklisted */, false /* isPossiblyOffensive */,
                NOT_A_PROBABILITY, HistoricalInfo(historicalInfo.getTimestamp(), 0 /* level */,
                0 /* count */));
        if (!addUnigramEntry(wordCodePoints, &unigramProperty)) {
            AKLOGE("Cannot add unigarm entry in updateEntriesForWordWithNgramContext().");
            return false;
        }
        if (!isValidWord) {
            return true;
        }
        wordId = getWordId(wordCodePoints, false /* tryLowerCaseSearch */);
    }

    WordIdArray<MAX_PREV_WORD_COUNT_FOR_N_GRAM> prevWordIdArray;
    const WordIdArrayView prevWordIds = ngramContext->getPrevWordIds(this, &prevWordIdArray,
            false /* tryLowerCaseSearch */);
    if (ngramContext->isNthPrevWordBeginningOfSentence(1 /* n */)) {
        if (prevWordIds.firstOrDefault(NOT_A_WORD_ID) == NOT_A_WORD_ID) {
            const UnigramProperty beginningOfSentenceUnigramProperty(
                    true /* representsBeginningOfSentence */,
                    true /* isNotAWord */, false /* isPossiblyOffensive */, NOT_A_PROBABILITY,
                    HistoricalInfo(historicalInfo.getTimestamp(), 0 /* level */, 0 /* count */));
            if (!addUnigramEntry(ngramContext->getNthPrevWordCodePoints(1 /* n */),
                    &beginningOfSentenceUnigramProperty)) {
                AKLOGE("Cannot add BoS entry in updateEntriesForWordWithNgramContext().");
                return false;
            }
            // Refresh word ids.
            ngramContext->getPrevWordIds(this, &prevWordIdArray, false /* tryLowerCaseSearch */);
        }
        // Update entries for beginning of sentence.
        if (!mBuffers->getMutableLanguageModelDictContent()->updateAllEntriesOnInputWord(
                prevWordIds.skip(1 /* n */), prevWordIds[0], true /* isVaild */, historicalInfo,
                mHeaderPolicy, &mEntryCounters)) {
            return false;
        }
    }
    if (!mBuffers->getMutableLanguageModelDictContent()->updateAllEntriesOnInputWord(prevWordIds,
            wordId, updateAsAValidWord, historicalInfo, mHeaderPolicy, &mEntryCounters)) {
        return false;
    }
    return true;
}

bool Ver4PatriciaTriePolicy::flush(const char *const filePath) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: flush() is called for non-updatable dictionary. filePath: %s", filePath);
        return false;
    }
    if (!mWritingHelper.writeToDictFile(filePath, mEntryCounters.getEntryCounts())) {
        AKLOGE("Cannot flush the dictionary to file.");
        mIsCorrupted = true;
        return false;
    }
    return true;
}

bool Ver4PatriciaTriePolicy::flushWithGC(const char *const filePath) {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: flushWithGC() is called for non-updatable dictionary.");
        return false;
    }
    if (!mWritingHelper.writeToDictFileWithGC(getRootPosition(), filePath)) {
        AKLOGE("Cannot flush the dictionary to file with GC.");
        mIsCorrupted = true;
        return false;
    }
    return true;
}

bool Ver4PatriciaTriePolicy::needsToRunGC(const bool mindsBlockByGC) const {
    if (!mBuffers->isUpdatable()) {
        AKLOGI("Warning: needsToRunGC() is called for non-updatable dictionary.");
        return false;
    }
    if (mBuffers->isNearSizeLimit()) {
        // Additional buffer size is near the limit.
        return true;
    } else if (mHeaderPolicy->getExtendedRegionSize() + mDictBuffer->getUsedAdditionalBufferSize()
            > Ver4DictConstants::MAX_DICT_EXTENDED_REGION_SIZE) {
        // Total extended region size of the trie exceeds the limit.
        return true;
    } else if (mDictBuffer->getTailPosition() >= MIN_DICT_SIZE_TO_REFUSE_DYNAMIC_OPERATIONS
            && mDictBuffer->getUsedAdditionalBufferSize() > 0) {
        // Needs to reduce dictionary size.
        return true;
    } else if (mHeaderPolicy->isDecayingDict()) {
        return ForgettingCurveUtils::needsToDecay(mindsBlockByGC, mEntryCounters.getEntryCounts(),
                mHeaderPolicy);
    }
    return false;
}

void Ver4PatriciaTriePolicy::getProperty(const char *const query, const int queryLength,
        char *const outResult, const int maxResultLength) {
    const int compareLength = queryLength + 1 /* terminator */;
    if (strncmp(query, UNIGRAM_COUNT_QUERY, compareLength) == 0) {
        snprintf(outResult, maxResultLength, "%d",
                mEntryCounters.getNgramCount(NgramType::Unigram));
    } else if (strncmp(query, BIGRAM_COUNT_QUERY, compareLength) == 0) {
        snprintf(outResult, maxResultLength, "%d", mEntryCounters.getNgramCount(NgramType::Bigram));
    } else if (strncmp(query, MAX_UNIGRAM_COUNT_QUERY, compareLength) == 0) {
        snprintf(outResult, maxResultLength, "%d",
                mHeaderPolicy->isDecayingDict() ?
                        ForgettingCurveUtils::getEntryCountHardLimit(
                                mHeaderPolicy->getMaxNgramCounts().getNgramCount(
                                        NgramType::Unigram)) :
                        static_cast<int>(Ver4DictConstants::MAX_DICTIONARY_SIZE));
    } else if (strncmp(query, MAX_BIGRAM_COUNT_QUERY, compareLength) == 0) {
        snprintf(outResult, maxResultLength, "%d",
                mHeaderPolicy->isDecayingDict() ?
                        ForgettingCurveUtils::getEntryCountHardLimit(
                                mHeaderPolicy->getMaxNgramCounts().getNgramCount(
                                        NgramType::Bigram)) :
                        static_cast<int>(Ver4DictConstants::MAX_DICTIONARY_SIZE));
    }
}

const WordProperty Ver4PatriciaTriePolicy::getWordProperty(
        const CodePointArrayView wordCodePoints) const {
    const int wordId = getWordId(wordCodePoints, false /* forceLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) {
        AKLOGE("getWordProperty is called for invalid word.");
        return WordProperty();
    }
    const LanguageModelDictContent *const languageModelDictContent =
            mBuffers->getLanguageModelDictContent();
    // Fetch ngram information.
    std::vector<NgramProperty> ngrams;
    int ngramTargetCodePoints[MAX_WORD_LENGTH];
    int ngramPrevWordsCodePoints[MAX_PREV_WORD_COUNT_FOR_N_GRAM][MAX_WORD_LENGTH];
    int ngramPrevWordsCodePointCount[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
    bool ngramPrevWordIsBeginningOfSentense[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
    for (const auto& entry : languageModelDictContent->exportAllNgramEntriesRelatedToWord(
            mHeaderPolicy, wordId)) {
        const int codePointCount = getCodePointsAndReturnCodePointCount(entry.getTargetWordId(),
                MAX_WORD_LENGTH, ngramTargetCodePoints);
        const WordIdArrayView prevWordIds = entry.getPrevWordIds();
        for (size_t i = 0; i < prevWordIds.size(); ++i) {
            ngramPrevWordsCodePointCount[i] = getCodePointsAndReturnCodePointCount(prevWordIds[i],
                       MAX_WORD_LENGTH, ngramPrevWordsCodePoints[i]);
            ngramPrevWordIsBeginningOfSentense[i] = languageModelDictContent->getProbabilityEntry(
                    prevWordIds[i]).representsBeginningOfSentence();
            if (ngramPrevWordIsBeginningOfSentense[i]) {
                ngramPrevWordsCodePointCount[i] = CharUtils::removeBeginningOfSentenceMarker(
                        ngramPrevWordsCodePoints[i], ngramPrevWordsCodePointCount[i]);
            }
        }
        const NgramContext ngramContext(ngramPrevWordsCodePoints, ngramPrevWordsCodePointCount,
                ngramPrevWordIsBeginningOfSentense, prevWordIds.size());
        const ProbabilityEntry ngramProbabilityEntry = entry.getProbabilityEntry();
        const HistoricalInfo *const historicalInfo = ngramProbabilityEntry.getHistoricalInfo();
        // TODO: Output flags in WordAttributes.
        ngrams.emplace_back(ngramContext,
                CodePointArrayView(ngramTargetCodePoints, codePointCount).toVector(),
                entry.getWordAttributes().getProbability(), *historicalInfo);
    }
    // Fetch shortcut information.
    std::vector<UnigramProperty::ShortcutProperty> shortcuts;
    int shortcutPos = getShortcutPositionOfWord(wordId);
    if (shortcutPos != NOT_A_DICT_POS) {
        int shortcutTarget[MAX_WORD_LENGTH];
        const ShortcutDictContent *const shortcutDictContent =
                mBuffers->getShortcutDictContent();
        bool hasNext = true;
        while (hasNext) {
            int shortcutTargetLength = 0;
            int shortcutProbability = NOT_A_PROBABILITY;
            shortcutDictContent->getShortcutEntryAndAdvancePosition(MAX_WORD_LENGTH, shortcutTarget,
                    &shortcutTargetLength, &shortcutProbability, &hasNext, &shortcutPos);
            shortcuts.emplace_back(
                    CodePointArrayView(shortcutTarget, shortcutTargetLength).toVector(),
                    shortcutProbability);
        }
    }
    const WordAttributes wordAttributes = languageModelDictContent->getWordAttributes(
            WordIdArrayView(), wordId, true /* mustMatchAllPrevWords */, mHeaderPolicy);
    const ProbabilityEntry probabilityEntry = languageModelDictContent->getProbabilityEntry(wordId);
    const HistoricalInfo *const historicalInfo = probabilityEntry.getHistoricalInfo();
    const UnigramProperty unigramProperty(probabilityEntry.representsBeginningOfSentence(),
            wordAttributes.isNotAWord(), wordAttributes.isBlacklisted(),
            wordAttributes.isPossiblyOffensive(), wordAttributes.getProbability(),
            *historicalInfo, std::move(shortcuts));
    return WordProperty(wordCodePoints.toVector(), unigramProperty, ngrams);
}

int Ver4PatriciaTriePolicy::getNextWordAndNextToken(const int token, int *const outCodePoints,
        int *const outCodePointCount) {
    *outCodePointCount = 0;
    if (token == 0) {
        mTerminalPtNodePositionsForIteratingWords.clear();
        DynamicPtReadingHelper::TraversePolicyToGetAllTerminalPtNodePositions traversePolicy(
                &mTerminalPtNodePositionsForIteratingWords);
        DynamicPtReadingHelper readingHelper(&mNodeReader, &mPtNodeArrayReader);
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
    const PtNodeParams ptNodeParams =
            mNodeReader.fetchPtNodeParamsInBufferFromPtNodePos(terminalPtNodePos);
    *outCodePointCount = getCodePointsAndReturnCodePointCount(ptNodeParams.getTerminalId(),
            MAX_WORD_LENGTH, outCodePoints);
    const int nextToken = token + 1;
    if (nextToken >= terminalPtNodePositionsVectorSize) {
        // All words have been iterated.
        mTerminalPtNodePositionsForIteratingWords.clear();
        return 0;
    }
    return nextToken;
}

} // namespace latinime
