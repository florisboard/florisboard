/*
 * Copyright (C) 2013, The Android Open Source Project
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

#include "dictionary/structure/pt_common/dynamic_pt_reading_helper.h"

#include "dictionary/structure/pt_common/pt_node_array_reader.h"
#include "utils/char_utils.h"

namespace latinime {

// To avoid infinite loop caused by invalid or malicious forward links.
const int DynamicPtReadingHelper::MAX_CHILD_COUNT_TO_AVOID_INFINITE_LOOP = 100000;
const int DynamicPtReadingHelper::MAX_PT_NODE_ARRAY_COUNT_TO_AVOID_INFINITE_LOOP = 100000;
const size_t DynamicPtReadingHelper::MAX_READING_STATE_STACK_SIZE = MAX_WORD_LENGTH;

bool DynamicPtReadingHelper::TraversePolicyToGetAllTerminalPtNodePositions::onVisitingPtNode(
        const PtNodeParams *const ptNodeParams) {
    if (ptNodeParams->isTerminal() && !ptNodeParams->isDeleted()) {
        mTerminalPositions->push_back(ptNodeParams->getHeadPos());
    }
    return true;
}

// Visits all PtNodes in post-order depth first manner.
// For example, visits c -> b -> y -> x -> a for the following dictionary:
// a _ b _ c
//   \ x _ y
bool DynamicPtReadingHelper::traverseAllPtNodesInPostorderDepthFirstManner(
        TraversingEventListener *const listener) {
    bool alreadyVisitedChildren = false;
    // Descend from the root to the root PtNode array.
    if (!listener->onDescend(getPosOfLastPtNodeArrayHead())) {
        return false;
    }
    while (!isEnd()) {
        const PtNodeParams ptNodeParams(getPtNodeParams());
        if (!ptNodeParams.isValid()) {
            break;
        }
        if (!alreadyVisitedChildren) {
            if (ptNodeParams.hasChildren()) {
                // Move to the first child.
                if (!listener->onDescend(ptNodeParams.getChildrenPos())) {
                    return false;
                }
                pushReadingStateToStack();
                readChildNode(ptNodeParams);
            } else {
                alreadyVisitedChildren = true;
            }
        } else {
            if (!listener->onVisitingPtNode(&ptNodeParams)) {
                return false;
            }
            readNextSiblingNode(ptNodeParams);
            if (isEnd()) {
                // All PtNodes in current linked PtNode arrays have been visited.
                // Return to the parent.
                if (!listener->onReadingPtNodeArrayTail()) {
                    return false;
                }
                if (mReadingStateStack.size() <= 0) {
                    break;
                }
                if (!listener->onAscend()) {
                    return false;
                }
                popReadingStateFromStack();
                alreadyVisitedChildren = true;
            } else {
                // Process sibling PtNode.
                alreadyVisitedChildren = false;
            }
        }
    }
    // Ascend from the root PtNode array to the root.
    if (!listener->onAscend()) {
        return false;
    }
    return !isError();
}

// Visits all PtNodes in PtNode array level pre-order depth first manner, which is the same order
// that PtNodes are written in the dictionary buffer.
// For example, visits a -> b -> x -> c -> y for the following dictionary:
// a _ b _ c
//   \ x _ y
bool DynamicPtReadingHelper::traverseAllPtNodesInPtNodeArrayLevelPreorderDepthFirstManner(
        TraversingEventListener *const listener) {
    bool alreadyVisitedAllPtNodesInArray = false;
    bool alreadyVisitedChildren = false;
    // Descend from the root to the root PtNode array.
    if (!listener->onDescend(getPosOfLastPtNodeArrayHead())) {
        return false;
    }
    if (isEnd()) {
        // Empty dictionary. Needs to notify the listener of the tail of empty PtNode array.
        if (!listener->onReadingPtNodeArrayTail()) {
            return false;
        }
    }
    pushReadingStateToStack();
    while (!isEnd()) {
        const PtNodeParams ptNodeParams(getPtNodeParams());
        if (!ptNodeParams.isValid()) {
            break;
        }
        if (alreadyVisitedAllPtNodesInArray) {
            if (alreadyVisitedChildren) {
                // Move to next sibling PtNode's children.
                readNextSiblingNode(ptNodeParams);
                if (isEnd()) {
                    // Return to the parent PTNode.
                    if (!listener->onAscend()) {
                        return false;
                    }
                    if (mReadingStateStack.size() <= 0) {
                        break;
                    }
                    popReadingStateFromStack();
                    alreadyVisitedChildren = true;
                    alreadyVisitedAllPtNodesInArray = true;
                } else {
                    alreadyVisitedChildren = false;
                }
            } else {
                if (ptNodeParams.hasChildren()) {
                    // Move to the first child.
                    if (!listener->onDescend(ptNodeParams.getChildrenPos())) {
                        return false;
                    }
                    pushReadingStateToStack();
                    readChildNode(ptNodeParams);
                    // Push state to return the head of PtNode array.
                    pushReadingStateToStack();
                    alreadyVisitedAllPtNodesInArray = false;
                    alreadyVisitedChildren = false;
                } else {
                    alreadyVisitedChildren = true;
                }
            }
        } else {
            if (!listener->onVisitingPtNode(&ptNodeParams)) {
                return false;
            }
            readNextSiblingNode(ptNodeParams);
            if (isEnd()) {
                if (!listener->onReadingPtNodeArrayTail()) {
                    return false;
                }
                // Return to the head of current PtNode array.
                popReadingStateFromStack();
                alreadyVisitedAllPtNodesInArray = true;
            }
        }
    }
    popReadingStateFromStack();
    // Ascend from the root PtNode array to the root.
    if (!listener->onAscend()) {
        return false;
    }
    return !isError();
}

int DynamicPtReadingHelper::getCodePointsAndReturnCodePointCount(const int maxCodePointCount,
        int *const outCodePoints) {
    // This method traverses parent nodes from the terminal by following parent pointers; thus,
    // node code points are stored in the buffer in the reverse order.
    int reverseCodePoints[maxCodePointCount];
    const PtNodeParams terminalPtNodeParams(getPtNodeParams());
    // First, read the terminal node and get its probability.
    if (!isValidTerminalNode(terminalPtNodeParams)) {
        // Node at the ptNodePos is not a valid terminal node.
        return 0;
    }
    // Then, following parent node link to the dictionary root and fetch node code points.
    int totalCodePointCount = 0;
    while (!isEnd()) {
        const PtNodeParams ptNodeParams(getPtNodeParams());
        totalCodePointCount = getTotalCodePointCount(ptNodeParams);
        if (!ptNodeParams.isValid() || totalCodePointCount > maxCodePointCount) {
            // The ptNodePos is not a valid terminal node position in the dictionary.
            return 0;
        }
        // Store node code points to buffer in the reverse order.
        fetchMergedNodeCodePointsInReverseOrder(ptNodeParams, getPrevTotalCodePointCount(),
                reverseCodePoints);
        // Follow parent node toward the root node.
        readParentNode(ptNodeParams);
    }
    if (isError()) {
        // The node position or the dictionary is invalid.
        return 0;
    }
    // Reverse the stored code points to output them.
    for (int i = 0; i < totalCodePointCount; ++i) {
        outCodePoints[i] = reverseCodePoints[totalCodePointCount - i - 1];
    }
    return totalCodePointCount;
}

int DynamicPtReadingHelper::getTerminalPtNodePositionOfWord(const int *const inWord,
        const size_t length, const bool forceLowerCaseSearch) {
    int searchCodePoints[length];
    for (size_t i = 0; i < length; ++i) {
        searchCodePoints[i] = forceLowerCaseSearch ? CharUtils::toLowerCase(inWord[i]) : inWord[i];
    }
    while (!isEnd()) {
        const PtNodeParams ptNodeParams(getPtNodeParams());
        const int matchedCodePointCount = getPrevTotalCodePointCount();
        if (getTotalCodePointCount(ptNodeParams) > length
                || !isMatchedCodePoint(ptNodeParams, 0 /* index */,
                        searchCodePoints[matchedCodePointCount])) {
            // Current node has too many code points or its first code point is different from
            // target code point. Skip this node and read the next sibling node.
            readNextSiblingNode(ptNodeParams);
            continue;
        }
        // Check following merged node code points.
        const int nodeCodePointCount = ptNodeParams.getCodePointCount();
        for (int j = 1; j < nodeCodePointCount; ++j) {
            if (!isMatchedCodePoint(ptNodeParams, j, searchCodePoints[matchedCodePointCount + j])) {
                // Different code point is found. The given word is not included in the dictionary.
                return NOT_A_DICT_POS;
            }
        }
        // All characters are matched.
        if (length == getTotalCodePointCount(ptNodeParams)) {
            if (!ptNodeParams.isTerminal()) {
                return NOT_A_DICT_POS;
            }
            // Terminal position is found.
            return ptNodeParams.getHeadPos();
        }
        if (!ptNodeParams.hasChildren()) {
            return NOT_A_DICT_POS;
        }
        // Advance to the children nodes.
        readChildNode(ptNodeParams);
    }
    // If we already traversed the tree further than the word is long, there means
    // there was no match (or we would have found it).
    return NOT_A_DICT_POS;
}

