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

#ifndef LATINIME_DIC_NODE_STATE_OUTPUT_H
#define LATINIME_DIC_NODE_STATE_OUTPUT_H

#include <algorithm>
#include <cstdint>
#include <cstring> // for memmove()

#include "defines.h"

namespace latinime {

// Class to have information to be output. This can contain previous words when the suggestion
// is a multi-word suggestion.
class DicNodeStateOutput {
 public:
    DicNodeStateOutput()
            : mOutputtedCodePointCount(0), mCurrentWordStart(0), mPrevWordCount(0),
              mPrevWordsLength(0), mPrevWordStart(0), mSecondWordFirstInputIndex(NOT_AN_INDEX) {}

    ~DicNodeStateOutput() {}

    // Init for root
    void init() {
        mOutputtedCodePointCount = 0;
        mCurrentWordStart = 0;
        mOutputCodePoints[0] = 0;
        mPrevWordCount = 0;
        mPrevWordsLength = 0;
        mPrevWordStart = 0;
        mSecondWordFirstInputIndex = NOT_AN_INDEX;
    }

    // Init for next word.
    void init(const DicNodeStateOutput *const stateOutput) {
        mOutputtedCodePointCount = stateOutput->mOutputtedCodePointCount + 1;
        memmove(mOutputCodePoints, stateOutput->mOutputCodePoints,
                stateOutput->mOutputtedCodePointCount * sizeof(mOutputCodePoints[0]));
        mOutputCodePoints[stateOutput->mOutputtedCodePointCount] = KEYCODE_SPACE;
        mCurrentWordStart = stateOutput->mOutputtedCodePointCount + 1;
        mPrevWordCount = std::min(static_cast<int16_t>(stateOutput->mPrevWordCount + 1),
                static_cast<int16_t>(MAX_RESULTS));
        mPrevWordsLength = stateOutput->mOutputtedCodePointCount + 1;
        mPrevWordStart = stateOutput->mCurrentWordStart;
        mSecondWordFirstInputIndex = stateOutput->mSecondWordFirstInputIndex;
    }

    void initByCopy(const DicNodeStateOutput *const stateOutput) {
        memmove(mOutputCodePoints, stateOutput->mOutputCodePoints,
                stateOutput->mOutputtedCodePointCount * sizeof(mOutputCodePoints[0]));
        mOutputtedCodePointCount = stateOutput->mOutputtedCodePointCount;
        if (mOutputtedCodePointCount < MAX_WORD_LENGTH) {
            mOutputCodePoints[mOutputtedCodePointCount] = 0;
        }
        mCurrentWordStart = stateOutput->mCurrentWordStart;
        mPrevWordCount = stateOutput->mPrevWordCount;
        mPrevWordsLength = stateOutput->mPrevWordsLength;
        mPrevWordStart = stateOutput->mPrevWordStart;
        mSecondWordFirstInputIndex = stateOutput->mSecondWordFirstInputIndex;
    }

    void addMergedNodeCodePoints(const uint16_t mergedNodeCodePointCount,
            const int *const mergedNodeCodePoints) {
        if (mergedNodeCodePoints) {
            const int additionalCodePointCount = std::min(
                    static_cast<int>(mergedNodeCodePointCount),
                    MAX_WORD_LENGTH - mOutputtedCodePointCount);
            memmove(&mOutputCodePoints[mOutputtedCodePointCount], mergedNodeCodePoints,
                    additionalCodePointCount * sizeof(mOutputCodePoints[0]));
            mOutputtedCodePointCount = static_cast<uint16_t>(
                    mOutputtedCodePointCount + additionalCodePointCount);
            if (mOutputtedCodePointCount < MAX_WORD_LENGTH) {
                mOutputCodePoints[mOutputtedCodePointCount] = 0;
            }
        }
    }

    int getCurrentWordCodePointAt(const int index) const {
        return mOutputCodePoints[mCurrentWordStart + index];
    }

    const int *getCodePointBuf() const {
        return mOutputCodePoints;
    }

    void setSecondWordFirstInputIndex(const int inputIndex) {
        mSecondWordFirstInputIndex = inputIndex;
    }

    int getSecondWordFirstInputIndex() const {
        return mSecondWordFirstInputIndex;
    }

    // TODO: remove
    int16_t getPrevWordsLength() const {
        return mPrevWordsLength;
    }

    int16_t getPrevWordCount() const {
        return mPrevWordCount;
    }

    int16_t getPrevWordStart() const {
        return mPrevWordStart;
    }

    int getOutputCodePointAt(const int id) const {
        return mOutputCodePoints[id];
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(DicNodeStateOutput);

    // When the DicNode represents "this is a pen":
    // mOutputtedCodePointCount is 13, which is total code point count of "this is a pen" including
    // spaces.
    // mCurrentWordStart indicates the head of "pen", thus it is 10.
    // This contains 3 previous words, "this", "is" and "a"; thus, mPrevWordCount is 3.
    // mPrevWordsLength is length of "this is a ", which is 10.
    // mPrevWordStart is the start index of "a"; thus, it is 8.
    // mSecondWordFirstInputIndex is the first input index of "is".

    uint16_t mOutputtedCodePointCount;
    int mOutputCodePoints[MAX_WORD_LENGTH];
    int16_t mCurrentWordStart;
    // Previous word count in mOutputCodePoints.
    int16_t mPrevWordCount;
    // Total length of previous words in mOutputCodePoints. This is being used by the algorithm
    // that may want to look at the previous word information.
    int16_t mPrevWordsLength;
    // Start index of the previous word in mOutputCodePoints. This is being used for auto commit.
    int16_t mPrevWordStart;
    int mSecondWordFirstInputIndex;
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_STATE_OUTPUT_H
