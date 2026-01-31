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

#include "dictionary/structure/v2/ver2_patricia_trie_node_reader.h"

#include "dictionary/structure/pt_common/patricia_trie_reading_utils.h"

namespace latinime {

const PtNodeParams Ver2ParticiaTrieNodeReader::fetchPtNodeParamsInBufferFromPtNodePos(
        const int ptNodePos) const {
    if (ptNodePos < 0 || ptNodePos >= static_cast<int>(mBuffer.size())) {
        // Reading invalid position because of bug or broken dictionary.
        AKLOGE("Fetching PtNode info from invalid dictionary position: %d, dictionary size: %zd",
                ptNodePos, mBuffer.size());
        ASSERT(false);
        return PtNodeParams();
    }
    PatriciaTrieReadingUtils::NodeFlags flags;
    int mergedNodeCodePointCount = 0;
    int mergedNodeCodePoints[MAX_WORD_LENGTH];
    int probability = NOT_A_PROBABILITY;
    int childrenPos = NOT_A_DICT_POS;
    int shortcutPos = NOT_A_DICT_POS;
    int bigramPos = NOT_A_DICT_POS;
    int siblingPos = NOT_A_DICT_POS;
    PatriciaTrieReadingUtils::readPtNodeInfo(mBuffer.data(), ptNodePos, mShortcutPolicy,
            mBigramPolicy, mCodePointTable, &flags, &mergedNodeCodePointCount, mergedNodeCodePoints,
            &probability, &childrenPos, &shortcutPos, &bigramPos, &siblingPos);
    if (mergedNodeCodePointCount <= 0) {
        AKLOGE("Empty PtNode is not allowed. Code point count: %d", mergedNodeCodePointCount);
        ASSERT(false);
        return PtNodeParams();
    }
    return PtNodeParams(ptNodePos, flags, mergedNodeCodePointCount, mergedNodeCodePoints,
            probability, childrenPos, shortcutPos, bigramPos, siblingPos);
}

}
