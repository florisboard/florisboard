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

#ifndef LATINIME_DIC_NODE_STATE_SCORING_H
#define LATINIME_DIC_NODE_STATE_SCORING_H

#include <algorithm>
#include <cstdint>

#include "defines.h"
#include "suggest/core/dictionary/digraph_utils.h"
#include "suggest/core/dictionary/error_type_utils.h"

namespace latinime {

class DicNodeStateScoring {
 public:
    AK_FORCE_INLINE DicNodeStateScoring()
            : mDoubleLetterLevel(NOT_A_DOUBLE_LETTER),
              mDigraphIndex(DigraphUtils::NOT_A_DIGRAPH_INDEX),
              mEditCorrectionCount(0), mProximityCorrectionCount(0), mCompletionCount(0),
              mNormalizedCompoundDistance(0.0f), mSpatialDistance(0.0f), mLanguageDistance(0.0f),
              mRawLength(0.0f), mContainedErrorTypes(ErrorTypeUtils::NOT_AN_ERROR),
              mNormalizedCompoundDistanceAfterFirstWord(MAX_VALUE_FOR_WEIGHTING) {
    }

    ~DicNodeStateScoring() {}

    void init() {
        mEditCorrectionCount = 0;
        mProximityCorrectionCount = 0;
        mCompletionCount = 0;
        mNormalizedCompoundDistance = 0.0f;
        mSpatialDistance = 0.0f;
        mLanguageDistance = 0.0f;
        mRawLength = 0.0f;
        mDoubleLetterLevel = NOT_A_DOUBLE_LETTER;
        mDigraphIndex = DigraphUtils::NOT_A_DIGRAPH_INDEX;
        mNormalizedCompoundDistanceAfterFirstWord = MAX_VALUE_FOR_WEIGHTING;
        mContainedErrorTypes = ErrorTypeUtils::NOT_AN_ERROR;
    }

    AK_FORCE_INLINE void initByCopy(const DicNodeStateScoring *const scoring) {
        mEditCorrectionCount = scoring->mEditCorrectionCount;
        mProximityCorrectionCount = scoring->mProximityCorrectionCount;
        mCompletionCount = scoring->mCompletionCount;
        mNormalizedCompoundDistance = scoring->mNormalizedCompoundDistance;
        mSpatialDistance = scoring->mSpatialDistance;
        mLanguageDistance = scoring->mLanguageDistance;
        mRawLength = scoring->mRawLength;
        mDoubleLetterLevel = scoring->mDoubleLetterLevel;
        mDigraphIndex = scoring->mDigraphIndex;
        mContainedErrorTypes = scoring->mContainedErrorTypes;
        mNormalizedCompoundDistanceAfterFirstWord =
                scoring->mNormalizedCompoundDistanceAfterFirstWord;
    }

    void addCost(const float spatialCost, const float languageCost, const bool doNormalization,
            const int inputSize, const int totalInputIndex,
            const ErrorTypeUtils::ErrorType errorType) {
        addDistance(spatialCost, languageCost, doNormalization, inputSize, totalInputIndex);
        mContainedErrorTypes = mContainedErrorTypes | errorType;
        if (ErrorTypeUtils::isEditCorrectionError(errorType)) {
            ++mEditCorrectionCount;
        }
        if (ErrorTypeUtils::isProximityCorrectionError(errorType)) {
            ++mProximityCorrectionCount;
        }
        if (ErrorTypeUtils::isCompletion(errorType)) {
            ++mCompletionCount;
        }
    }

    // Saves the current normalized distance for space-aware gestures.
    // See getNormalizedCompoundDistanceAfterFirstWord for details.
    void saveNormalizedCompoundDistanceAfterFirstWordIfNoneYet() {
        // We get called here after each word. We only want to store the distance after
        // the first word, so if we already have a distance we skip saving -- hence "IfNoneYet"
        // in the method name.
        if (mNormalizedCompoundDistanceAfterFirstWord >= MAX_VALUE_FOR_WEIGHTING) {
            mNormalizedCompoundDistanceAfterFirstWord = getNormalizedCompoundDistance();
        }
    }

