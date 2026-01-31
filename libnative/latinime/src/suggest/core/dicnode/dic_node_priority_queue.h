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

#ifndef LATINIME_DIC_NODE_PRIORITY_QUEUE_H
#define LATINIME_DIC_NODE_PRIORITY_QUEUE_H

#include <algorithm>
#include <queue>
#include <vector>

#include "defines.h"
#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/dicnode/dic_node_pool.h"

namespace latinime {

class DicNodePriorityQueue {
 public:
    AK_FORCE_INLINE explicit DicNodePriorityQueue(const int capacity)
            : mMaxSize(capacity), mDicNodesQueue(), mDicNodePool(capacity) {
        clear();
    }

    // Non virtual inline destructor -- never inherit this class
    AK_FORCE_INLINE ~DicNodePriorityQueue() {}

    AK_FORCE_INLINE int getSize() const {
        return static_cast<int>(mDicNodesQueue.size());
    }

    AK_FORCE_INLINE int getMaxSize() const {
        return mMaxSize;
    }

    AK_FORCE_INLINE void setMaxSize(const int maxSize) {
        mMaxSize = maxSize;
    }

    AK_FORCE_INLINE void clear() {
        clearAndResize(mMaxSize);
    }

    AK_FORCE_INLINE void clearAndResize(const int maxSize) {
        mMaxSize = maxSize;
        while (!mDicNodesQueue.empty()) {
            mDicNodesQueue.pop();
        }
        mDicNodePool.reset(mMaxSize + 1);
    }

    AK_FORCE_INLINE void copyPush(const DicNode *const dicNode) {
        DicNode *const pooledDicNode = newDicNode(dicNode);
        if (!pooledDicNode) {
            return;
        }
        if (getSize() < mMaxSize) {
            mDicNodesQueue.push(pooledDicNode);
            return;
        }
        if (betterThanWorstDicNode(pooledDicNode)) {
            mDicNodePool.placeBackInstance(mDicNodesQueue.top());
            mDicNodesQueue.pop();
            mDicNodesQueue.push(pooledDicNode);
            return;
        }
        mDicNodePool.placeBackInstance(pooledDicNode);
    }

    AK_FORCE_INLINE void copyPop(DicNode *const dest) {
        if (mDicNodesQueue.empty()) {
            ASSERT(false);
            return;
        }
        DicNode *node = mDicNodesQueue.top();
        if (dest) {
            DicNodeUtils::initByCopy(node, dest);
        }
        mDicNodePool.placeBackInstance(node);
        mDicNodesQueue.pop();
    }

    AK_FORCE_INLINE void dump() {
        mDicNodePool.dump();
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(DicNodePriorityQueue);

    AK_FORCE_INLINE static bool compareDicNode(const DicNode *const left,
            const DicNode *const right) {
        return left->compare(right);
    }

    struct DicNodeComparator {
        bool operator ()(const DicNode *left, const DicNode *right) const {
            return compareDicNode(left, right);
        }
    };

    typedef std::priority_queue<DicNode *, std::vector<DicNode *>, DicNodeComparator> DicNodesQueue;
    int mMaxSize;
    DicNodesQueue mDicNodesQueue;
    DicNodePool mDicNodePool;

    AK_FORCE_INLINE bool betterThanWorstDicNode(const DicNode *const dicNode) const {
        DicNode *worstNode = mDicNodesQueue.top();
        if (!worstNode) {
            return true;
        }
        return compareDicNode(dicNode, worstNode);
    }

    AK_FORCE_INLINE DicNode *newDicNode(const DicNode *const dicNode) {
        DicNode *newNode = mDicNodePool.getInstance();
        if (newNode) {
            DicNodeUtils::initByCopy(dicNode, newNode);
        }
        return newNode;
    }
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_PRIORITY_QUEUE_H
