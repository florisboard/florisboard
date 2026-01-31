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

#ifndef LATINIME_DIC_NODES_CACHE_H
#define LATINIME_DIC_NODES_CACHE_H

#include <algorithm>

#include "defines.h"
#include "suggest/core/dicnode/dic_node_priority_queue.h"

namespace latinime {

class DicNode;

/**
 * Class for controlling dicNode search priority queue and lexicon trie traversal.
 */
class DicNodesCache {
 public:
    AK_FORCE_INLINE explicit DicNodesCache(const bool usesLargeCapacityCache)
            : mUsesLargeCapacityCache(usesLargeCapacityCache),
              mDicNodePriorityQueue0(getCacheCapacity()),
              mDicNodePriorityQueue1(getCacheCapacity()),
              mDicNodePriorityQueue2(getCacheCapacity()),
              mDicNodePriorityQueueForTerminal(MAX_RESULTS),
              mActiveDicNodes(&mDicNodePriorityQueue0),
              mNextActiveDicNodes(&mDicNodePriorityQueue1),
              mCachedDicNodesForContinuousSuggestion(&mDicNodePriorityQueue2),
              mTerminalDicNodes(&mDicNodePriorityQueueForTerminal),
              mInputIndex(0), mLastCachedInputIndex(0) {}

    AK_FORCE_INLINE virtual ~DicNodesCache() {}

    AK_FORCE_INLINE void reset(const int nextActiveSize, const int terminalSize) {
        mInputIndex = 0;
        mLastCachedInputIndex = 0;
        // The size of current active DicNode queue doesn't have to be changed.
        mActiveDicNodes->clear();
        // nextActiveSize is used to limit the next iteration's active DicNode size.
        const int nextActiveSizeFittingToTheCapacity = std::min(nextActiveSize, getCacheCapacity());
        mNextActiveDicNodes->clearAndResize(nextActiveSizeFittingToTheCapacity);
        mTerminalDicNodes->clearAndResize(terminalSize);
        // The size of cached DicNode queue doesn't have to be changed.
        mCachedDicNodesForContinuousSuggestion->clear();
    }

    AK_FORCE_INLINE void continueSearch() {
        resetTemporaryCaches();
        restoreActiveDicNodesFromCache();
    }

    AK_FORCE_INLINE void advanceActiveDicNodes() {
        if (DEBUG_DICT) {
            AKLOGI("Advance active %d nodes.", mNextActiveDicNodes->getSize());
        }
        if (DEBUG_DICT_FULL) {
            mNextActiveDicNodes->dump();
        }
        mNextActiveDicNodes =
                moveNodesAndReturnReusableEmptyQueue(mNextActiveDicNodes, &mActiveDicNodes);
    }

    int activeSize() const { return mActiveDicNodes->getSize(); }
    int terminalSize() const { return mTerminalDicNodes->getSize(); }
    bool isLookAheadCorrectionInputIndex(const int inputIndex) const {
        return inputIndex == mInputIndex - 1;
    }
    void advanceInputIndex(const int inputSize) {
        if (mInputIndex < inputSize) {
            mInputIndex++;
        }
    }

    AK_FORCE_INLINE void copyPushTerminal(DicNode *dicNode) {
        mTerminalDicNodes->copyPush(dicNode);
    }

    AK_FORCE_INLINE void copyPushActive(DicNode *dicNode) {
        mActiveDicNodes->copyPush(dicNode);
    }

    AK_FORCE_INLINE void copyPushContinue(DicNode *dicNode) {
        mCachedDicNodesForContinuousSuggestion->copyPush(dicNode);
    }

    AK_FORCE_INLINE void copyPushNextActive(DicNode *dicNode) {
        mNextActiveDicNodes->copyPush(dicNode);
    }

    void popTerminal(DicNode *dest) {
        mTerminalDicNodes->copyPop(dest);
    }

    void popActive(DicNode *dest) {
        mActiveDicNodes->copyPop(dest);
    }

    bool hasCachedDicNodesForContinuousSuggestion() const {
        return mCachedDicNodesForContinuousSuggestion
                && mCachedDicNodesForContinuousSuggestion->getSize() > 0;
    }

    AK_FORCE_INLINE bool isCacheBorderForTyping(const int inputSize) const {
        // TODO: Move this variable to header
        static const int CACHE_BACK_LENGTH = 3;
        const int cacheInputIndex = inputSize - CACHE_BACK_LENGTH;
        const bool shouldCache = (cacheInputIndex == mInputIndex)
                && (cacheInputIndex != mLastCachedInputIndex);
        return shouldCache;
    }

    AK_FORCE_INLINE void updateLastCachedInputIndex() {
        mLastCachedInputIndex = mInputIndex;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(DicNodesCache);

    AK_FORCE_INLINE void restoreActiveDicNodesFromCache() {
        if (DEBUG_DICT) {
            AKLOGI("Restore %d nodes. inputIndex = %d.",
                    mCachedDicNodesForContinuousSuggestion->getSize(), mLastCachedInputIndex);
        }
        if (DEBUG_DICT_FULL || DEBUG_CACHE) {
            mCachedDicNodesForContinuousSuggestion->dump();
        }
        mInputIndex = mLastCachedInputIndex;
        mCachedDicNodesForContinuousSuggestion = moveNodesAndReturnReusableEmptyQueue(
                mCachedDicNodesForContinuousSuggestion, &mActiveDicNodes);
    }

    AK_FORCE_INLINE static DicNodePriorityQueue *moveNodesAndReturnReusableEmptyQueue(
            DicNodePriorityQueue *src, DicNodePriorityQueue **dest) {
        const int srcMaxSize = src->getMaxSize();
        const int destMaxSize = (*dest)->getMaxSize();
        DicNodePriorityQueue *tmp = *dest;
        *dest = src;
        (*dest)->setMaxSize(destMaxSize);
        tmp->clearAndResize(srcMaxSize);
        return tmp;
    }

    AK_FORCE_INLINE int getCacheCapacity() const {
        return mUsesLargeCapacityCache ?
                LARGE_PRIORITY_QUEUE_CAPACITY : SMALL_PRIORITY_QUEUE_CAPACITY;
    }

    AK_FORCE_INLINE void resetTemporaryCaches() {
        mActiveDicNodes->clear();
        mNextActiveDicNodes->clear();
        mTerminalDicNodes->clear();
    }

    static const int LARGE_PRIORITY_QUEUE_CAPACITY;
    static const int SMALL_PRIORITY_QUEUE_CAPACITY;

    const bool mUsesLargeCapacityCache;
    // Instances
    DicNodePriorityQueue mDicNodePriorityQueue0;
    DicNodePriorityQueue mDicNodePriorityQueue1;
    DicNodePriorityQueue mDicNodePriorityQueue2;
    DicNodePriorityQueue mDicNodePriorityQueueForTerminal;

    // Active dicNodes currently being expanded.
    DicNodePriorityQueue *mActiveDicNodes;
    // Next dicNodes to be expanded.
    DicNodePriorityQueue *mNextActiveDicNodes;
    // Cached dicNodes used for continuous suggestion.
    DicNodePriorityQueue *mCachedDicNodesForContinuousSuggestion;
    // Current top terminal dicNodes.
    DicNodePriorityQueue *mTerminalDicNodes;
    int mInputIndex;
    int mLastCachedInputIndex;
};
} // namespace latinime
#endif // LATINIME_DIC_NODES_CACHE_H