// Read node array size and process empty node arrays. Nodes and arrays are counted up in this
// method to avoid an infinite loop.
void DynamicPtReadingHelper::nextPtNodeArray() {
    int ptNodeCountInArray = 0;
    int firstPtNodePos = NOT_A_DICT_POS;
    if (!mPtNodeArrayReader->readPtNodeArrayInfoAndReturnIfValid(
            mReadingState.mPos, &ptNodeCountInArray, &firstPtNodePos)) {
        mIsError = true;
        mReadingState.mPos = NOT_A_DICT_POS;
        return;
    }
    mReadingState.mPosOfThisPtNodeArrayHead = mReadingState.mPos;
    mReadingState.mRemainingPtNodeCountInThisArray = ptNodeCountInArray;
    mReadingState.mPos = firstPtNodePos;
    // Count up nodes and node arrays to avoid infinite loop.
    mReadingState.mTotalPtNodeIndexInThisArrayChain +=
            mReadingState.mRemainingPtNodeCountInThisArray;
    mReadingState.mPtNodeArrayIndexInThisArrayChain++;
    if (mReadingState.mRemainingPtNodeCountInThisArray < 0
            || mReadingState.mTotalPtNodeIndexInThisArrayChain
                    > MAX_CHILD_COUNT_TO_AVOID_INFINITE_LOOP
            || mReadingState.mPtNodeArrayIndexInThisArrayChain
                    > MAX_PT_NODE_ARRAY_COUNT_TO_AVOID_INFINITE_LOOP) {
        // Invalid dictionary.
        AKLOGI("Invalid dictionary. nodeCount: %d, totalNodeCount: %d, MAX_CHILD_COUNT: %d"
                "nodeArrayCount: %d, MAX_NODE_ARRAY_COUNT: %d",
                mReadingState.mRemainingPtNodeCountInThisArray,
                mReadingState.mTotalPtNodeIndexInThisArrayChain,
                MAX_CHILD_COUNT_TO_AVOID_INFINITE_LOOP,
                mReadingState.mPtNodeArrayIndexInThisArrayChain,
                MAX_PT_NODE_ARRAY_COUNT_TO_AVOID_INFINITE_LOOP);
        ASSERT(false);
        mIsError = true;
        mReadingState.mPos = NOT_A_DICT_POS;
        return;
    }
    if (mReadingState.mRemainingPtNodeCountInThisArray == 0) {
        // Empty node array. Try following forward link.
        followForwardLink();
    }
}

// Follow the forward link and read the next node array if exists.
void DynamicPtReadingHelper::followForwardLink() {
    int nextPtNodeArrayPos = NOT_A_DICT_POS;
    if (!mPtNodeArrayReader->readForwardLinkAndReturnIfValid(
            mReadingState.mPos, &nextPtNodeArrayPos)) {
        mIsError = true;
        mReadingState.mPos = NOT_A_DICT_POS;
        return;
    }
    mReadingState.mPosOfLastForwardLinkField = mReadingState.mPos;
    if (nextPtNodeArrayPos != NOT_A_DICT_POS) {
        // Follow the forward link.
        mReadingState.mPos = nextPtNodeArrayPos;
        nextPtNodeArray();
    } else {
        // All node arrays have been read.
        mReadingState.mPos = NOT_A_DICT_POS;
    }
}

} // namespace latinime
