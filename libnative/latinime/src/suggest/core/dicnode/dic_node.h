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

#ifndef LATINIME_DIC_NODE_H
#define LATINIME_DIC_NODE_H

#include "defines.h"
#include "suggest/core/dicnode/dic_node_profiler.h"
#include "suggest/core/dicnode/dic_node_utils.h"
#include "suggest/core/dicnode/internal/dic_node_state.h"
#include "suggest/core/dicnode/internal/dic_node_properties.h"
#include "suggest/core/dictionary/digraph_utils.h"
#include "suggest/core/dictionary/error_type_utils.h"
#include "suggest/core/layout/proximity_info_state.h"
#include "utils/char_utils.h"
#include "utils/int_array_view.h"

#if DEBUG_DICT
#define LOGI_SHOW_ADD_COST_PROP \
        do { \
            char charBuf[50]; \
            INTS_TO_CHARS(getOutputWordBuf(), getNodeCodePointCount(), charBuf, NELEMS(charBuf)); \
            AKLOGI("%20s, \"%c\", size = %03d, total = %03d, index(0) = %02d, dist = %.4f, %s,,", \
                    __FUNCTION__, getNodeCodePoint(), inputSize, getTotalInputIndex(), \
                    getInputIndex(0), getNormalizedCompoundDistance(), charBuf); \
        } while (0)
#define DUMP_WORD_AND_SCORE(header) \
        do { \
            char charBuf[50]; \
            INTS_TO_CHARS(getOutputWordBuf(), \
                    getNodeCodePointCount() \
                            + mDicNodeState.mDicNodeStateOutput.getPrevWordsLength(), \
                    charBuf, NELEMS(charBuf)); \
            AKLOGI("#%8s, %5f, %5f, %5f, %5f, %s, %d, %5f,", header, \
                    getSpatialDistanceForScoring(), \
                    mDicNodeState.mDicNodeStateScoring.getLanguageDistance(), \
                    getNormalizedCompoundDistance(), getRawLength(), charBuf, \
                    getInputIndex(0), getNormalizedCompoundDistanceAfterFirstWord()); \
        } while (0)
#else
#define LOGI_SHOW_ADD_COST_PROP
#define DUMP_WORD_AND_SCORE(header)
#endif

namespace latinime {

// This struct is purely a bucket to return values. No instances of this struct should be kept.
struct DicNode_InputStateG {
    DicNode_InputStateG()
            : mNeedsToUpdateInputStateG(false), mPointerId(0), mInputIndex(0),
              mPrevCodePoint(0), mTerminalDiffCost(0.0f), mRawLength(0.0f),
              mDoubleLetterLevel(NOT_A_DOUBLE_LETTER) {}

    bool mNeedsToUpdateInputStateG;
    int mPointerId;
    int16_t mInputIndex;
    int mPrevCodePoint;
    float mTerminalDiffCost;
    float mRawLength;
    DoubleLetterLevel mDoubleLetterLevel;
};

class DicNode {
    // Caveat: We define Weighting as a friend class of DicNode to let Weighting change
    // the distance of DicNode.
    // Caution!!! In general, we avoid using the "friend" access modifier.
    // This is an exception to explicitly hide DicNode::addCost() from all classes but Weighting.
    friend class Weighting;

 public:
#if DEBUG_DICT
    DicNodeProfiler mProfiler;
#endif

    AK_FORCE_INLINE DicNode()
            :
#if DEBUG_DICT
              mProfiler(),
#endif
              mDicNodeProperties(), mDicNodeState(), mIsCachedForNextSuggestion(false) {}

    DicNode(const DicNode &dicNode);
    DicNode &operator=(const DicNode &dicNode);
    ~DicNode() {}

    // Init for copy
    void initByCopy(const DicNode *const dicNode) {
        mIsCachedForNextSuggestion = dicNode->mIsCachedForNextSuggestion;
        mDicNodeProperties.initByCopy(&dicNode->mDicNodeProperties);
        mDicNodeState.initByCopy(&dicNode->mDicNodeState);
        PROF_NODE_COPY(&dicNode->mProfiler, mProfiler);
    }

    // Init for root with prevWordIds which is used for n-gram
    void initAsRoot(const int rootPtNodeArrayPos, const WordIdArrayView prevWordIds) {
        mIsCachedForNextSuggestion = false;
        mDicNodeProperties.init(rootPtNodeArrayPos, prevWordIds);
        mDicNodeState.init();
        PROF_NODE_RESET(mProfiler);
    }

