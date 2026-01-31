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

#ifndef LATINIME_VER4_PATRICIA_TRIE_POLICY_H
#define LATINIME_VER4_PATRICIA_TRIE_POLICY_H

#include <vector>

#include "defines.h"
#include "dictionary/header/header_policy.h"
#include "dictionary/interface/dictionary_structure_with_buffer_policy.h"
#include "dictionary/structure/pt_common/dynamic_pt_updating_helper.h"
#include "dictionary/structure/v4/shortcut/ver4_shortcut_list_policy.h"
#include "dictionary/structure/v4/ver4_dict_buffers.h"
#include "dictionary/structure/v4/ver4_patricia_trie_node_reader.h"
#include "dictionary/structure/v4/ver4_patricia_trie_node_writer.h"
#include "dictionary/structure/v4/ver4_patricia_trie_writing_helper.h"
#include "dictionary/structure/v4/ver4_pt_node_array_reader.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "dictionary/utils/entry_counters.h"
#include "utils/int_array_view.h"

namespace latinime {

class DicNode;
class DicNodeVector;

// Word id = Artificial id that is stored in the PtNode looked up by the word.
class Ver4PatriciaTriePolicy : public DictionaryStructureWithBufferPolicy {
 public:
    Ver4PatriciaTriePolicy(Ver4DictBuffers::Ver4DictBuffersPtr buffers)
            : mBuffers(std::move(buffers)), mHeaderPolicy(mBuffers->getHeaderPolicy()),
              mDictBuffer(mBuffers->getWritableTrieBuffer()),
              mShortcutPolicy(mBuffers->getMutableShortcutDictContent(),
                      mBuffers->getTerminalPositionLookupTable()),
              mNodeReader(mDictBuffer), mPtNodeArrayReader(mDictBuffer),
              mNodeWriter(mDictBuffer, mBuffers.get(), &mNodeReader, &mPtNodeArrayReader,
                      &mShortcutPolicy),
              mUpdatingHelper(mDictBuffer, &mNodeReader, &mNodeWriter),
              mWritingHelper(mBuffers.get()),
              mEntryCounters(mHeaderPolicy->getNgramCounts().getCountArray()),
              mTerminalPtNodePositionsForIteratingWords(), mIsCorrupted(false) {};

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

    // TODO: Remove
    int getProbability(const int unigramProbability, const int bigramProbability) const {
        // Not used.
        return NOT_A_PROBABILITY;
    }

    int getProbabilityOfWord(const WordIdArrayView prevWordIds, const int wordId) const;

    void iterateNgramEntries(const WordIdArrayView prevWordIds,
            NgramListener *const listener) const;

    BinaryDictionaryShortcutIterator getShortcutIterator(const int wordId) const;

    const DictionaryHeaderStructurePolicy *getHeaderStructurePolicy() const {
        return mHeaderPolicy;
    }

    bool addUnigramEntry(const CodePointArrayView wordCodePoints,
            const UnigramProperty *const unigramProperty);

    bool removeUnigramEntry(const CodePointArrayView wordCodePoints);

    bool addNgramEntry(const NgramProperty *const ngramProperty);

    bool removeNgramEntry(const NgramContext *const ngramContext,
            const CodePointArrayView wordCodePoints);

    bool updateEntriesForWordWithNgramContext(const NgramContext *const ngramContext,
            const CodePointArrayView wordCodePoints, const bool isValidWord,
            const HistoricalInfo historicalInfo);

    bool flush(const char *const filePath);

    bool flushWithGC(const char *const filePath);

    bool needsToRunGC(const bool mindsBlockByGC) const;

    void getProperty(const char *const query, const int queryLength, char *const outResult,
            const int maxResultLength);

    const WordProperty getWordProperty(const CodePointArrayView wordCodePoints) const;

    int getNextWordAndNextToken(const int token, int *const outCodePoints,
            int *const outCodePointCount);

    bool isCorrupted() const {
        return mIsCorrupted;
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(Ver4PatriciaTriePolicy);

    static const char *const UNIGRAM_COUNT_QUERY;
    static const char *const BIGRAM_COUNT_QUERY;
    static const char *const MAX_UNIGRAM_COUNT_QUERY;
    static const char *const MAX_BIGRAM_COUNT_QUERY;
    // When the dictionary size is near the maximum size, we have to refuse dynamic operations to
    // prevent the dictionary from overflowing.
    static const int MARGIN_TO_REFUSE_DYNAMIC_OPERATIONS;
    static const int MIN_DICT_SIZE_TO_REFUSE_DYNAMIC_OPERATIONS;

    const Ver4DictBuffers::Ver4DictBuffersPtr mBuffers;
    const HeaderPolicy *const mHeaderPolicy;
    BufferWithExtendableBuffer *const mDictBuffer;
    Ver4ShortcutListPolicy mShortcutPolicy;
    Ver4PatriciaTrieNodeReader mNodeReader;
    Ver4PtNodeArrayReader mPtNodeArrayReader;
    Ver4PatriciaTrieNodeWriter mNodeWriter;
    DynamicPtUpdatingHelper mUpdatingHelper;
    Ver4PatriciaTrieWritingHelper mWritingHelper;
    MutableEntryCounters mEntryCounters;
    std::vector<int> mTerminalPtNodePositionsForIteratingWords;
    mutable bool mIsCorrupted;

    int getShortcutPositionOfWord(const int wordId) const;
};
} // namespace latinime
#endif // LATINIME_VER4_PATRICIA_TRIE_POLICY_H
