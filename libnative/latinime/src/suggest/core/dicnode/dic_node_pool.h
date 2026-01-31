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

#ifndef LATINIME_DIC_NODE_POOL_H
#define LATINIME_DIC_NODE_POOL_H

#include <deque>
#include <unordered_set>
#include <vector>

#include "defines.h"
#include "suggest/core/dicnode/dic_node.h"

namespace latinime {

class DicNodePool {
 public:
    explicit DicNodePool(const int capacity) : mDicNodes(), mPooledDicNodes() {
        reset(capacity);
    }

    void reset(const int capacity) {
        if (capacity == static_cast<int>(mDicNodes.size())
                && capacity == static_cast<int>(mPooledDicNodes.size())) {
            // No need to reset.
            return;
        }
        mDicNodes.resize(capacity);
        mDicNodes.shrink_to_fit();
        mPooledDicNodes.clear();
        for (auto &dicNode : mDicNodes) {
            mPooledDicNodes.emplace_back(&dicNode);
        }
    }

    // Get a DicNode instance from the pool. The instance has to be returned by returnInstance().
    DicNode *getInstance() {
        if (mPooledDicNodes.empty()) {
            return nullptr;
        }
        DicNode *const dicNode = mPooledDicNodes.back();
        mPooledDicNodes.pop_back();
        return dicNode;
    }

    // Return an instance that has been removed from the pool by getInstance() to the pool. The
    // instance must not be used after returning without getInstance().
    void placeBackInstance(DicNode *dicNode) {
        mPooledDicNodes.emplace_back(dicNode);
    }

    void dump() const {
        AKLOGI("\n\n\n\n\n===========================");
        std::unordered_set<const DicNode*> usedDicNodes;
        for (const auto &dicNode : mDicNodes) {
            usedDicNodes.insert(&dicNode);
        }
        for (const auto &dicNodePtr : mPooledDicNodes) {
            usedDicNodes.erase(dicNodePtr);
        }
        for (const auto &usedDicNodePtr : usedDicNodes) {
            usedDicNodePtr->dump("DIC_NODE_POOL: ");
        }
        AKLOGI("===========================\n\n\n\n\n");
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(DicNodePool);

    std::vector<DicNode> mDicNodes;
    std::deque<DicNode*> mPooledDicNodes;
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_POOL_H
