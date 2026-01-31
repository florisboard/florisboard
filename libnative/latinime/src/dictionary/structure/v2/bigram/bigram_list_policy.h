/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef LATINIME_BIGRAM_LIST_POLICY_H
#define LATINIME_BIGRAM_LIST_POLICY_H

#include <cstdint>

#include "defines.h"
#include "dictionary/interface/dictionary_bigrams_structure_policy.h"
#include "dictionary/structure/pt_common/bigram/bigram_list_read_write_utils.h"
#include "utils/byte_array_view.h"

namespace latinime {

class BigramListPolicy : public DictionaryBigramsStructurePolicy {
 public:
    BigramListPolicy(const ReadOnlyByteArrayView buffer) : mBuffer(buffer) {}

    ~BigramListPolicy() {}

    void getNextBigram(int *const outBigramPos, int *const outProbability, bool *const outHasNext,
            int *const pos) const {
        BigramListReadWriteUtils::BigramFlags flags;
        if (!BigramListReadWriteUtils::getBigramEntryPropertiesAndAdvancePosition(mBuffer, &flags,
                outBigramPos, pos)) {
            AKLOGE("Cannot read bigram entry. bufSize: %zd, pos: %d. ", mBuffer.size(), *pos);
            *outProbability = NOT_A_PROBABILITY;
            *outHasNext = false;
            return;
        }
        *outProbability = BigramListReadWriteUtils::getProbabilityFromFlags(flags);
        *outHasNext = BigramListReadWriteUtils::hasNext(flags);
    }

    bool skipAllBigrams(int *const pos) const {
        return BigramListReadWriteUtils::skipExistingBigrams(mBuffer, pos);
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(BigramListPolicy);

    const ReadOnlyByteArrayView mBuffer;
};
} // namespace latinime
#endif // LATINIME_BIGRAM_LIST_POLICY_H
