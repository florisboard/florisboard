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

#include "dictionary/property/ngram_context.h"

#include "dictionary/interface/dictionary_structure_with_buffer_policy.h"
#include "utils/char_utils.h"

namespace latinime {

NgramContext::NgramContext() : mPrevWordCount(0) {}

NgramContext::NgramContext(const NgramContext &ngramContext)
        : mPrevWordCount(ngramContext.mPrevWordCount) {
    for (size_t i = 0; i < mPrevWordCount; ++i) {
        mPrevWordCodePointCount[i] = ngramContext.mPrevWordCodePointCount[i];
        memmove(mPrevWordCodePoints[i], ngramContext.mPrevWordCodePoints[i],
                sizeof(mPrevWordCodePoints[i][0]) * mPrevWordCodePointCount[i]);
        mIsBeginningOfSentence[i] = ngramContext.mIsBeginningOfSentence[i];
    }
}

NgramContext::NgramContext(const int prevWordCodePoints[][MAX_WORD_LENGTH],
        const int *const prevWordCodePointCount, const bool *const isBeginningOfSentence,
        const size_t prevWordCount)
        : mPrevWordCount(std::min(NELEMS(mPrevWordCodePoints), prevWordCount)) {
    clear();
    for (size_t i = 0; i < mPrevWordCount; ++i) {
        if (prevWordCodePointCount[i] < 0 || prevWordCodePointCount[i] > MAX_WORD_LENGTH) {
            continue;
        }
        memmove(mPrevWordCodePoints[i], prevWordCodePoints[i],
                sizeof(mPrevWordCodePoints[i][0]) * prevWordCodePointCount[i]);
        mPrevWordCodePointCount[i] = prevWordCodePointCount[i];
        mIsBeginningOfSentence[i] = isBeginningOfSentence[i];
    }
}

NgramContext::NgramContext(const int *const prevWordCodePoints, const int prevWordCodePointCount,
        const bool isBeginningOfSentence) : mPrevWordCount(1) {
    clear();
    if (prevWordCodePointCount > MAX_WORD_LENGTH || !prevWordCodePoints) {
        return;
    }
    memmove(mPrevWordCodePoints[0], prevWordCodePoints,
            sizeof(mPrevWordCodePoints[0][0]) * prevWordCodePointCount);
    mPrevWordCodePointCount[0] = prevWordCodePointCount;
    mIsBeginningOfSentence[0] = isBeginningOfSentence;
}

bool NgramContext::isValid() const {
    if (mPrevWordCodePointCount[0] > 0) {
        return true;
    }
    if (mIsBeginningOfSentence[0]) {
        return true;
    }
    return false;
}

const CodePointArrayView NgramContext::getNthPrevWordCodePoints(const size_t n) const {
    if (n <= 0 || n > mPrevWordCount) {
        return CodePointArrayView();
    }
    return CodePointArrayView(mPrevWordCodePoints[n - 1], mPrevWordCodePointCount[n - 1]);
}

bool NgramContext::isNthPrevWordBeginningOfSentence(const size_t n) const {
    if (n <= 0 || n > mPrevWordCount) {
        return false;
    }
    return mIsBeginningOfSentence[n - 1];
}

/* static */ int NgramContext::getWordId(
        const DictionaryStructureWithBufferPolicy *const dictStructurePolicy,
        const int *const wordCodePoints, const int wordCodePointCount,
        const bool isBeginningOfSentence, const bool tryLowerCaseSearch) {
    if (!dictStructurePolicy || !wordCodePoints || wordCodePointCount > MAX_WORD_LENGTH) {
        return NOT_A_WORD_ID;
    }
    int codePoints[MAX_WORD_LENGTH];
    int codePointCount = wordCodePointCount;
    memmove(codePoints, wordCodePoints, sizeof(int) * codePointCount);
    if (isBeginningOfSentence) {
        codePointCount = CharUtils::attachBeginningOfSentenceMarker(codePoints, codePointCount,
                MAX_WORD_LENGTH);
        if (codePointCount <= 0) {
            return NOT_A_WORD_ID;
        }
    }
    const CodePointArrayView codePointArrayView(codePoints, codePointCount);
    const int wordId = dictStructurePolicy->getWordId(codePointArrayView,
            false /* forceLowerCaseSearch */);
    if (wordId != NOT_A_WORD_ID || !tryLowerCaseSearch) {
        // Return the id when when the word was found or doesn't try lower case search.
        return wordId;
    }
    // Check bigrams for lower-cased previous word if original was not found. Useful for
    // auto-capitalized words like "The [current_word]".
    return dictStructurePolicy->getWordId(codePointArrayView, true /* forceLowerCaseSearch */);
}

void NgramContext::clear() {
    for (size_t i = 0; i < NELEMS(mPrevWordCodePoints); ++i) {
        mPrevWordCodePointCount[i] = 0;
        mIsBeginningOfSentence[i] = false;
    }
}
} // namespace latinime
