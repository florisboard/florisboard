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

#include "dictionary/structure/v4/ver4_pt_node_array_reader.h"

#include "dictionary/structure/pt_common/dynamic_pt_reading_utils.h"
#include "dictionary/structure/pt_common/patricia_trie_reading_utils.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

bool Ver4PtNodeArrayReader::readPtNodeArrayInfoAndReturnIfValid(const int ptNodeArrayPos,
        int *const outPtNodeCount, int *const outFirstPtNodePos) const {
    if (ptNodeArrayPos < 0 || ptNodeArrayPos >= mBuffer->getTailPosition()) {
        // Reading invalid position because of a bug or a broken dictionary.
        AKLOGE("Reading PtNode array info from invalid dictionary position: %d, dict size: %d",
                ptNodeArrayPos, mBuffer->getTailPosition());
        ASSERT(false);
        return false;
    }
    const bool usesAdditionalBuffer = mBuffer->isInAdditionalBuffer(ptNodeArrayPos);
    const uint8_t *const dictBuf = mBuffer->getBuffer(usesAdditionalBuffer);
    int readingPos = ptNodeArrayPos;
    if (usesAdditionalBuffer) {
        readingPos -= mBuffer->getOriginalBufferSize();
    }
    const int ptNodeCountInArray = PatriciaTrieReadingUtils::getPtNodeArraySizeAndAdvancePosition(
            dictBuf, &readingPos);
    if (usesAdditionalBuffer) {
        readingPos += mBuffer->getOriginalBufferSize();
    }
    if (ptNodeCountInArray < 0) {
        AKLOGE("Invalid PtNode count in an array: %d.", ptNodeCountInArray);
        return false;
    }
    *outPtNodeCount = ptNodeCountInArray;
    *outFirstPtNodePos = readingPos;
    return true;
}

bool Ver4PtNodeArrayReader::readForwardLinkAndReturnIfValid(const int forwordLinkPos,
        int *const outNextPtNodeArrayPos) const {
    if (forwordLinkPos < 0 || forwordLinkPos >= mBuffer->getTailPosition()) {
        // Reading invalid position because of bug or broken dictionary.
        AKLOGE("Reading forward link from invalid dictionary position: %d, dict size: %d",
                forwordLinkPos, mBuffer->getTailPosition());
        ASSERT(false);
        return false;
    }
    const bool usesAdditionalBuffer = mBuffer->isInAdditionalBuffer(forwordLinkPos);
    const uint8_t *const dictBuf = mBuffer->getBuffer(usesAdditionalBuffer);
    int readingPos = forwordLinkPos;
    if (usesAdditionalBuffer) {
        readingPos -= mBuffer->getOriginalBufferSize();
    }
    const int nextPtNodeArrayOffset =
            DynamicPtReadingUtils::getForwardLinkPosition(dictBuf, readingPos);
    if (DynamicPtReadingUtils::isValidForwardLinkPosition(nextPtNodeArrayOffset)) {
        *outNextPtNodeArrayPos = forwordLinkPos + nextPtNodeArrayOffset;
    } else {
        *outNextPtNodeArrayPos = NOT_A_DICT_POS;
    }
    return true;
}

} // namespace latinime