    // Init for root with previous word
    void initAsRootWithPreviousWord(const DicNode *const dicNode, const int rootPtNodeArrayPos) {
        mIsCachedForNextSuggestion = dicNode->mIsCachedForNextSuggestion;
        WordIdArray<MAX_PREV_WORD_COUNT_FOR_N_GRAM> newPrevWordIds;
        newPrevWordIds[0] = dicNode->mDicNodeProperties.getWordId();
        dicNode->getPrevWordIds().limit(newPrevWordIds.size() - 1)
                .copyToArray(&newPrevWordIds, 1 /* offset */);
        mDicNodeProperties.init(rootPtNodeArrayPos, WordIdArrayView::fromArray(newPrevWordIds));
        mDicNodeState.initAsRootWithPreviousWord(&dicNode->mDicNodeState,
                dicNode->mDicNodeProperties.getDepth());
        PROF_NODE_COPY(&dicNode->mProfiler, mProfiler);
    }

    void initAsPassingChild(const DicNode *parentDicNode) {
        mIsCachedForNextSuggestion = parentDicNode->mIsCachedForNextSuggestion;
        const int codePoint =
                parentDicNode->mDicNodeState.mDicNodeStateOutput.getCurrentWordCodePointAt(
                            parentDicNode->getNodeCodePointCount());
        mDicNodeProperties.init(&parentDicNode->mDicNodeProperties, codePoint);
        mDicNodeState.initByCopy(&parentDicNode->mDicNodeState);
        PROF_NODE_COPY(&parentDicNode->mProfiler, mProfiler);
    }

    void initAsChild(const DicNode *const dicNode, const int childrenPtNodeArrayPos,
            const int wordId, const CodePointArrayView mergedCodePoints) {
        uint16_t newDepth = static_cast<uint16_t>(dicNode->getNodeCodePointCount() + 1);
        mIsCachedForNextSuggestion = dicNode->mIsCachedForNextSuggestion;
        const uint16_t newLeavingDepth = static_cast<uint16_t>(
                dicNode->mDicNodeProperties.getLeavingDepth() + mergedCodePoints.size());
        mDicNodeProperties.init(childrenPtNodeArrayPos, mergedCodePoints[0],
                wordId, newDepth, newLeavingDepth, dicNode->mDicNodeProperties.getPrevWordIds());
        mDicNodeState.init(&dicNode->mDicNodeState, mergedCodePoints.size(),
                mergedCodePoints.data());
        PROF_NODE_COPY(&dicNode->mProfiler, mProfiler);
    }

    bool isRoot() const {
        return getNodeCodePointCount() == 0;
    }

    bool hasChildren() const {
        return mDicNodeProperties.hasChildren();
    }

    bool isLeavingNode() const {
        ASSERT(getNodeCodePointCount() <= mDicNodeProperties.getLeavingDepth());
        return getNodeCodePointCount() == mDicNodeProperties.getLeavingDepth();
    }

    AK_FORCE_INLINE bool isFirstLetter() const {
        return getNodeCodePointCount() == 1;
    }

    bool isCached() const {
        return mIsCachedForNextSuggestion;
    }

    void setCached() {
        mIsCachedForNextSuggestion = true;
    }

    // Check if the current word and the previous word can be considered as a valid multiple word
    // suggestion.
    bool isValidMultipleWordSuggestion() const {
        // Treat suggestion as invalid if the current and the previous word are single character
        // words.
        const int prevWordLen = mDicNodeState.mDicNodeStateOutput.getPrevWordsLength()
                - mDicNodeState.mDicNodeStateOutput.getPrevWordStart() - 1;
        const int currentWordLen = getNodeCodePointCount();
        return (prevWordLen != 1 || currentWordLen != 1);
    }

    bool isFirstCharUppercase() const {
        const int c = mDicNodeState.mDicNodeStateOutput.getCurrentWordCodePointAt(0);
        return CharUtils::isAsciiUpper(c);
    }

    bool isCompletion(const int inputSize) const {
        return mDicNodeState.mDicNodeStateInput.getInputIndex(0) >= inputSize;
    }

    bool canDoLookAheadCorrection(const int inputSize) const {
        return mDicNodeState.mDicNodeStateInput.getInputIndex(0) < inputSize - 1;
    }

