/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef LATINIME_BLOOM_FILTER_H
#define LATINIME_BLOOM_FILTER_H

#include <bitset>

#include "defines.h"

namespace latinime {

// This bloom filter is used for optimizing bigram retrieval.
// Execution times with previous word "this" are as follows:
//  without bloom filter (use only hash_map):
//   Total 147792.34 (sum of others 147771.57)
//  with bloom filter:
//   Total 145900.64 (sum of others 145874.30)
//  always read binary dictionary:
//   Total 148603.14 (sum of others 148579.90)
class BloomFilter {
 public:
    BloomFilter() : mFilter() {}

    AK_FORCE_INLINE void setInFilter(const int position) {
        mFilter.set(getIndex(position));
    }

    AK_FORCE_INLINE bool isInFilter(const int position) const {
        return mFilter.test(getIndex(position));
    }

 private:
    DISALLOW_ASSIGNMENT_OPERATOR(BloomFilter);

    AK_FORCE_INLINE size_t getIndex(const int position) const {
        return static_cast<size_t>(position) % BIGRAM_FILTER_MODULO;
    }

    // Size, in bits, of the bloom filter index for bigrams
    // The probability of false positive is (1 - e ** (-kn/m))**k,
    // where k is the number of hash functions, n the number of bigrams, and m the number of
    // bits we can test.
    // At the moment 100 is the maximum number of bigrams for a word with the current main
    // dictionaries, so n = 100. 1024 buckets give us m = 1024.
    // With 1 hash function, our false positive rate is about 9.3%, which should be enough for
    // our uses since we are only using this to increase average performance. For the record,
    // k = 2 gives 3.1% and k = 3 gives 1.6%. With k = 1, making m = 2048 gives 4.8%,
    // and m = 4096 gives 2.4%.
    // This is assigned here because it is used for bitset size.
    // 1021 is the largest prime under 1024.
    static const size_t BIGRAM_FILTER_MODULO = 1021;
    std::bitset<BIGRAM_FILTER_MODULO> mFilter;
};
} // namespace latinime
#endif // LATINIME_BLOOM_FILTER_H
