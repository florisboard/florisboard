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

#ifndef LATINIME_VER4_PATRICIA_TRIE_NODE_WRITER_H
#define LATINIME_VER4_PATRICIA_TRIE_NODE_WRITER_H

#include "defines.h"
#include "dictionary/structure/pt_common/dynamic_pt_reading_helper.h"
#include "dictionary/structure/pt_common/pt_node_params.h"
#include "dictionary/structure/pt_common/pt_node_writer.h"
#include "dictionary/structure/v4/content/probability_entry.h"

namespace latinime {

class BufferWithExtendableBuffer;
class HeaderPolicy;
class Ver4DictBuffers;
class Ver4PatriciaTrieNodeReader;
class Ver4PtNodeArrayReader;
class Ver4ShortcutListPolicy;

/*
 * This class is used for helping to writes nodes of ver4 patricia trie.
 */
class Ver4PatriciaTrieNodeWriter : public PtNodeWriter {
 public:
    Ver4PatriciaTrieNodeWriter(BufferWithExtendableBuffer *const trieBuffer,
            Ver4DictBuffers *const buffers, const PtNodeReader *const ptNodeReader,
            const PtNodeArrayReader *const ptNodeArrayReader,
            Ver4ShortcutListPolicy *const shortcutPolicy)
            : mTrieBuffer(trieBuffer), mBuffers(buffers),
              mReadingHelper(ptNodeReader, ptNodeArrayReader), mShortcutPolicy(shortcutPolicy) {}

    virtual ~Ver4PatriciaTrieNodeWriter() {}

    virtual bool markPtNodeAsDeleted(const PtNodeParams *const toBeUpdatedPtNodeParams);

    virtual bool markPtNodeAsMoved(const PtNodeParams *const toBeUpdatedPtNodeParams,
            const int movedPos, const int bigramLinkedNodePos);

    virtual bool markPtNodeAsWillBecomeNonTerminal(
            const PtNodeParams *const toBeUpdatedPtNodeParams);

    virtual bool updatePtNodeUnigramProperty(const PtNodeParams *const toBeUpdatedPtNodeParams,
            const UnigramProperty *const unigramProperty);

    virtual bool updatePtNodeProbabilityAndGetNeedsToKeepPtNodeAfterGC(
            const PtNodeParams *const toBeUpdatedPtNodeParams, bool *const outNeedsToKeepPtNode);

    virtual bool updateChildrenPosition(const PtNodeParams *const toBeUpdatedPtNodeParams,
            const int newChildrenPosition);

    bool updateTerminalId(const PtNodeParams *const toBeUpdatedPtNodeParams,
            const int newTerminalId);

    virtual bool writePtNodeAndAdvancePosition(const PtNodeParams *const ptNodeParams,
            int *const ptNodeWritingPos);

    virtual bool writeNewTerminalPtNodeAndAdvancePosition(const PtNodeParams *const ptNodeParams,
            const UnigramProperty *const unigramProperty, int *const ptNodeWritingPos);

    virtual bool addNgramEntry(const WordIdArrayView prevWordIds, const int wordId,
            const NgramProperty *const ngramProperty, bool *const outAddedNewEntry);

    virtual bool removeNgramEntry(const WordIdArrayView prevWordIds, const int wordId);

    virtual bool updateAllBigramEntriesAndDeleteUselessEntries(
            const PtNodeParams *const sourcePtNodeParams, int *const outBigramEntryCount);

    virtual bool updateAllPositionFields(const PtNodeParams *const toBeUpdatedPtNodeParams,
            const DictPositionRelocationMap *const dictPositionRelocationMap,
            int *const outBigramEntryCount);

    virtual bool addShortcutTarget(const PtNodeParams *const ptNodeParams,
            const int *const targetCodePoints, const int targetCodePointCount,
            const int shortcutProbability);

 private:
    DISALLOW_COPY_AND_ASSIGN(Ver4PatriciaTrieNodeWriter);

    bool writePtNodeAndGetTerminalIdAndAdvancePosition(
            const PtNodeParams *const ptNodeParams, int *const outTerminalId,
            int *const ptNodeWritingPos);

    bool updatePtNodeFlags(const int ptNodePos, const bool isTerminal, const bool hasMultipleChars);

    static const int CHILDREN_POSITION_FIELD_SIZE;

    BufferWithExtendableBuffer *const mTrieBuffer;
    Ver4DictBuffers *const mBuffers;
    DynamicPtReadingHelper mReadingHelper;
    Ver4ShortcutListPolicy *const mShortcutPolicy;
};
} // namespace latinime
#endif /* LATINIME_VER4_PATRICIA_TRIE_NODE_WRITER_H */