    // Used to get n-gram probability in DicNodeUtils.
    int getWordId() const {
        return mDicNodeProperties.getWordId();
    }

    const WordIdArrayView getPrevWordIds() const {
        return mDicNodeProperties.getPrevWordIds();
    }

    // Used in DicNodeUtils
    int getChildrenPtNodeArrayPos() const {
        return mDicNodeProperties.getChildrenPtNodeArrayPos();
    }

    AK_FORCE_INLINE bool isTerminalDicNode() const {
        const bool isTerminalPtNode = mDicNodeProperties.isTerminal();
        const int currentDicNodeDepth = getNodeCodePointCount();
        const int terminalDicNodeDepth = mDicNodeProperties.getLeavingDepth();
        return isTerminalPtNode && currentDicNodeDepth > 0
                && currentDicNodeDepth == terminalDicNodeDepth;
    }

    bool shouldBeFilteredBySafetyNetForBigram() const {
        const uint16_t currentDepth = getNodeCodePointCount();
        const int prevWordLen = mDicNodeState.mDicNodeStateOutput.getPrevWordsLength()
                - mDicNodeState.mDicNodeStateOutput.getPrevWordStart() - 1;
        return !(currentDepth > 0 && (currentDepth != 1 || prevWordLen != 1));
    }

    bool hasMatchedOrProximityCodePoints() const {
        // This DicNode does not have matched or proximity code points when all code points have
        // been handled as edit corrections or completion so far.
        const int editCorrectionCount = mDicNodeState.mDicNodeStateScoring.getEditCorrectionCount();
        const int completionCount = mDicNodeState.mDicNodeStateScoring.getCompletionCount();
        return (editCorrectionCount + completionCount) < getNodeCodePointCount();
    }

    bool isTotalInputSizeExceedingLimit() const {
        // TODO: 3 can be 2? Needs to be investigated.
        // TODO: Have a const variable for 3 (or 2)
        return getTotalNodeCodePointCount() > MAX_WORD_LENGTH - 3;
    }

    void outputResult(int *dest) const {
        memmove(dest, getOutputWordBuf(), getTotalNodeCodePointCount() * sizeof(dest[0]));
        DUMP_WORD_AND_SCORE("OUTPUT");
    }

    // "Total" in this context (and other methods in this class) means the whole suggestion. When
    // this represents a multi-word suggestion, the referenced PtNode (in mDicNodeState) is only
    // the one that corresponds to the last word of the suggestion, and all the previous words
    // are concatenated together in mDicNodeStateOutput.
    int getTotalNodeSpaceCount() const {
        if (!hasMultipleWords()) {
            return 0;
        }
        return CharUtils::getSpaceCount(mDicNodeState.mDicNodeStateOutput.getCodePointBuf(),
                mDicNodeState.mDicNodeStateOutput.getPrevWordsLength());
    }

    int getSecondWordFirstInputIndex(const ProximityInfoState *const pInfoState) const {
        const int inputIndex = mDicNodeState.mDicNodeStateOutput.getSecondWordFirstInputIndex();
        if (inputIndex == NOT_AN_INDEX) {
            return NOT_AN_INDEX;
        } else {
            return pInfoState->getInputIndexOfSampledPoint(inputIndex);
        }
    }

    bool hasMultipleWords() const {
        return mDicNodeState.mDicNodeStateOutput.getPrevWordCount() > 0;
    }

    int getProximityCorrectionCount() const {
        return mDicNodeState.mDicNodeStateScoring.getProximityCorrectionCount();
    }

    int getEditCorrectionCount() const {
        return mDicNodeState.mDicNodeStateScoring.getEditCorrectionCount();
    }

    // Used to prune nodes
    float getNormalizedCompoundDistance() const {
        return mDicNodeState.mDicNodeStateScoring.getNormalizedCompoundDistance();
    }

    // Used to prune nodes
    float getNormalizedSpatialDistance() const {
        return mDicNodeState.mDicNodeStateScoring.getSpatialDistance()
                / static_cast<float>(getInputIndex(0) + 1);
    }

    // Used to prune nodes
    float getCompoundDistance() const {
        return mDicNodeState.mDicNodeStateScoring.getCompoundDistance();
    }

    // Used to prune nodes
    float getCompoundDistance(const float weightOfLangModelVsSpatialModel) const {
        return mDicNodeState.mDicNodeStateScoring.getCompoundDistance(
                weightOfLangModelVsSpatialModel);
    }

