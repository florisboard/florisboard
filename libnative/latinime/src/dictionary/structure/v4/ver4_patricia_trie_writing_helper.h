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

#ifndef LATINIME_VER4_PATRICIA_TRIE_WRITING_HELPER_H
#define LATINIME_VER4_PATRICIA_TRIE_WRITING_HELPER_H

#include "defines.h"
#include "dictionary/structure/pt_common/dynamic_pt_gc_event_listeners.h"
#include "dictionary/structure/v4/content/terminal_position_lookup_table.h"
#include "dictionary/utils/entry_counters.h"

namespace latinime {

class HeaderPolicy;
class Ver4DictBuffers;
class Ver4PatriciaTrieNodeReader;
class Ver4PatriciaTrieNodeWriter;

class Ver4PatriciaTrieWritingHelper {
 public:
    Ver4PatriciaTrieWritingHelper(Ver4DictBuffers *const buffers)
            : mBuffers(buffers) {}

    bool writeToDictFile(const char *const dictDirPath, const EntryCounts &entryCounts) const;

    // This method cannot be const because the original dictionary buffer will be updated to detect
    // useless PtNodes during GC.
    bool writeToDictFileWithGC(const int rootPtNodeArrayPos, const char *const dictDirPath);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(Ver4PatriciaTrieWritingHelper);

    class TraversePolicyToUpdateAllPtNodeFlagsAndTerminalIds
            : public DynamicPtReadingHelper::TraversingEventListener {
     public:
        TraversePolicyToUpdateAllPtNodeFlagsAndTerminalIds(
                Ver4PatriciaTrieNodeWriter *const ptNodeWriter,
                const TerminalPositionLookupTable::TerminalIdMap *const terminalIdMap)
                : mPtNodeWriter(ptNodeWriter), mTerminalIdMap(terminalIdMap) {}

        bool onAscend() { return true; }

        bool onDescend(const int ptNodeArrayPos) { return true; }

        bool onReadingPtNodeArrayTail() { return true; }

        bool onVisitingPtNode(const PtNodeParams *const ptNodeParams);

     private:
        DISALLOW_IMPLICIT_CONSTRUCTORS(TraversePolicyToUpdateAllPtNodeFlagsAndTerminalIds);

        Ver4PatriciaTrieNodeWriter *const mPtNodeWriter;
        const TerminalPositionLookupTable::TerminalIdMap *const mTerminalIdMap;
    };

    bool runGC(const int rootPtNodeArrayPos, const HeaderPolicy *const headerPolicy,
            Ver4DictBuffers *const buffersToWrite, MutableEntryCounters *const outEntryCounters);

    Ver4DictBuffers *const mBuffers;
};
} // namespace latinime

#endif /* LATINIME_VER4_PATRICIA_TRIE_WRITING_HELPER_H */
