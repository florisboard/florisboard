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

#ifndef LATINIME_VER4_PATRICIA_TRIE_NODE_READER_H
#define LATINIME_VER4_PATRICIA_TRIE_NODE_READER_H

#include "defines.h"
#include "dictionary/structure/pt_common/pt_node_params.h"
#include "dictionary/structure/pt_common/pt_node_reader.h"

namespace latinime {

class BufferWithExtendableBuffer;
class HeaderPolicy;
class LanguageModelDictContent;

/*
 * This class is used for helping to read nodes of ver4 patricia trie. This class handles moved
 * node and reads node attributes.
 */
class Ver4PatriciaTrieNodeReader : public PtNodeReader {
 public:
    explicit Ver4PatriciaTrieNodeReader(const BufferWithExtendableBuffer *const buffer)
            : mBuffer(buffer) {}

    ~Ver4PatriciaTrieNodeReader() {}

    virtual const PtNodeParams fetchPtNodeParamsInBufferFromPtNodePos(const int ptNodePos) const {
        return fetchPtNodeInfoFromBufferAndProcessMovedPtNode(ptNodePos,
                NOT_A_DICT_POS /* siblingNodePos */);
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(Ver4PatriciaTrieNodeReader);

    const BufferWithExtendableBuffer *const mBuffer;

    const PtNodeParams fetchPtNodeInfoFromBufferAndProcessMovedPtNode(const int ptNodePos,
            const int siblingNodePos) const;
};
} // namespace latinime
#endif /* LATINIME_VER4_PATRICIA_TRIE_NODE_READER_H */
