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

#ifndef LATINIME_DIC_NODE_STATE_INPUT_H
#define LATINIME_DIC_NODE_STATE_INPUT_H

#include "defines.h"

namespace latinime {

// TODO: Have a .cpp for this class
class DicNodeStateInput {
 public:
    DicNodeStateInput() {}
    ~DicNodeStateInput() {}

    void init() {
        for (int i = 0; i < MAX_POINTER_COUNT_G; i++) {
            // TODO: The initial value for mInputIndex should be -1?
            //mInputIndex[i] = i == 0 ? 0 : -1;
            mInputIndex[i] = 0;
            mPrevCodePoint[i] = NOT_A_CODE_POINT;
            mTerminalDiffCost[i] = static_cast<float>(MAX_VALUE_FOR_WEIGHTING);
        }
    }

    void init(const DicNodeStateInput *const src, const bool resetTerminalDiffCost) {
        for (int i = 0; i < MAX_POINTER_COUNT_G; i++) {
             mInputIndex[i] = src->mInputIndex[i];
             mPrevCodePoint[i] = src->mPrevCodePoint[i];
             mTerminalDiffCost[i] = resetTerminalDiffCost ?
                     static_cast<float>(MAX_VALUE_FOR_WEIGHTING) : src->mTerminalDiffCost[i];
         }
    }

    void updateInputIndexG(const int pointerId, const int inputIndex,
            const int prevCodePoint, const float terminalDiffCost, const float rawLength) {
        mInputIndex[pointerId] = inputIndex;
        mPrevCodePoint[pointerId] = prevCodePoint;
        mTerminalDiffCost[pointerId] = terminalDiffCost;
    }

    void initByCopy(const DicNodeStateInput *const src) {
        init(src, false);
    }

    // For transposition
    void setPrevCodePoint(const int pointerId, const int c) {
        mPrevCodePoint[pointerId] = c;
    }

    void forwardInputIndex(const int pointerId, const int val) {
        if (mInputIndex[pointerId] < 0) {
            mInputIndex[pointerId] = val;
        } else {
            mInputIndex[pointerId] = mInputIndex[pointerId] + val;
        }
    }

    int getInputIndex(const int pointerId) const {
        // when "inputIndex" exceeds "inputSize", auto-completion needs to be done
        return mInputIndex[pointerId];
    }

    int getPrevCodePoint(const int pointerId) const {
        return mPrevCodePoint[pointerId];
    }

    float getTerminalDiffCost(const int pointerId) const {
        return mTerminalDiffCost[pointerId];
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(DicNodeStateInput);

    int mInputIndex[MAX_POINTER_COUNT_G];
    int mPrevCodePoint[MAX_POINTER_COUNT_G];
    float mTerminalDiffCost[MAX_POINTER_COUNT_G];
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_STATE_INPUT_H
