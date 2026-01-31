/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef LATINIME_PROXIMITY_INFO_H
#define LATINIME_PROXIMITY_INFO_H

#include <unordered_map>
#include <vector>

#include "defines.h"
#include "jni.h"
#include "suggest/core/layout/proximity_info_utils.h"

namespace latinime {

class ProximityInfo {
 public:
    ProximityInfo(JNIEnv *env, const int keyboardWidth, const int keyboardHeight,
            const int gridWidth, const int gridHeight,
            const int mostCommonKeyWidth, const int mostCommonKeyHeight,
            const jintArray proximityChars, const int keyCount, const jintArray keyXCoordinates,
            const jintArray keyYCoordinates, const jintArray keyWidths, const jintArray keyHeights,
            const jintArray keyCharCodes, const jfloatArray sweetSpotCenterXs,
            const jfloatArray sweetSpotCenterYs, const jfloatArray sweetSpotRadii);
    ~ProximityInfo();
    bool hasSpaceProximity(const int x, const int y) const;
    float getNormalizedSquaredDistanceFromCenterFloatG(
            const int keyId, const int x, const int y, const bool isGeometric) const;
    int getCodePointOf(const int keyIndex) const;
    int getOriginalCodePointOf(const int keyIndex) const;
    bool hasSweetSpotData(const int keyIndex) const {
        // When there are no calibration data for a key,
        // the radius of the key is assigned to zero.
        return mSweetSpotRadii[keyIndex] > 0.0f;
    }
    float getSweetSpotRadiiAt(int keyIndex) const { return mSweetSpotRadii[keyIndex]; }
    float getSweetSpotCenterXAt(int keyIndex) const { return mSweetSpotCenterXs[keyIndex]; }
    float getSweetSpotCenterYAt(int keyIndex) const { return mSweetSpotCenterYs[keyIndex]; }
    bool hasTouchPositionCorrectionData() const { return HAS_TOUCH_POSITION_CORRECTION_DATA; }
    int getMostCommonKeyWidth() const { return MOST_COMMON_KEY_WIDTH; }
    int getMostCommonKeyWidthSquare() const { return MOST_COMMON_KEY_WIDTH_SQUARE; }
    float getNormalizedSquaredMostCommonKeyHypotenuse() const {
        return NORMALIZED_SQUARED_MOST_COMMON_KEY_HYPOTENUSE;
    }
    int getKeyCount() const { return KEY_COUNT; }
    int getCellHeight() const { return CELL_HEIGHT; }
    int getCellWidth() const { return CELL_WIDTH; }
    int getGridWidth() const { return GRID_WIDTH; }
    int getGridHeight() const { return GRID_HEIGHT; }
    int getKeyboardWidth() const { return KEYBOARD_WIDTH; }
    int getKeyboardHeight() const { return KEYBOARD_HEIGHT; }
    float getKeyboardHypotenuse() const { return KEYBOARD_HYPOTENUSE; }

    int getKeyCenterXOfKeyIdG(
            const int keyId, const int referencePointX, const bool isGeometric) const;
    int getKeyCenterYOfKeyIdG(
            const int keyId, const int referencePointY, const bool isGeometric) const;
    int getKeyKeyDistanceG(int keyId0, int keyId1) const;

    AK_FORCE_INLINE void initializeProximities(const int *const inputCodes,
            const int *const inputXCoordinates, const int *const inputYCoordinates,
            const int inputSize, int *allInputCodes, const std::vector<int> *locale) const {
        ProximityInfoUtils::initializeProximities(inputCodes, inputXCoordinates, inputYCoordinates,
                inputSize, mKeyXCoordinates, mKeyYCoordinates, mKeyWidths, mKeyHeights,
                mProximityCharsArray, CELL_HEIGHT, CELL_WIDTH, GRID_WIDTH, MOST_COMMON_KEY_WIDTH,
                KEY_COUNT, locale, &mLowerCodePointToKeyMap, allInputCodes);
    }

    AK_FORCE_INLINE int getKeyIndexOf(const int c) const {
        return ProximityInfoUtils::getKeyIndexOf(KEY_COUNT, c, &mLowerCodePointToKeyMap);
    }

    AK_FORCE_INLINE bool isCodePointOnKeyboard(const int codePoint) const {
        return getKeyIndexOf(codePoint) != NOT_AN_INDEX;
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(ProximityInfo);

    void initializeG();

    const int GRID_WIDTH;
    const int GRID_HEIGHT;
    const int MOST_COMMON_KEY_WIDTH;
    const int MOST_COMMON_KEY_WIDTH_SQUARE;
    const float NORMALIZED_SQUARED_MOST_COMMON_KEY_HYPOTENUSE;
    const int CELL_WIDTH;
    const int CELL_HEIGHT;
    const int KEY_COUNT;
    const int KEYBOARD_WIDTH;
    const int KEYBOARD_HEIGHT;
    const float KEYBOARD_HYPOTENUSE;
    const bool HAS_TOUCH_POSITION_CORRECTION_DATA;
    int *mProximityCharsArray;
    int mKeyXCoordinates[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mKeyYCoordinates[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mKeyWidths[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mKeyHeights[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mKeyCodePoints[MAX_KEY_COUNT_IN_A_KEYBOARD];
    float mSweetSpotCenterXs[MAX_KEY_COUNT_IN_A_KEYBOARD];
    float mSweetSpotCenterYs[MAX_KEY_COUNT_IN_A_KEYBOARD];
    // Sweet spots for geometric input. Note that we have extra sweet spots only for Y coordinates.
    float mSweetSpotCenterYsG[MAX_KEY_COUNT_IN_A_KEYBOARD];
    float mSweetSpotRadii[MAX_KEY_COUNT_IN_A_KEYBOARD];
    std::unordered_map<int, int> mLowerCodePointToKeyMap;
    int mKeyIndexToOriginalCodePoint[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mKeyIndexToLowerCodePointG[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mCenterXsG[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mCenterYsG[MAX_KEY_COUNT_IN_A_KEYBOARD];
    int mKeyKeyDistancesG[MAX_KEY_COUNT_IN_A_KEYBOARD][MAX_KEY_COUNT_IN_A_KEYBOARD];
};
} // namespace latinime
#endif // LATINIME_PROXIMITY_INFO_H
