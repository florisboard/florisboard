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

#define LOG_TAG "LatinIME: proximity_info.cpp"

#include "suggest/core/layout/proximity_info.h"

#include <algorithm>
#include <cstring>
#include <cmath>

#include "defines.h"
#include "jni.h"
#include "suggest/core/layout/additional_proximity_chars.h"
#include "suggest/core/layout/geometry_utils.h"
#include "suggest/core/layout/proximity_info_params.h"
#include "utils/char_utils.h"

namespace latinime {

static AK_FORCE_INLINE void safeGetOrFillZeroIntArrayRegion(JNIEnv *env, jintArray jArray,
        jsize len, jint *buffer) {
    if (jArray && buffer) {
        env->GetIntArrayRegion(jArray, 0, len, buffer);
    } else if (buffer) {
        memset(buffer, 0, len * sizeof(buffer[0]));
    }
}

static AK_FORCE_INLINE void safeGetOrFillZeroFloatArrayRegion(JNIEnv *env, jfloatArray jArray,
        jsize len, jfloat *buffer) {
    if (jArray && buffer) {
        env->GetFloatArrayRegion(jArray, 0, len, buffer);
    } else if (buffer) {
        memset(buffer, 0, len * sizeof(buffer[0]));
    }
}

ProximityInfo::ProximityInfo(JNIEnv *env, const int keyboardWidth, const int keyboardHeight,
        const int gridWidth, const int gridHeight, const int mostCommonKeyWidth,
        const int mostCommonKeyHeight, const jintArray proximityChars, const int keyCount,
        const jintArray keyXCoordinates, const jintArray keyYCoordinates,
        const jintArray keyWidths, const jintArray keyHeights, const jintArray keyCharCodes,
        const jfloatArray sweetSpotCenterXs, const jfloatArray sweetSpotCenterYs,
        const jfloatArray sweetSpotRadii)
        : GRID_WIDTH(gridWidth), GRID_HEIGHT(gridHeight), MOST_COMMON_KEY_WIDTH(mostCommonKeyWidth),
          MOST_COMMON_KEY_WIDTH_SQUARE(mostCommonKeyWidth * mostCommonKeyWidth),
          NORMALIZED_SQUARED_MOST_COMMON_KEY_HYPOTENUSE(1.0f +
                  GeometryUtils::SQUARE_FLOAT(static_cast<float>(mostCommonKeyHeight) /
                          static_cast<float>(mostCommonKeyWidth))),
          CELL_WIDTH((keyboardWidth + gridWidth - 1) / gridWidth),
          CELL_HEIGHT((keyboardHeight + gridHeight - 1) / gridHeight),
          KEY_COUNT(std::min(keyCount, MAX_KEY_COUNT_IN_A_KEYBOARD)),
          KEYBOARD_WIDTH(keyboardWidth), KEYBOARD_HEIGHT(keyboardHeight),
          KEYBOARD_HYPOTENUSE(hypotf(KEYBOARD_WIDTH, KEYBOARD_HEIGHT)),
          HAS_TOUCH_POSITION_CORRECTION_DATA(keyCount > 0 && keyXCoordinates && keyYCoordinates
                  && keyWidths && keyHeights && keyCharCodes && sweetSpotCenterXs
                  && sweetSpotCenterYs && sweetSpotRadii),
          mProximityCharsArray(new int[GRID_WIDTH * GRID_HEIGHT * MAX_PROXIMITY_CHARS_SIZE
                  /* proximityCharsLength */]),
          mLowerCodePointToKeyMap() {
    /* Let's check the input array length here to make sure */
    const jsize proximityCharsLength = env->GetArrayLength(proximityChars);
    if (proximityCharsLength != GRID_WIDTH * GRID_HEIGHT * MAX_PROXIMITY_CHARS_SIZE) {
        AKLOGE("Invalid proximityCharsLength: %d", proximityCharsLength);
        ASSERT(false);
        return;
    }
    if (DEBUG_PROXIMITY_INFO) {
        AKLOGI("Create proximity info array %d", proximityCharsLength);
    }
    safeGetOrFillZeroIntArrayRegion(env, proximityChars, proximityCharsLength,
            mProximityCharsArray);
    safeGetOrFillZeroIntArrayRegion(env, keyXCoordinates, KEY_COUNT, mKeyXCoordinates);
    safeGetOrFillZeroIntArrayRegion(env, keyYCoordinates, KEY_COUNT, mKeyYCoordinates);
    safeGetOrFillZeroIntArrayRegion(env, keyWidths, KEY_COUNT, mKeyWidths);
    safeGetOrFillZeroIntArrayRegion(env, keyHeights, KEY_COUNT, mKeyHeights);
    safeGetOrFillZeroIntArrayRegion(env, keyCharCodes, KEY_COUNT, mKeyCodePoints);
    safeGetOrFillZeroFloatArrayRegion(env, sweetSpotCenterXs, KEY_COUNT, mSweetSpotCenterXs);
    safeGetOrFillZeroFloatArrayRegion(env, sweetSpotCenterYs, KEY_COUNT, mSweetSpotCenterYs);
    safeGetOrFillZeroFloatArrayRegion(env, sweetSpotRadii, KEY_COUNT, mSweetSpotRadii);
    initializeG();
}

ProximityInfo::~ProximityInfo() {
    delete[] mProximityCharsArray;
}

bool ProximityInfo::hasSpaceProximity(const int x, const int y) const {
    if (x < 0 || y < 0) {
        if (DEBUG_DICT) {
            AKLOGI("HasSpaceProximity: Illegal coordinates (%d, %d)", x, y);
            // TODO: Enable this assertion.
            //ASSERT(false);
        }
        return false;
    }

    const int startIndex = ProximityInfoUtils::getStartIndexFromCoordinates(x, y,
            CELL_HEIGHT, CELL_WIDTH, GRID_WIDTH);
    if (DEBUG_PROXIMITY_INFO) {
        AKLOGI("hasSpaceProximity: index %d, %d, %d", startIndex, x, y);
    }
    int *proximityCharsArray = mProximityCharsArray;
    for (int i = 0; i < MAX_PROXIMITY_CHARS_SIZE; ++i) {
        if (DEBUG_PROXIMITY_INFO) {
            AKLOGI("Index: %d", mProximityCharsArray[startIndex + i]);
        }
        if (proximityCharsArray[startIndex + i] == KEYCODE_SPACE) {
            return true;
        }
    }
    return false;
}

float ProximityInfo::getNormalizedSquaredDistanceFromCenterFloatG(
        const int keyId, const int x, const int y, const bool isGeometric) const {
    const float centerX = static_cast<float>(getKeyCenterXOfKeyIdG(keyId, x, isGeometric));
    const float centerY = static_cast<float>(getKeyCenterYOfKeyIdG(keyId, y, isGeometric));
    const float touchX = static_cast<float>(x);
    const float touchY = static_cast<float>(y);
    return ProximityInfoUtils::getSquaredDistanceFloat(centerX, centerY, touchX, touchY)
            / GeometryUtils::SQUARE_FLOAT(static_cast<float>(getMostCommonKeyWidth()));
}

int ProximityInfo::getCodePointOf(const int keyIndex) const {
    if (keyIndex < 0 || keyIndex >= KEY_COUNT) {
        return NOT_A_CODE_POINT;
    }
    return mKeyIndexToLowerCodePointG[keyIndex];
}

int ProximityInfo::getOriginalCodePointOf(const int keyIndex) const {
    if (keyIndex < 0 || keyIndex >= KEY_COUNT) {
        return NOT_A_CODE_POINT;
    }
    return mKeyIndexToOriginalCodePoint[keyIndex];
}

void ProximityInfo::initializeG() {
    // TODO: Optimize
    for (int i = 0; i < KEY_COUNT; ++i) {
        const int code = mKeyCodePoints[i];
        const int lowerCode = CharUtils::toLowerCase(code);
        mCenterXsG[i] = mKeyXCoordinates[i] + mKeyWidths[i] / 2;
        mCenterYsG[i] = mKeyYCoordinates[i] + mKeyHeights[i] / 2;
        if (hasTouchPositionCorrectionData()) {
            // Computes sweet spot center points for geometric input.
            const float verticalScale = ProximityInfoParams::VERTICAL_SWEET_SPOT_SCALE_G;
            const float sweetSpotCenterY = static_cast<float>(mSweetSpotCenterYs[i]);
            const float gapY = sweetSpotCenterY - mCenterYsG[i];
            mSweetSpotCenterYsG[i] = static_cast<int>(mCenterYsG[i] + gapY * verticalScale);
        }
        mLowerCodePointToKeyMap[lowerCode] = i;
        mKeyIndexToOriginalCodePoint[i] = code;
        mKeyIndexToLowerCodePointG[i] = lowerCode;
    }
    for (int i = 0; i < KEY_COUNT; i++) {
        mKeyKeyDistancesG[i][i] = 0;
        for (int j = i + 1; j < KEY_COUNT; j++) {
            if (hasTouchPositionCorrectionData()) {
                // Computes distances using sweet spots if they exist.
                // We have two types of Y coordinate sweet spots, for geometric and for the others.
                // The sweet spots for geometric input are used for calculating key-key distances
                // here.
                mKeyKeyDistancesG[i][j] = GeometryUtils::getDistanceInt(
                        mSweetSpotCenterXs[i], mSweetSpotCenterYsG[i],
                        mSweetSpotCenterXs[j], mSweetSpotCenterYsG[j]);
            } else {
                mKeyKeyDistancesG[i][j] = GeometryUtils::getDistanceInt(
                        mCenterXsG[i], mCenterYsG[i], mCenterXsG[j], mCenterYsG[j]);
            }
            mKeyKeyDistancesG[j][i] = mKeyKeyDistancesG[i][j];
        }
    }
}

// referencePointX is used only for keys wider than most common key width. When the referencePointX
// is NOT_A_COORDINATE, this method calculates the return value without using the line segment.
// isGeometric is currently not used because we don't have extra X coordinates sweet spots for
// geometric input.
int ProximityInfo::getKeyCenterXOfKeyIdG(
        const int keyId, const int referencePointX, const bool isGeometric) const {
    if (keyId < 0) {
        return 0;
    }
    int centerX = (hasTouchPositionCorrectionData()) ? static_cast<int>(mSweetSpotCenterXs[keyId])
            : mCenterXsG[keyId];
    const int keyWidth = mKeyWidths[keyId];
    if (referencePointX != NOT_A_COORDINATE
            && keyWidth > getMostCommonKeyWidth()) {
        // For keys wider than most common keys, we use a line segment instead of the center point;
        // thus, centerX is adjusted depending on referencePointX.
        const int keyWidthHalfDiff = (keyWidth - getMostCommonKeyWidth()) / 2;
        if (referencePointX < centerX - keyWidthHalfDiff) {
            centerX -= keyWidthHalfDiff;
        } else if (referencePointX > centerX + keyWidthHalfDiff) {
            centerX += keyWidthHalfDiff;
        } else {
            centerX = referencePointX;
        }
    }
    return centerX;
}

// When the referencePointY is NOT_A_COORDINATE, this method calculates the return value without
// using the line segment.
int ProximityInfo::getKeyCenterYOfKeyIdG(
        const int keyId, const int referencePointY, const bool isGeometric) const {
    // TODO: Remove "isGeometric" and have separate "proximity_info"s for gesture and typing.
    if (keyId < 0) {
        return 0;
    }
    int centerY;
    if (!hasTouchPositionCorrectionData()) {
        centerY = mCenterYsG[keyId];
    } else if (isGeometric) {
        centerY = static_cast<int>(mSweetSpotCenterYsG[keyId]);
    } else {
        centerY = static_cast<int>(mSweetSpotCenterYs[keyId]);
    }
    if (referencePointY != NOT_A_COORDINATE &&
            centerY + mKeyHeights[keyId] > KEYBOARD_HEIGHT && centerY < referencePointY) {
        // When the distance between center point and bottom edge of the keyboard is shorter than
        // the key height, we assume the key is located at the bottom row of the keyboard.
        // The center point is extended to the bottom edge for such keys.
        return referencePointY;
    }
    return centerY;
}

int ProximityInfo::getKeyKeyDistanceG(const int keyId0, const int keyId1) const {
    if (keyId0 >= 0 && keyId1 >= 0) {
        return mKeyKeyDistancesG[keyId0][keyId1];
    }
    return MAX_VALUE_FOR_WEIGHTING;
}
} // namespace latinime
