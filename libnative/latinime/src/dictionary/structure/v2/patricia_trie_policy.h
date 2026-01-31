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

#ifndef LATINIME_PATRICIA_TRIE_POLICY_H
#define LATINIME_PATRICIA_TRIE_POLICY_H

#include <cstdint>
#include <vector>

#include "defines.h"
#include "dictionary/header/header_policy.h"
#include "dictionary/interface/dictionary_structure_with_buffer_policy.h"
#include "dictionary/structure/v2/bigram/bigram_list_policy.h"
#include "dictionary/structure/v2/shortcut/shortcut_list_policy.h"
#include "dictionary/structure/v2/ver2_patricia_trie_node_reader.h"
#include "dictionary/structure/v2/ver2_pt_node_array_reader.h"
#include "dictionary/utils/format_utils.h"
#include "dictionary/utils/mmapped_buffer.h"
#include "utils/byte_array_view.h"
#include "utils/int_array_view.h"

namespace latinime {

class DicNode;
class DicNodeVector;

// Word id = Position of a PtNode that represents the word.
// Max supported n-gram is bigram.
class PatriciaTriePolicy : public DictionaryStructureWithBufferPolicy {
 public:
    PatriciaTriePolicy(MmappedBuffer::MmappedBufferPtr mmappedBuffer)
            : mMmappedBuffer(std::move(mmappedBuffer)),
              mHeaderPolicy(mMmappedBuffer->getReadOnlyByteArrayView().data(),
                      FormatUtils::detectFormatVersion(mMmappedBuffer->getReadOnlyByteArrayView())),
              mBuffer(mMmappedBuffer->getReadOnlyByteArrayView().skip(mHeaderPolicy.getSize())),
              mBigramListPolicy(mBuffer), mShortcutListPolicy(mBuffer),
              mPtNodeReader(mBuffer, &mBigramListPolicy, &mShortcutListPolicy,
                      mHeaderPolicy.getCodePointTable()),
              mPtNodeArrayReader(mBuffer), mTerminalPtNodePositionsForIteratingWords(),
              mIsCorrupted(false) {}

    AK_FORCE_INLINE int getRootPosition() const {
        return 0;
    }

    void createAndGetAllChildDicNodes(const DicNode *const dicNode,
            DicNodeVector *const childDicNodes) const;

    int getCodePointsAndReturnCodePointCount(const int wordId, const int maxCodePointCount,
            int *const outCodePoints) const;

    int getWordId(const CodePointArrayView wordCodePoints, const bool forceLowerCaseSearch) const;

    const WordAttributes getWordAttributesInContext(const WordIdArrayView prevWordIds,
            const int wordId, MultiBigramMap *const multiBigramMap) const;

    int getProbability(const int unigramProbability, const int bigramProbability) const;

    int getProbabilityOfWord(const WordIdArrayView prevWordIds, const int wordId) const;

    void iterateNgramEntries(const WordIdArrayView prevWordIds,
            NgramListener *const listener) const;

    BinaryDictionaryShortcutIterator getShortcutIterator(const int wordId) const;

    const DictionaryHeaderStructurePolicy *getHeaderStructurePolicy() const {
        return &mHeaderPolicy;
    }

    bool addUnigramEntry(const CodePointArrayView wordCodePoints,
            const UnigramProperty *const unigramProperty) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: addUnigramEntry() is called for non-updatable dictionary.");
        return false;
    }

    bool removeUnigramEntry(const CodePointArrayView wordCodePoints) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: removeUnigramEntry() is called for non-updatable dictionary.");
        return false;
    }

    bool addNgramEntry(const NgramProperty *const ngramProperty) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: addNgramEntry() is called for non-updatable dictionary.");
        return false;
    }

    bool removeNgramEntry(const NgramContext *const ngramContext,
            const CodePointArrayView wordCodePoints) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: removeNgramEntry() is called for non-updatable dictionary.");
        return false;
    }

    bool updateEntriesForWordWithNgramContext(const NgramContext *const ngramContext,
            const CodePointArrayView wordCodePoints, const bool isValidWord,
            const HistoricalInfo historicalInfo) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: updateEntriesForWordWithNgramContext() is called for non-updatable "
                "dictionary.");
        return false;
    }

    bool flush(const char *const filePath) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: flush() is called for non-updatable dictionary.");
        return false;
    }

    bool flushWithGC(const char *const filePath) {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: flushWithGC() is called for non-updatable dictionary.");
        return false;
    }

    bool needsToRunGC(const bool mindsBlockByGC) const {
        // This method should not be called for non-updatable dictionary.
        AKLOGI("Warning: needsToRunGC() is called for non-updatable dictionary.");
        return false;
    }

    void getProperty(const char *const query, const int queryLength, char *const outResult,
            const int maxResultLength) {
        // getProperty is not supported for this class.
        if (maxResultLength > 0) {
            outResult[0] = '\0';
        }
    }

    const WordProperty getWordProperty(const CodePointArrayView wordCodePoints) const;

    int getNextWordAndNextToken(const int token, int *const outCodePoints,
            int *const outCodePointCount);

    bool isCorrupted() const {
        return mIsCorrupted;
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(PatriciaTriePolicy);

    const MmappedBuffer::MmappedBufferPtr mMmappedBuffer;
    const HeaderPolicy mHeaderPolicy;
    const ReadOnlyByteArrayView mBuffer;
    const BigramListPolicy mBigramListPolicy;
    const ShortcutListPolicy mShortcutListPolicy;
    const Ver2ParticiaTrieNodeReader mPtNodeReader;
    const Ver2PtNodeArrayReader mPtNodeArrayReader;
    std::vector<int> mTerminalPtNodePositionsForIteratingWords;
    mutable bool mIsCorrupted;

    int getCodePointsAndProbabilityAndReturnCodePointCount(const int wordId,
            const int maxCodePointCount, int *const outCodePoints,
            int *const outUnigramProbability) const;
    int getShortcutPositionOfPtNode(const int ptNodePos) const;
    int getBigramsPositionOfPtNode(const int ptNodePos) const;
    int createAndGetLeavingChildNode(const DicNode *const dicNode, const int ptNodePos,
            DicNodeVector *const childDicNodes) const;
    int getWordIdFromTerminalPtNodePos(const int ptNodePos) const;
    int getTerminalPtNodePosFromWordId(const int wordId) const;
    const WordAttributes getWordAttributes(const int probability,
            const PtNodeParams &ptNodeParams) const;
    bool isValidPos(const int pos) const;
};
} // namespace latinime
#endif // LATINIME_PATRICIA_TRIE_POLICY_H