    void addRawLength(const float rawLength) {
        mRawLength += rawLength;
    }

    float getCompoundDistance() const {
        return getCompoundDistance(1.0f);
    }

    float getCompoundDistance(
            const float weightOfLangModelVsSpatialModel) const {
        return mSpatialDistance
                + mLanguageDistance * weightOfLangModelVsSpatialModel;
    }

    float getNormalizedCompoundDistance() const {
        return mNormalizedCompoundDistance;
    }

    // For space-aware gestures, we store the normalized distance at the char index
    // that ends the first word of the suggestion. We call this the distance after
    // first word.
    float getNormalizedCompoundDistanceAfterFirstWord() const {
        return mNormalizedCompoundDistanceAfterFirstWord;
    }

    float getSpatialDistance() const {
        return mSpatialDistance;
    }

    float getLanguageDistance() const {
        return mLanguageDistance;
    }

    int16_t getEditCorrectionCount() const {
        return mEditCorrectionCount;
    }

    int16_t getProximityCorrectionCount() const {
        return mProximityCorrectionCount;
    }

    int16_t getCompletionCount() const {
        return mCompletionCount;
    }

    float getRawLength() const {
        return mRawLength;
    }

    DoubleLetterLevel getDoubleLetterLevel() const {
        return mDoubleLetterLevel;
    }

    void setDoubleLetterLevel(DoubleLetterLevel doubleLetterLevel) {
        switch(doubleLetterLevel) {
            case NOT_A_DOUBLE_LETTER:
                break;
            case A_DOUBLE_LETTER:
                if (mDoubleLetterLevel != A_STRONG_DOUBLE_LETTER) {
                    mDoubleLetterLevel = doubleLetterLevel;
                }
                break;
            case A_STRONG_DOUBLE_LETTER:
                mDoubleLetterLevel = doubleLetterLevel;
                break;
        }
    }

    DigraphUtils::DigraphCodePointIndex getDigraphIndex() const {
        return mDigraphIndex;
    }

    void advanceDigraphIndex() {
        switch(mDigraphIndex) {
            case DigraphUtils::NOT_A_DIGRAPH_INDEX:
                mDigraphIndex = DigraphUtils::FIRST_DIGRAPH_CODEPOINT;
                break;
            case DigraphUtils::FIRST_DIGRAPH_CODEPOINT:
                mDigraphIndex = DigraphUtils::SECOND_DIGRAPH_CODEPOINT;
                break;
            case DigraphUtils::SECOND_DIGRAPH_CODEPOINT:
                mDigraphIndex = DigraphUtils::NOT_A_DIGRAPH_INDEX;
                break;
        }
    }

    ErrorTypeUtils::ErrorType getContainedErrorTypes() const {
        return mContainedErrorTypes;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(DicNodeStateScoring);

    DoubleLetterLevel mDoubleLetterLevel;
    DigraphUtils::DigraphCodePointIndex mDigraphIndex;

    int16_t mEditCorrectionCount;
    int16_t mProximityCorrectionCount;
    int16_t mCompletionCount;

    float mNormalizedCompoundDistance;
    float mSpatialDistance;
    float mLanguageDistance;
    float mRawLength;
    // All accumulated error types so far
    ErrorTypeUtils::ErrorType mContainedErrorTypes;
    float mNormalizedCompoundDistanceAfterFirstWord;

    AK_FORCE_INLINE void addDistance(float spatialDistance, float languageDistance,
            bool doNormalization, int inputSize, int totalInputIndex) {
        mSpatialDistance += spatialDistance;
        mLanguageDistance += languageDistance;
        if (!doNormalization) {
            mNormalizedCompoundDistance = mSpatialDistance + mLanguageDistance;
        } else {
            mNormalizedCompoundDistance = (mSpatialDistance + mLanguageDistance)
                    / static_cast<float>(std::max(1, totalInputIndex));
        }
    }
};
} // namespace latinime
#endif // LATINIME_DIC_NODE_STATE_SCORING_H