    AK_FORCE_INLINE const int *getOutputWordBuf() const {
        return mDicNodeState.mDicNodeStateOutput.getCodePointBuf();
    }

    int getPrevCodePointG(int pointerId) const {
        return mDicNodeState.mDicNodeStateInput.getPrevCodePoint(pointerId);
    }

    // Whether the current codepoint can be an intentional omission, in which case the traversal
    // algorithm will always check for a possible omission here.
    bool canBeIntentionalOmission() const {
        return CharUtils::isIntentionalOmissionCodePoint(getNodeCodePoint());
    }

    // Whether the omission is so frequent that it should incur zero cost.
    bool isZeroCostOmission() const {
        // TODO: do not hardcode and read from header
        return (getNodeCodePoint() == KEYCODE_SINGLE_QUOTE);
    }

    // TODO: remove
    float getTerminalDiffCostG(int path) const {
        return mDicNodeState.mDicNodeStateInput.getTerminalDiffCost(path);
    }

    //////////////////////
    // Temporary getter //
    // TODO: Remove     //
    //////////////////////
    // TODO: Remove once touch path is merged into ProximityInfoState
    // Note: Returned codepoint may be a digraph codepoint if the node is in a composite glyph.
    int getNodeCodePoint() const {
        const int codePoint = mDicNodeProperties.getDicNodeCodePoint();
        const DigraphUtils::DigraphCodePointIndex digraphIndex =
                mDicNodeState.mDicNodeStateScoring.getDigraphIndex();
        if (digraphIndex == DigraphUtils::NOT_A_DIGRAPH_INDEX) {
            return codePoint;
        }
        return DigraphUtils::getDigraphCodePointForIndex(codePoint, digraphIndex);
    }

    ////////////////////////////////
    // Utils for cost calculation //
    ////////////////////////////////
    AK_FORCE_INLINE bool isSameNodeCodePoint(const DicNode *const dicNode) const {
        return mDicNodeProperties.getDicNodeCodePoint()
                == dicNode->mDicNodeProperties.getDicNodeCodePoint();
    }

    // TODO: remove
    // TODO: rename getNextInputIndex
    int16_t getInputIndex(int pointerId) const {
        return mDicNodeState.mDicNodeStateInput.getInputIndex(pointerId);
    }

    ////////////////////////////////////
    // Getter of features for scoring //
    ////////////////////////////////////
    float getSpatialDistanceForScoring() const {
        return mDicNodeState.mDicNodeStateScoring.getSpatialDistance();
    }

    // For space-aware gestures, we store the normalized distance at the char index
    // that ends the first word of the suggestion. We call this the distance after
    // first word.
    float getNormalizedCompoundDistanceAfterFirstWord() const {
        return mDicNodeState.mDicNodeStateScoring.getNormalizedCompoundDistanceAfterFirstWord();
    }

    float getRawLength() const {
        return mDicNodeState.mDicNodeStateScoring.getRawLength();
    }

    DoubleLetterLevel getDoubleLetterLevel() const {
        return mDicNodeState.mDicNodeStateScoring.getDoubleLetterLevel();
    }

    void setDoubleLetterLevel(DoubleLetterLevel doubleLetterLevel) {
        mDicNodeState.mDicNodeStateScoring.setDoubleLetterLevel(doubleLetterLevel);
    }

    bool isInDigraph() const {
        return mDicNodeState.mDicNodeStateScoring.getDigraphIndex()
                != DigraphUtils::NOT_A_DIGRAPH_INDEX;
    }

    void advanceDigraphIndex() {
        mDicNodeState.mDicNodeStateScoring.advanceDigraphIndex();
    }

    ErrorTypeUtils::ErrorType getContainedErrorTypes() const {
        return mDicNodeState.mDicNodeStateScoring.getContainedErrorTypes();
    }

    inline uint16_t getNodeCodePointCount() const {
        return mDicNodeProperties.getDepth();
    }

    // Returns code point count including spaces
    inline uint16_t getTotalNodeCodePointCount() const {
        return getNodeCodePointCount() + mDicNodeState.mDicNodeStateOutput.getPrevWordsLength();
    }

