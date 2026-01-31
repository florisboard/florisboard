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

#ifndef LATINIME_NGRAM_UTILS_H
#define LATINIME_NGRAM_UTILS_H

#include "defines.h"

namespace latinime {

enum class NgramType : int {
    Unigram = 0,
    Bigram = 1,
    Trigram = 2,
    Quadgram = 3,
    NotANgramType = -1,
};

namespace AllNgramTypes {
// Use anonymous namespace to avoid ODR (One Definition Rule) violation.
namespace {

const NgramType ASCENDING[] = {
   NgramType::Unigram, NgramType::Bigram, NgramType::Trigram
};

const NgramType DESCENDING[] = {
   NgramType::Trigram, NgramType::Bigram, NgramType::Unigram
};

}  // namespace
}  // namespace AllNgramTypes

class NgramUtils final {
 public:
    static AK_FORCE_INLINE NgramType getNgramTypeFromWordCount(const int wordCount) {
        // Max supported ngram is (MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1)-gram.
        if (wordCount <= 0 || wordCount > MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1) {
            return NgramType::NotANgramType;
        }
        // Convert word count to 0-origin enum value.
        return static_cast<NgramType>(wordCount - 1);
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(NgramUtils);

};
}
#endif /* LATINIME_NGRAM_UTILS_H */
