/*
 * Copyright (C) 2014 The Android Open Source Project
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

#ifndef LATINIME_NGRAM_CONTEXT_H
#define LATINIME_NGRAM_CONTEXT_H

#include <array>

#include "defines.h"
#include "utils/int_array_view.h"

namespace latinime {

class DictionaryStructureWithBufferPolicy;

class NgramContext {
 public:
    // No prev word information.
    NgramContext();
    // Copy constructor to use this class with std::vector and use this class as a return value.
    NgramContext(const NgramContext &ngramContext);
    // Construct from previous words.
    NgramContext(const int prevWordCodePoints[][MAX_WORD_LENGTH],
            const int *const prevWordCodePointCount, const bool *const isBeginningOfSentence,
            const size_t prevWordCount);
    // Construct from a previous word.
    NgramContext(const int *const prevWordCodePoints, const int prevWordCodePointCount,
            const bool isBeginningOfSentence);

    size_t getPrevWordCount() const {
        return mPrevWordCount;
    }
    bool isValid() const;

    template<size_t N>
    const WordIdArrayView getPrevWordIds(
            const DictionaryStructureWithBufferPolicy *const dictStructurePolicy,
            WordIdArray<N> *const prevWordIdBuffer, const bool tryLowerCaseSearch) const {
        for (size_t i = 0; i < std::min(mPrevWordCount, N); ++i) {
            prevWordIdBuffer->at(i) = getWordId(dictStructurePolicy, mPrevWordCodePoints[i],
                    mPrevWordCodePointCount[i], mIsBeginningOfSentence[i], tryLowerCaseSearch);
        }
        return WordIdArrayView::fromArray(*prevWordIdBuffer).limit(mPrevWordCount);
    }

    // n is 1-indexed.
    const CodePointArrayView getNthPrevWordCodePoints(const size_t n) const;
    // n is 1-indexed.
    bool isNthPrevWordBeginningOfSentence(const size_t n) const;

 private:
    DISALLOW_ASSIGNMENT_OPERATOR(NgramContext);

    static int getWordId(const DictionaryStructureWithBufferPolicy *const dictStructurePolicy,
            const int *const wordCodePoints, const int wordCodePointCount,
            const bool isBeginningOfSentence, const bool tryLowerCaseSearch);
    void clear();

    const size_t mPrevWordCount;
    int mPrevWordCodePoints[MAX_PREV_WORD_COUNT_FOR_N_GRAM][MAX_WORD_LENGTH];
    int mPrevWordCodePointCount[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
    bool mIsBeginningOfSentence[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
};
} // namespace latinime
#endif // LATINIME_NGRAM_CONTEXT_H
