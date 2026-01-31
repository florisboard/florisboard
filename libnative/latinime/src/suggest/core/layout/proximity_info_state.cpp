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

#define LOG_TAG "LatinIME: proximity_info_state.cpp"

#include "suggest/core/layout/proximity_info_state.h"

#include <algorithm>
#include <cstring> // for memset() and memmove()
#include <sstream> // for debug prints
#include <unordered_map>
#include <vector>

#include "defines.h"
#include "suggest/core/layout/geometry_utils.h"
#include "suggest/core/layout/proximity_info.h"
#include "suggest/core/layout/proximity_info_state_utils.h"
#include "utils/char_utils.h"

namespace latinime {

int ProximityInfoState::getPrimaryOriginalCodePointAt(const int index) const {
    const int primaryCodePoint = getPrimaryCodePointAt(index);
    const int keyIndex = mProximityInfo->getKeyIndexOf(primaryCodePoint);
    return mProximityInfo->getOriginalCodePointOf(keyIndex);
}

// TODO: Remove the dependency of "isGeometric"
void ProximityInfoState::initInputParams(const int pointerId, const float maxPointToKeyLength,
        const ProximityInfo *proximityInfo, const int *const inputCodes, const int inputSize,
        const int *const xCoordinates, const int *const yCoordinates, const int *const times,
        const int *const pointerIds, const bool isGeometric, const std::vector<int> *locale) {
    ASSERT(isGeometric || (inputSize < MAX_WORD_LENGTH));
    mIsContinuousSuggestionPossible = (mHasBeenUpdatedByGeometricInput != isGeometric) ?
            false : ProximityInfoStateUtils::checkAndReturnIsContinuousSuggestionPossible(
                    inputSize, xCoordinates, yCoordinates, times, mSampledInputSize,
                    &mSampledInputXs, &mSampledInputYs, &mSampledTimes, &mSampledInputIndice);
    if (DEBUG_DICT) {
        AKLOGI("isContinuousSuggestionPossible = %s",
                (mIsContinuousSuggestionPossible ? "true" : "false"));
    }

    mProximityInfo = proximityInfo;
    mHasTouchPositionCorrectionData = proximityInfo->hasTouchPositionCorrectionData();
    mMostCommonKeyWidthSquare = proximityInfo->getMostCommonKeyWidthSquare();
    mKeyCount = proximityInfo->getKeyCount();
    mCellHeight = proximityInfo->getCellHeight();
    mCellWidth = proximityInfo->getCellWidth();
    mGridHeight = proximityInfo->getGridWidth();
    mGridWidth = proximityInfo->getGridHeight();

    memset(mInputProximities, 0, sizeof(mInputProximities));

    if (!isGeometric && pointerId == 0) {
        mProximityInfo->initializeProximities(inputCodes, xCoordinates, yCoordinates,
                inputSize, mInputProximities, locale);
    }

    ///////////////////////
    // Setup touch points
    int pushTouchPointStartIndex = 0;
    int lastSavedInputSize = 0;
    mMaxPointToKeyLength = maxPointToKeyLength;
    mSampledInputSize = 0;
    mMostProbableStringProbability = 0.0f;

    if (mIsContinuousSuggestionPossible && mSampledInputIndice.size() > 1) {
        // Just update difference.
        // Previous two points are never skipped. Thus, we pop 2 input point data here.
        pushTouchPointStartIndex = ProximityInfoStateUtils::trimLastTwoTouchPoints(
                &mSampledInputXs, &mSampledInputYs, &mSampledTimes, &mSampledLengthCache,
                &mSampledInputIndice);
        lastSavedInputSize = mSampledInputXs.size();
    } else {
        // Clear all data.
        mSampledInputXs.clear();
        mSampledInputYs.clear();
        mSampledTimes.clear();
        mSampledInputIndice.clear();
        mSampledLengthCache.clear();
        mSampledNormalizedSquaredLengthCache.clear();
        mSampledSearchKeySets.clear();
        mSpeedRates.clear();
        mBeelineSpeedPercentiles.clear();
        mCharProbabilities.clear();
        mDirections.clear();
    }

    if (DEBUG_GEO_FULL) {
        AKLOGI("Init ProximityInfoState: reused points =  %d, last input size = %d",
                pushTouchPointStartIndex, lastSavedInputSize);
    }

    if (xCoordinates && yCoordinates) {
        mSampledInputSize = ProximityInfoStateUtils::updateTouchPoints(mProximityInfo,
                mMaxPointToKeyLength, mInputProximities, xCoordinates, yCoordinates, times,
                pointerIds, inputSize, isGeometric, pointerId,
                pushTouchPointStartIndex, &mSampledInputXs, &mSampledInputYs, &mSampledTimes,
                &mSampledLengthCache, &mSampledInputIndice);
    }

    if (mSampledInputSize > 0 && isGeometric) {
        mAverageSpeed = ProximityInfoStateUtils::refreshSpeedRates(inputSize, xCoordinates,
                yCoordinates, times, lastSavedInputSize, mSampledInputSize, &mSampledInputXs,
                &mSampledInputYs, &mSampledTimes, &mSampledLengthCache, &mSampledInputIndice,
                &mSpeedRates, &mDirections);
        ProximityInfoStateUtils::refreshBeelineSpeedRates(mProximityInfo->getMostCommonKeyWidth(),
                mAverageSpeed, inputSize, xCoordinates, yCoordinates, times, mSampledInputSize,
                &mSampledInputXs, &mSampledInputYs, &mSampledInputIndice,
                &mBeelineSpeedPercentiles);
    }

    if (mSampledInputSize > 0) {
        ProximityInfoStateUtils::initGeometricDistanceInfos(mProximityInfo, mSampledInputSize,
                lastSavedInputSize, isGeometric, &mSampledInputXs, &mSampledInputYs,
                &mSampledNormalizedSquaredLengthCache);
        if (isGeometric) {
            // updates probabilities of skipping or mapping each key for all points.
            ProximityInfoStateUtils::updateAlignPointProbabilities(
                    mMaxPointToKeyLength, mProximityInfo->getMostCommonKeyWidth(),
                    mProximityInfo->getKeyCount(), lastSavedInputSize, mSampledInputSize,
                    &mSampledInputXs, &mSampledInputYs, &mSpeedRates, &mSampledLengthCache,
                    &mSampledNormalizedSquaredLengthCache, mProximityInfo, &mCharProbabilities);
            ProximityInfoStateUtils::updateSampledSearchKeySets(mProximityInfo,
                    mSampledInputSize, lastSavedInputSize, &mSampledLengthCache,
                    &mCharProbabilities, &mSampledSearchKeySets,
                    &mSampledSearchKeyVectors);
            mMostProbableStringProbability = ProximityInfoStateUtils::getMostProbableString(
                    mProximityInfo, mSampledInputSize, &mCharProbabilities, mMostProbableString);

        }
    }

    if (DEBUG_SAMPLING_POINTS) {
        ProximityInfoStateUtils::dump(isGeometric, inputSize, xCoordinates, yCoordinates,
                mSampledInputSize, &mSampledInputXs, &mSampledInputYs, &mSampledTimes, &mSpeedRates,
                &mBeelineSpeedPercentiles);
    }
    // end
    ///////////////////////

    mTouchPositionCorrectionEnabled = mSampledInputSize > 0 && mHasTouchPositionCorrectionData
            && xCoordinates && yCoordinates;
    if (!isGeometric && pointerId == 0) {
        ProximityInfoStateUtils::initPrimaryInputWord(
                inputSize, mInputProximities, mPrimaryInputWord);
    }
    if (DEBUG_GEO_FULL) {
        AKLOGI("ProximityState init finished: %d points out of %d", mSampledInputSize, inputSize);
    }
    mHasBeenUpdatedByGeometricInput = isGeometric;
}

// This function basically converts from a length to an edit distance. Accordingly, it's obviously
// wrong to compare with mMaxPointToKeyLength.
float ProximityInfoState::getPointToKeyLength(
        const int inputIndex, const int codePoint) const {
    const int keyId = mProximityInfo->getKeyIndexOf(codePoint);
    if (keyId != NOT_AN_INDEX) {
        const int index = inputIndex * mProximityInfo->getKeyCount() + keyId;
        return std::min(mSampledNormalizedSquaredLengthCache[index], mMaxPointToKeyLength);
    }
    if (CharUtils::isIntentionalOmissionCodePoint(codePoint)) {
        return 0.0f;
    }
    // If the char is not a key on the keyboard then return the max length.
    return static_cast<float>(MAX_VALUE_FOR_WEIGHTING);
}

float ProximityInfoState::getPointToKeyByIdLength(
        const int inputIndex, const int keyId) const {
    return ProximityInfoStateUtils::getPointToKeyByIdLength(mMaxPointToKeyLength,
            &mSampledNormalizedSquaredLengthCache, mProximityInfo->getKeyCount(), inputIndex,
            keyId);
}

// In the following function, c is the current character of the dictionary word currently examined.
// currentChars is an array containing the keys close to the character the user actually typed at
// the same position. We want to see if c is in it: if so, then the word contains at that position
// a character close to what the user typed.
// What the user typed is actually the first character of the array.
// proximityIndex is a pointer to the variable where getProximityType returns the index of c
// in the proximity chars of the input index.
// Notice : accented characters do not have a proximity list, so they are alone in their list. The
// non-accented version of the character should be considered "close", but not the other keys close
// to the non-accented version.
ProximityType ProximityInfoState::getProximityType(const int index, const int codePoint,
        const bool checkProximityChars, int *proximityIndex) const {
    const int *currentCodePoints = getProximityCodePointsAt(index);
    const int firstCodePoint = currentCodePoints[0];
    const int baseLowerC = CharUtils::toBaseLowerCase(codePoint);

    // The first char in the array is what user typed. If it matches right away, that means the
    // user typed that same char for this pos.
    if (firstCodePoint == baseLowerC || firstCodePoint == codePoint) {
        return MATCH_CHAR;
    }

    if (!checkProximityChars) return SUBSTITUTION_CHAR;

    // If the non-accented, lowercased version of that first character matches c, then we have a
    // non-accented version of the accented character the user typed. Treat it as a close char.
    if (CharUtils::toBaseLowerCase(firstCodePoint) == baseLowerC) {
        return PROXIMITY_CHAR;
    }

    // Not an exact nor an accent-alike match: search the list of close keys
    int j = 1;
    while (j < MAX_PROXIMITY_CHARS_SIZE
            && currentCodePoints[j] > ADDITIONAL_PROXIMITY_CHAR_DELIMITER_CODE) {
        const bool matched = (currentCodePoints[j] == baseLowerC
                || currentCodePoints[j] == codePoint);
        if (matched) {
            if (proximityIndex) {
                *proximityIndex = j;
            }
            return PROXIMITY_CHAR;
        }
        ++j;
    }
    if (j < MAX_PROXIMITY_CHARS_SIZE
            && currentCodePoints[j] == ADDITIONAL_PROXIMITY_CHAR_DELIMITER_CODE) {
        ++j;
        while (j < MAX_PROXIMITY_CHARS_SIZE
                && currentCodePoints[j] > ADDITIONAL_PROXIMITY_CHAR_DELIMITER_CODE) {
            const bool matched = (currentCodePoints[j] == baseLowerC
                    || currentCodePoints[j] == codePoint);
            if (matched) {
                if (proximityIndex) {
                    *proximityIndex = j;
                }
                return ADDITIONAL_PROXIMITY_CHAR;
            }
            ++j;
        }
    }
    // Was not included, signal this as a substitution character.
    return SUBSTITUTION_CHAR;
}

ProximityType ProximityInfoState::getProximityTypeG(const int index, const int codePoint) const {
    if (!isUsed()) {
        return UNRELATED_CHAR;
    }
    const int sampledSearchKeyVectorsSize = static_cast<int>(mSampledSearchKeyVectors.size());
    if (index < 0 || index >= sampledSearchKeyVectorsSize) {
        AKLOGE("getProximityTypeG() is called with an invalid index(%d). "
                "mSampledSearchKeyVectors.size() = %d, codePoint = %x.", index,
                sampledSearchKeyVectorsSize, codePoint);
        ASSERT(false);
        return UNRELATED_CHAR;
    }
    const int lowerCodePoint = CharUtils::toLowerCase(codePoint);
    const int baseLowerCodePoint = CharUtils::toBaseCodePoint(lowerCodePoint);
    for (int i = 0; i < static_cast<int>(mSampledSearchKeyVectors[index].size()); ++i) {
        if (mSampledSearchKeyVectors[index][i] == lowerCodePoint
                || mSampledSearchKeyVectors[index][i] == baseLowerCodePoint) {
            return MATCH_CHAR;
        }
    }
    return UNRELATED_CHAR;
}

bool ProximityInfoState::isKeyInSerchKeysAfterIndex(const int index, const int keyId) const {
    ASSERT(keyId >= 0 && index >= 0 && index < mSampledInputSize);
    return mSampledSearchKeySets[index].test(keyId);
}

float ProximityInfoState::getDirection(const int index0, const int index1) const {
    return ProximityInfoStateUtils::getDirection(
            &mSampledInputXs, &mSampledInputYs, index0, index1);
}

float ProximityInfoState::getMostProbableString(int *const codePointBuf) const {
    memmove(codePointBuf, mMostProbableString, sizeof(mMostProbableString));
    return mMostProbableStringProbability;
}

bool ProximityInfoState::hasSpaceProximity(const int index) const {
    ASSERT(0 <= index && index < mSampledInputSize);
    return mProximityInfo->hasSpaceProximity(getInputX(index), getInputY(index));
}

// Returns a probability of mapping index to keyIndex.
float ProximityInfoState::getProbability(const int index, const int keyIndex) const {
    ASSERT(0 <= index && index < mSampledInputSize);
    std::unordered_map<int, float>::const_iterator it = mCharProbabilities[index].find(keyIndex);
    if (it != mCharProbabilities[index].end()) {
        return it->second;
    }
    return static_cast<float>(MAX_VALUE_FOR_WEIGHTING);
}
} // namespace latinime
