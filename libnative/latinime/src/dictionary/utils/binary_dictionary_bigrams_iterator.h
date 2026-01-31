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

#ifndef LATINIME_BINARY_DICTIONARY_BIGRAMS_ITERATOR_H
#define LATINIME_BINARY_DICTIONARY_BIGRAMS_ITERATOR_H

#include "defines.h"
#include "dictionary/interface/dictionary_bigrams_structure_policy.h"

namespace latinime {

class BinaryDictionaryBigramsIterator {
 public:
    // Empty iterator.
    BinaryDictionaryBigramsIterator()
           : mBigramsStructurePolicy(nullptr), mPos(NOT_A_DICT_POS),
             mBigramPos(NOT_A_DICT_POS), mProbability(NOT_A_PROBABILITY), mHasNext(false) {}

    BinaryDictionaryBigramsIterator(
            const DictionaryBigramsStructurePolicy *const bigramsStructurePolicy, const int pos)
            : mBigramsStructurePolicy(bigramsStructurePolicy), mPos(pos),
              mBigramPos(NOT_A_DICT_POS), mProbability(NOT_A_PROBABILITY),
              mHasNext(pos != NOT_A_DICT_POS) {}

    BinaryDictionaryBigramsIterator(BinaryDictionaryBigramsIterator &&bigramsIterator) noexcept
            : mBigramsStructurePolicy(bigramsIterator.mBigramsStructurePolicy),
              mPos(bigramsIterator.mPos), mBigramPos(bigramsIterator.mBigramPos),
              mProbability(bigramsIterator.mProbability), mHasNext(bigramsIterator.mHasNext) {}

    AK_FORCE_INLINE bool hasNext() const {
        return mHasNext;
    }

    AK_FORCE_INLINE void next() {
        mBigramsStructurePolicy->getNextBigram(&mBigramPos, &mProbability, &mHasNext, &mPos);
    }

    AK_FORCE_INLINE int getProbability() const {
        return mProbability;
    }

    AK_FORCE_INLINE int getBigramPos() const {
        return mBigramPos;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(BinaryDictionaryBigramsIterator);

    const DictionaryBigramsStructurePolicy *const mBigramsStructurePolicy;
    int mPos;
    int mBigramPos;
    int mProbability;
    bool mHasNext;
};
} // namespace latinime
#endif // LATINIME_BINARY_DICTIONARY_BIGRAMS_ITERATOR_H
