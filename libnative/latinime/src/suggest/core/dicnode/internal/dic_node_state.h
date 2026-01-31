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

#ifndef LATINIME_DIC_NODE_STATE_H
#define LATINIME_DIC_NODE_STATE_H

#include "defines.h"
#include "suggest/core/dicnode/internal/dic_node_state_input.h"
#include "suggest/core/dicnode/internal/dic_node_state_output.h"
#include "suggest/core/dicnode/internal/dic_node_state_scoring.h"

namespace latinime {

class DicNodeState {
 public:
    DicNodeStateInput mDicNodeStateInput;
    DicNodeStateOutput mDicNodeStateOutput;
    DicNodeStateScoring mDicNodeStateScoring;

    AK_FORCE_INLINE DicNodeState()
            : mDicNodeStateInput(), mDicNodeStateOutput(), mDicNodeStateScoring() {}

    ~DicNodeState() {}

    DicNodeState &operator=(const DicNodeState& src) {
        initByCopy(&src);
        return *this;
    }

    DicNodeState(const DicNodeState& src)
            : mDicNodeStateInput(), mDicNodeStateOutput(), mDicNodeStateScoring() {
        initByCopy(&src);
    }

    // Init for root
    void init() {
        mDicNodeStateInput.init();
        mDicNodeStateOutput.init();
        mDicNodeStateScoring.init();
    }

    // Init with previous word.
    void initAsRootWithPreviousWord(const DicNodeState *prevWordDicNodeState,
            const int prevWordCodePointCount) {
        mDicNodeStateOutput.init(&prevWordDicNodeState->mDicNodeStateOutput);
        mDicNodeStateInput.init(
                &prevWordDicNodeState->mDicNodeStateInput, true /* resetTerminalDiffCost */);
        mDicNodeStateScoring.initByCopy(&prevWordDicNodeState->mDicNodeStateScoring);
    }

    // Init by copy
    AK_FORCE_INLINE void initByCopy(const DicNodeState *const src) {
        mDicNodeStateInput.initByCopy(&src->mDicNodeStateInput);
        mDicNodeStateOutput.initByCopy(&src->mDicNodeStateOutput);
        mDicNodeStateScoring.initByCopy(&src->mDicNodeStateScoring);
    }

    // Init by copy and adding merged node code points.
    void init(const DicNodeState *const src, const uint16_t mergedNodeCodePointCount,
            const int *const mergedNodeCodePoints) {
        initByCopy(src);
        mDicNodeStateOutput.addMergedNodeCodePoints(
                mergedNodeCodePointCount, mergedNodeCodePoints);
    }
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_STATE_H
