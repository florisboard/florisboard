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

#ifndef LATINIME_DIC_NODE_VECTOR_H
#define LATINIME_DIC_NODE_VECTOR_H

#include <vector>

#include "defines.h"
#include "suggest/core/dicnode/dic_node.h"
#include "utils/int_array_view.h"

namespace latinime {

class DicNodeVector {
 public:
#ifdef FLAG_DBG
    // 0 will introduce resizing the vector.
    static const int DEFAULT_NODES_SIZE_FOR_OPTIMIZATION = 0;
#else
    static const int DEFAULT_NODES_SIZE_FOR_OPTIMIZATION = 60;
#endif
    AK_FORCE_INLINE DicNodeVector() : mDicNodes(), mLock(false) {}

    // Specify the capacity of the vector
    AK_FORCE_INLINE DicNodeVector(const int size) : mDicNodes(), mLock(false) {
        mDicNodes.reserve(size);
    }

    // Non virtual inline destructor -- never inherit this class
    AK_FORCE_INLINE ~DicNodeVector() {}

    AK_FORCE_INLINE void clear() {
        mDicNodes.clear();
        mLock = false;
    }

    int getSizeAndLock() {
        mLock = true;
        return static_cast<int>(mDicNodes.size());
    }

    void pushPassingChild(const DicNode *dicNode) {
        ASSERT(!mLock);
        mDicNodes.emplace_back();
        mDicNodes.back().initAsPassingChild(dicNode);
    }

    void pushLeavingChild(const DicNode *const dicNode, const int childrenPtNodeArrayPos,
            const int wordId, const CodePointArrayView mergedCodePoints) {
        ASSERT(!mLock);
        mDicNodes.emplace_back();
        mDicNodes.back().initAsChild(dicNode, childrenPtNodeArrayPos, wordId, mergedCodePoints);
    }

    DicNode *operator[](const int id) {
        ASSERT(id < static_cast<int>(mDicNodes.size()));
        return &mDicNodes[id];
    }

    DicNode *front() {
        ASSERT(1 <= static_cast<int>(mDicNodes.size()));
        return &mDicNodes.front();
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(DicNodeVector);
    std::vector<DicNode> mDicNodes;
    bool mLock;
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_VECTOR_H