    AK_FORCE_INLINE void dump(const char *tag) const {
#if DEBUG_DICT
        DUMP_WORD_AND_SCORE(tag);
#if DEBUG_DUMP_ERROR
        mProfiler.dump();
#endif
#endif
    }

    AK_FORCE_INLINE bool compare(const DicNode *right) const {
        // Promote exact matches to prevent them from being pruned.
        const bool leftExactMatch = ErrorTypeUtils::isExactMatch(getContainedErrorTypes());
        const bool rightExactMatch = ErrorTypeUtils::isExactMatch(right->getContainedErrorTypes());
        if (leftExactMatch != rightExactMatch) {
            return leftExactMatch;
        }
        const float diff =
                right->getNormalizedCompoundDistance() - getNormalizedCompoundDistance();
        static const float MIN_DIFF = 0.000001f;
        if (diff > MIN_DIFF) {
            return true;
        } else if (diff < -MIN_DIFF) {
            return false;
        }
        const int depth = getNodeCodePointCount();
        const int depthDiff = right->getNodeCodePointCount() - depth;
        if (depthDiff != 0) {
            return depthDiff > 0;
        }
        for (int i = 0; i < depth; ++i) {
            const int codePoint = mDicNodeState.mDicNodeStateOutput.getCurrentWordCodePointAt(i);
            const int rightCodePoint =
                    right->mDicNodeState.mDicNodeStateOutput.getCurrentWordCodePointAt(i);
            if (codePoint != rightCodePoint) {
                return rightCodePoint > codePoint;
            }
        }
        // Compare pointer values here for stable comparison
        return this > right;
    }

 private:
    DicNodeProperties mDicNodeProperties;
    DicNodeState mDicNodeState;
    // TODO: Remove
    bool mIsCachedForNextSuggestion;

    AK_FORCE_INLINE int getTotalInputIndex() const {
        int index = 0;
        for (int i = 0; i < MAX_POINTER_COUNT_G; i++) {
            index += mDicNodeState.mDicNodeStateInput.getInputIndex(i);
        }
        return index;
    }

    // Caveat: Must not be called outside Weighting
    // This restriction is guaranteed by "friend"
    AK_FORCE_INLINE void addCost(const float spatialCost, const float languageCost,
            const bool doNormalization, const int inputSize,
            const ErrorTypeUtils::ErrorType errorType) {
        if (DEBUG_GEO_FULL) {
            LOGI_SHOW_ADD_COST_PROP;
        }
        mDicNodeState.mDicNodeStateScoring.addCost(spatialCost, languageCost, doNormalization,
                inputSize, getTotalInputIndex(), errorType);
    }

    // Saves the current normalized compound distance for space-aware gestures.
    // See getNormalizedCompoundDistanceAfterFirstWord for details.
    AK_FORCE_INLINE void saveNormalizedCompoundDistanceAfterFirstWordIfNoneYet() {
        mDicNodeState.mDicNodeStateScoring.saveNormalizedCompoundDistanceAfterFirstWordIfNoneYet();
    }

    // Caveat: Must not be called outside Weighting
    // This restriction is guaranteed by "friend"
    AK_FORCE_INLINE void forwardInputIndex(const int pointerId, const int count,
            const bool overwritesPrevCodePointByNodeCodePoint) {
        if (count == 0) {
            return;
        }
        mDicNodeState.mDicNodeStateInput.forwardInputIndex(pointerId, count);
        if (overwritesPrevCodePointByNodeCodePoint) {
            mDicNodeState.mDicNodeStateInput.setPrevCodePoint(0, getNodeCodePoint());
        }
    }

    AK_FORCE_INLINE void updateInputIndexG(const DicNode_InputStateG *const inputStateG) {
        if (mDicNodeState.mDicNodeStateOutput.getPrevWordCount() == 1 && isFirstLetter()) {
            mDicNodeState.mDicNodeStateOutput.setSecondWordFirstInputIndex(
                    inputStateG->mInputIndex);
        }
        mDicNodeState.mDicNodeStateInput.updateInputIndexG(inputStateG->mPointerId,
                inputStateG->mInputIndex, inputStateG->mPrevCodePoint,
                inputStateG->mTerminalDiffCost, inputStateG->mRawLength);
        mDicNodeState.mDicNodeStateScoring.addRawLength(inputStateG->mRawLength);
        mDicNodeState.mDicNodeStateScoring.setDoubleLetterLevel(inputStateG->mDoubleLetterLevel);
    }
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_H
