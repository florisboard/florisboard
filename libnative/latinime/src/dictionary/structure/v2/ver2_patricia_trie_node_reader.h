/*
 * Copyright (C) 2014, The Android Open Source Project
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

#ifndef LATINIME_VER2_PATRICIA_TRIE_NODE_READER_H
#define LATINIME_VER2_PATRICIA_TRIE_NODE_READER_H

#include <cstdint>

#include "defines.h"
#include "dictionary/structure/pt_common/pt_node_params.h"
#include "dictionary/structure/pt_common/pt_node_reader.h"
#include "utils/byte_array_view.h"

namespace latinime {

class DictionaryBigramsStructurePolicy;
class DictionaryShortcutsStructurePolicy;

class Ver2ParticiaTrieNodeReader : public PtNodeReader {
 public:
    Ver2ParticiaTrieNodeReader(const ReadOnlyByteArrayView buffer,
            const DictionaryBigramsStructurePolicy *const bigramPolicy,
            const DictionaryShortcutsStructurePolicy *const shortcutPolicy,
            const int *const codePointTable)
            : mBuffer(buffer), mBigramPolicy(bigramPolicy), mShortcutPolicy(shortcutPolicy),
              mCodePointTable(codePointTable) {}

    virtual const PtNodeParams fetchPtNodeParamsInBufferFromPtNodePos(const int ptNodePos) const;

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(Ver2ParticiaTrieNodeReader);

    const ReadOnlyByteArrayView mBuffer;
    const DictionaryBigramsStructurePolicy *const mBigramPolicy;
    const DictionaryShortcutsStructurePolicy *const mShortcutPolicy;
    const int *const mCodePointTable;
};
} // namespace latinime
#endif /* LATINIME_VER2_PATRICIA_TRIE_NODE_READER_H */
