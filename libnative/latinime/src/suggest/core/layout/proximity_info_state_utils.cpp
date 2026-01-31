/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "suggest/core/layout/proximity_info_state_utils.h"

#include <algorithm>
#include <cmath>
#include <cstring> // for memset()
#include <sstream> // for debug prints
#include <unordered_map>
#include <vector>

#include "defines.h"
#include "suggest/core/layout/geometry_utils.h"
#include "suggest/core/layout/normal_distribution_2d.h"
#include "suggest/core/layout/proximity_info.h"
#include "suggest/core/layout/proximity_info_params.h"

namespace latinime {

/* static */ int ProximityInfoStateUtils::trimLastTwoTouchPoints(std::vector<int> *sampledInputXs,
        std::vector<int> *sampledInputYs, std::vector<int> *sampledInputTimes,
        std::vector<int> *sampledLengthCache, std::vector<int> *sampledInputIndice) {
    const int nextStartIndex = (*sampledInputIndice)[sampledInputIndice->size() - 2];
    popInputData(sampledInputXs, sampledInputYs, sampledInputTimes, sampledLengthCache,
            sampledInputIndice);
    popInputData(sampledInputXs, sampledInputYs, sampledInputTimes, sampledLengthCache,
            sampledInputIndice);
    return nextStartIndex;
}

/* static */ int ProximityInfoStateUtils::updateTouchPoints(
        const ProximityInfo *const proximityInfo, const int maxPointToKeyLength,
        const int *const inputProximities, const int *const inputXCoordinates,
        const int *const inputYCoordinates, const int *const times, const int *const pointerIds,
        const int inputSize, const bool isGeometric, const int pointerId,
        const int pushTouchPointStartIndex, std::vector<int> *sampledInputXs,
        std::vector<int> *sampledInputYs, std::vector<int> *sampledInputTimes,
        std::vector<int> *sampledLengthCache, std::vector<int> *sampledInputIndice) {
    if (DEBUG_SAMPLING_POINTS) {
        if (times) {
            for (int i = 0; i < inputSize; ++i) {
                AKLOGI("(%d) x %d, y %d, time %d",
                        i, inputXCoordinates[i], inputYCoordinates[i], times[i]);
            }
        }
    }
#ifdef DO_ASSERT_TEST
    if (times) {
        for (int i = 0; i < inputSize; ++i) {
            if (i > 0) {
                if (times[i] < times[i - 1]) {
                    AKLOGI("Invalid time sequence. %d, %d", times[i - 1], times[i]);
                    ASSERT(false);
                }
            }
        }
    }
#endif
    const bool proximityOnly = !isGeometric
            && (inputXCoordinates[0] < 0 || inputYCoordinates[0] < 0);
    int lastInputIndex = pushTouchPointStartIndex;
    for (int i = lastInputIndex; i < inputSize; ++i) {
        const int pid = pointerIds ? pointerIds[i] : 0;
        if (pointerId == pid) {
            lastInputIndex = i;
        }
    }
    if (DEBUG_GEO_FULL) {
        AKLOGI("Init ProximityInfoState: last input index = %d", lastInputIndex);
    }
    // Working space to save near keys distances for current, prev and prevprev input point.
    NearKeysDistanceMap nearKeysDistances[3];
    // These pointers are swapped for each inputs points.
    NearKeysDistanceMap *currentNearKeysDistances = &nearKeysDistances[0];
    NearKeysDistanceMap *prevNearKeysDistances = &nearKeysDistances[1];
    NearKeysDistanceMap *prevPrevNearKeysDistances = &nearKeysDistances[2];
    // "sumAngle" is accumulated by each angle of input points. And when "sumAngle" exceeds
    // the threshold we save that point, reset sumAngle. This aims to keep the figure of
    // the curve.
    float sumAngle = 0.0f;

    for (int i = pushTouchPointStartIndex; i <= lastInputIndex; ++i) {
        // Assuming pointerId == 0 if pointerIds is null.
        const int pid = pointerIds ? pointerIds[i] : 0;
        if (DEBUG_GEO_FULL) {
            AKLOGI("Init ProximityInfoState: (%d)PID = %d", i, pid);
        }
        if (pointerId == pid) {
            const int c = isGeometric ?
                    NOT_A_COORDINATE : getPrimaryCodePointAt(inputProximities, i);
            const int x = proximityOnly ? NOT_A_COORDINATE : inputXCoordinates[i];
            const int y = proximityOnly ? NOT_A_COORDINATE : inputYCoordinates[i];
            const int time = times ? times[i] : -1;

            if (i > 1) {
                const float prevAngle = GeometryUtils::getAngle(
                        inputXCoordinates[i - 2], inputYCoordinates[i - 2],
                        inputXCoordinates[i - 1], inputYCoordinates[i - 1]);
                const float currentAngle = GeometryUtils::getAngle(
                        inputXCoordinates[i - 1], inputYCoordinates[i - 1], x, y);
                sumAngle += GeometryUtils::getAngleDiff(prevAngle, currentAngle);
            }

            if (pushTouchPoint(proximityInfo, maxPointToKeyLength, i, c, x, y, time,
                    isGeometric, isGeometric /* doSampling */, i == lastInputIndex,
                    sumAngle, currentNearKeysDistances, prevNearKeysDistances,
                    prevPrevNearKeysDistances, sampledInputXs, sampledInputYs, sampledInputTimes,
                    sampledLengthCache, sampledInputIndice)) {
                // Previous point information was popped.
                NearKeysDistanceMap *tmp = prevNearKeysDistances;
                prevNearKeysDistances = currentNearKeysDistances;
                currentNearKeysDistances = tmp;
            } else {
                NearKeysDistanceMap *tmp = prevPrevNearKeysDistances;
                prevPrevNearKeysDistances = prevNearKeysDistances;
                prevNearKeysDistances = currentNearKeysDistances;
                currentNearKeysDistances = tmp;
                sumAngle = 0.0f;
            }
        }
    }
    return sampledInputXs->size();
}

/* static */ const int *ProximityInfoStateUtils::getProximityCodePointsAt(
        const int *const inputProximities, const int index) {
    return inputProximities + (index * MAX_PROXIMITY_CHARS_SIZE);
}

/* static */ int ProximityInfoStateUtils::getPrimaryCodePointAt(const int *const inputProximities,
        const int index) {
    return getProximityCodePointsAt(inputProximities, index)[0];
}

/* static */ void ProximityInfoStateUtils::initPrimaryInputWord(const int inputSize,
        const int *const inputProximities, int *primaryInputWord) {
    memset(primaryInputWord, 0, sizeof(primaryInputWord[0]) * MAX_WORD_LENGTH);
    for (int i = 0; i < inputSize; ++i) {
        primaryInputWord[i] = getPrimaryCodePointAt(inputProximities, i);
    }
}

/* static */ float ProximityInfoStateUtils::calculateSquaredDistanceFromSweetSpotCenter(
        const ProximityInfo *const proximityInfo, const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs, const int keyIndex, const int inputIndex) {
    const float sweetSpotCenterX = proximityInfo->getSweetSpotCenterXAt(keyIndex);
    const float sweetSpotCenterY = proximityInfo->getSweetSpotCenterYAt(keyIndex);
    const float inputX = static_cast<float>((*sampledInputXs)[inputIndex]);
    const float inputY = static_cast<float>((*sampledInputYs)[inputIndex]);
    return GeometryUtils::SQUARE_FLOAT(inputX - sweetSpotCenterX)
            + GeometryUtils::SQUARE_FLOAT(inputY - sweetSpotCenterY);
}

/* static */ float ProximityInfoStateUtils::calculateNormalizedSquaredDistance(
        const ProximityInfo *const proximityInfo, const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs, const int keyIndex, const int inputIndex) {
    if (keyIndex == NOT_AN_INDEX) {
        return ProximityInfoParams::NOT_A_DISTANCE_FLOAT;
    }
    if (!proximityInfo->hasSweetSpotData(keyIndex)) {
        return ProximityInfoParams::NOT_A_DISTANCE_FLOAT;
    }
    if (NOT_A_COORDINATE == (*sampledInputXs)[inputIndex]) {
        return ProximityInfoParams::NOT_A_DISTANCE_FLOAT;
    }
    const float squaredDistance = calculateSquaredDistanceFromSweetSpotCenter(proximityInfo,
            sampledInputXs, sampledInputYs, keyIndex, inputIndex);
    const float squaredRadius = GeometryUtils::SQUARE_FLOAT(
            proximityInfo->getSweetSpotRadiiAt(keyIndex));
    return squaredDistance / squaredRadius;
}

/* static */ void ProximityInfoStateUtils::initGeometricDistanceInfos(
        const ProximityInfo *const proximityInfo, const int sampledInputSize,
        const int lastSavedInputSize, const bool isGeometric,
        const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs,
        std::vector<float> *sampledNormalizedSquaredLengthCache) {
    const int keyCount = proximityInfo->getKeyCount();
    sampledNormalizedSquaredLengthCache->resize(sampledInputSize * keyCount);
    for (int i = lastSavedInputSize; i < sampledInputSize; ++i) {
        for (int k = 0; k < keyCount; ++k) {
            const int index = i * keyCount + k;
            const int x = (*sampledInputXs)[i];
            const int y = (*sampledInputYs)[i];
            const float normalizedSquaredDistance =
                    proximityInfo->getNormalizedSquaredDistanceFromCenterFloatG(
                            k, x, y, isGeometric);
            (*sampledNormalizedSquaredLengthCache)[index] = normalizedSquaredDistance;
        }
    }
}

/* static */ void ProximityInfoStateUtils::popInputData(std::vector<int> *sampledInputXs,
        std::vector<int> *sampledInputYs, std::vector<int> *sampledInputTimes,
        std::vector<int> *sampledLengthCache, std::vector<int> *sampledInputIndice) {
    sampledInputXs->pop_back();
    sampledInputYs->pop_back();
    sampledInputTimes->pop_back();
    sampledLengthCache->pop_back();
    sampledInputIndice->pop_back();
}

/* static */ float ProximityInfoStateUtils::refreshSpeedRates(const int inputSize,
        const int *const xCoordinates, const int *const yCoordinates, const int *const times,
        const int lastSavedInputSize, const int sampledInputSize,
        const std::vector<int> *const sampledInputXs, const std::vector<int> *const sampledInputYs,
        const std::vector<int> *const sampledInputTimes,
        const std::vector<int> *const sampledLengthCache,
        const std::vector<int> *const sampledInputIndice, std::vector<float> *sampledSpeedRates,
        std::vector<float> *sampledDirections) {
    // Relative speed calculation.
    const int sumDuration = sampledInputTimes->back() - sampledInputTimes->front();
    const int sumLength = sampledLengthCache->back() - sampledLengthCache->front();
    const float averageSpeed = static_cast<float>(sumLength) / static_cast<float>(sumDuration);
    sampledSpeedRates->resize(sampledInputSize);
    for (int i = lastSavedInputSize; i < sampledInputSize; ++i) {
        const int index = (*sampledInputIndice)[i];
        int length = 0;
        int duration = 0;

        // Calculate velocity by using distances and durations of
        // ProximityInfoParams::NUM_POINTS_FOR_SPEED_CALCULATION points for both forward and
        // backward.
        const int forwardNumPoints = std::min(inputSize - 1,
                index + ProximityInfoParams::NUM_POINTS_FOR_SPEED_CALCULATION);
        for (int j = index; j < forwardNumPoints; ++j) {
            if (i < sampledInputSize - 1 && j >= (*sampledInputIndice)[i + 1]) {
                break;
            }
            length += GeometryUtils::getDistanceInt(xCoordinates[j], yCoordinates[j],
                    xCoordinates[j + 1], yCoordinates[j + 1]);
            duration += times[j + 1] - times[j];
        }
        const int backwardNumPoints = std::max(0,
                index - ProximityInfoParams::NUM_POINTS_FOR_SPEED_CALCULATION);
        for (int j = index - 1; j >= backwardNumPoints; --j) {
            if (i > 0 && j < (*sampledInputIndice)[i - 1]) {
                break;
            }
            // TODO: use mSampledLengthCache instead?
            length += GeometryUtils::getDistanceInt(xCoordinates[j], yCoordinates[j],
                    xCoordinates[j + 1], yCoordinates[j + 1]);
            duration += times[j + 1] - times[j];
        }
        if (duration == 0 || sumDuration == 0) {
            // Cannot calculate speed; thus, it gives an average value (1.0);
            (*sampledSpeedRates)[i] = 1.0f;
        } else {
            const float speed = static_cast<float>(length) / static_cast<float>(duration);
            (*sampledSpeedRates)[i] = speed / averageSpeed;
        }
    }

    // Direction calculation.
    sampledDirections->resize(sampledInputSize - 1);
    for (int i = std::max(0, lastSavedInputSize - 1); i < sampledInputSize - 1; ++i) {
        (*sampledDirections)[i] = getDirection(sampledInputXs, sampledInputYs, i, i + 1);
    }
    return averageSpeed;
}

/* static */ void ProximityInfoStateUtils::refreshBeelineSpeedRates(const int mostCommonKeyWidth,
        const float averageSpeed, const int inputSize, const int *const xCoordinates,
        const int *const yCoordinates, const int *times, const int sampledInputSize,
        const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs, const std::vector<int> *const inputIndice,
        std::vector<int> *beelineSpeedPercentiles) {
    if (DEBUG_SAMPLING_POINTS) {
        AKLOGI("--- refresh beeline speed rates");
    }
    beelineSpeedPercentiles->resize(sampledInputSize);
    for (int i = 0; i < sampledInputSize; ++i) {
        (*beelineSpeedPercentiles)[i] = static_cast<int>(calculateBeelineSpeedRate(
                mostCommonKeyWidth, averageSpeed, i, inputSize, xCoordinates, yCoordinates, times,
                sampledInputSize, sampledInputXs, sampledInputYs, inputIndice) * MAX_PERCENTILE);
    }
}

/* static */float ProximityInfoStateUtils::getDirection(
        const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs, const int index0, const int index1) {
    ASSERT(sampledInputXs && sampledInputYs);
    const int sampledInputSize =sampledInputXs->size();
    if (index0 < 0 || index0 > sampledInputSize - 1) {
        return 0.0f;
    }
    if (index1 < 0 || index1 > sampledInputSize - 1) {
        return 0.0f;
    }
    const int x1 = (*sampledInputXs)[index0];
    const int y1 = (*sampledInputYs)[index0];
    const int x2 = (*sampledInputXs)[index1];
    const int y2 = (*sampledInputYs)[index1];
    return GeometryUtils::getAngle(x1, y1, x2, y2);
}

// Calculating point to key distance for all near keys and returning the distance between
// the given point and the nearest key position.
/* static */ float ProximityInfoStateUtils::updateNearKeysDistances(
        const ProximityInfo *const proximityInfo, const float maxPointToKeyLength, const int x,
        const int y, const bool isGeometric, NearKeysDistanceMap *const currentNearKeysDistances) {
    currentNearKeysDistances->clear();
    const int keyCount = proximityInfo->getKeyCount();
    float nearestKeyDistance = maxPointToKeyLength;
    for (int k = 0; k < keyCount; ++k) {
        const float dist = proximityInfo->getNormalizedSquaredDistanceFromCenterFloatG(k, x, y,
                isGeometric);
        if (dist < ProximityInfoParams::NEAR_KEY_THRESHOLD_FOR_DISTANCE) {
            currentNearKeysDistances->insert(std::pair<int, float>(k, dist));
        }
        if (nearestKeyDistance > dist) {
            nearestKeyDistance = dist;
        }
    }
    return nearestKeyDistance;
}

// Check if previous point is at local minimum position to near keys.
/* static */ bool ProximityInfoStateUtils::isPrevLocalMin(
        const NearKeysDistanceMap *const currentNearKeysDistances,
        const NearKeysDistanceMap *const prevNearKeysDistances,
        const NearKeysDistanceMap *const prevPrevNearKeysDistances) {
    for (NearKeysDistanceMap::const_iterator it = prevNearKeysDistances->begin();
            it != prevNearKeysDistances->end(); ++it) {
        NearKeysDistanceMap::const_iterator itPP = prevPrevNearKeysDistances->find(it->first);
        NearKeysDistanceMap::const_iterator itC = currentNearKeysDistances->find(it->first);
        const bool isPrevPrevNear = (itPP == prevPrevNearKeysDistances->end()
                || itPP->second > it->second + ProximityInfoParams::MARGIN_FOR_PREV_LOCAL_MIN);
        const bool isCurrentNear = (itC == currentNearKeysDistances->end()
                || itC->second > it->second + ProximityInfoParams::MARGIN_FOR_PREV_LOCAL_MIN);
        if (isPrevPrevNear && isCurrentNear) {
            return true;
        }
    }
    return false;
}

// Calculating a point score that indicates usefulness of the point.
/* static */ float ProximityInfoStateUtils::getPointScore(const int mostCommonKeyWidth,
        const int x, const int y, const int time, const bool lastPoint, const float nearest,
        const float sumAngle, const NearKeysDistanceMap *const currentNearKeysDistances,
        const NearKeysDistanceMap *const prevNearKeysDistances,
        const NearKeysDistanceMap *const prevPrevNearKeysDistances,
        std::vector<int> *sampledInputXs, std::vector<int> *sampledInputYs) {
    const size_t size = sampledInputXs->size();
    // If there is only one point, add this point. Besides, if the previous point's distance map
    // is empty, we re-compute nearby keys distances from the current point.
    // Note that the current point is the first point in the incremental input that needs to
    // be re-computed.
    if (size <= 1 || prevNearKeysDistances->empty()) {
        return 0.0f;
    }

    const int baseSampleRate = mostCommonKeyWidth;
    const int distPrev = GeometryUtils::getDistanceInt(sampledInputXs->back(),
            sampledInputYs->back(), (*sampledInputXs)[size - 2],
            (*sampledInputYs)[size - 2]) * ProximityInfoParams::DISTANCE_BASE_SCALE;
    float score = 0.0f;

    // Location
    if (!isPrevLocalMin(currentNearKeysDistances, prevNearKeysDistances,
        prevPrevNearKeysDistances)) {
        score += ProximityInfoParams::NOT_LOCALMIN_DISTANCE_SCORE;
    } else if (nearest < ProximityInfoParams::NEAR_KEY_THRESHOLD_FOR_POINT_SCORE) {
        // Promote points nearby keys
        score += ProximityInfoParams::LOCALMIN_DISTANCE_AND_NEAR_TO_KEY_SCORE;
    }
    // Angle
    const float angle1 = GeometryUtils::getAngle(x, y, sampledInputXs->back(),
            sampledInputYs->back());
    const float angle2 = GeometryUtils::getAngle(sampledInputXs->back(), sampledInputYs->back(),
            (*sampledInputXs)[size - 2], (*sampledInputYs)[size - 2]);
    const float angleDiff = GeometryUtils::getAngleDiff(angle1, angle2);

    // Save corner
    if (distPrev > baseSampleRate * ProximityInfoParams::CORNER_CHECK_DISTANCE_THRESHOLD_SCALE
            && (sumAngle > ProximityInfoParams::CORNER_SUM_ANGLE_THRESHOLD
                    || angleDiff > ProximityInfoParams::CORNER_ANGLE_THRESHOLD_FOR_POINT_SCORE)) {
        score += ProximityInfoParams::CORNER_SCORE;
    }
    return score;
}

// Sampling touch point and pushing information to vectors.
// Returning if previous point is popped or not.
/* static */ bool ProximityInfoStateUtils::pushTouchPoint(const ProximityInfo *const proximityInfo,
        const int maxPointToKeyLength, const int inputIndex, const int nodeCodePoint, int x, int y,
        const int time, const bool isGeometric, const bool doSampling,
        const bool isLastPoint, const float sumAngle,
        NearKeysDistanceMap *const currentNearKeysDistances,
        const NearKeysDistanceMap *const prevNearKeysDistances,
        const NearKeysDistanceMap *const prevPrevNearKeysDistances,
        std::vector<int> *sampledInputXs, std::vector<int> *sampledInputYs,
        std::vector<int> *sampledInputTimes, std::vector<int> *sampledLengthCache,
        std::vector<int> *sampledInputIndice) {
    const int mostCommonKeyWidth = proximityInfo->getMostCommonKeyWidth();

    size_t size = sampledInputXs->size();
    bool popped = false;
    if (nodeCodePoint < 0 && doSampling) {
        const float nearest = updateNearKeysDistances(proximityInfo, maxPointToKeyLength, x, y,
                isGeometric, currentNearKeysDistances);
        const float score = getPointScore(mostCommonKeyWidth, x, y, time, isLastPoint, nearest,
                sumAngle, currentNearKeysDistances, prevNearKeysDistances,
                prevPrevNearKeysDistances, sampledInputXs, sampledInputYs);
        if (score < 0) {
            // Pop previous point because it would be useless.
            popInputData(sampledInputXs, sampledInputYs, sampledInputTimes, sampledLengthCache,
                    sampledInputIndice);
            size = sampledInputXs->size();
            popped = true;
        } else {
            popped = false;
        }
        // Check if the last point should be skipped.
        if (isLastPoint && size > 0) {
            if (GeometryUtils::getDistanceInt(x, y, sampledInputXs->back(), sampledInputYs->back())
                    * ProximityInfoParams::LAST_POINT_SKIP_DISTANCE_SCALE < mostCommonKeyWidth) {
                // This point is not used because it's too close to the previous point.
                if (DEBUG_GEO_FULL) {
                    AKLOGI("p0: size = %zd, x = %d, y = %d, lx = %d, ly = %d, dist = %d, "
                           "width = %d", size, x, y, sampledInputXs->back(),
                           sampledInputYs->back(), GeometryUtils::getDistanceInt(
                                   x, y, sampledInputXs->back(), sampledInputYs->back()),
                           mostCommonKeyWidth
                                   / ProximityInfoParams::LAST_POINT_SKIP_DISTANCE_SCALE);
                }
                return popped;
            }
        }
    }

    if (nodeCodePoint >= 0 && (x < 0 || y < 0)) {
        const int keyId = proximityInfo->getKeyIndexOf(nodeCodePoint);
        if (keyId >= 0) {
            x = proximityInfo->getKeyCenterXOfKeyIdG(keyId, NOT_AN_INDEX, isGeometric);
            y = proximityInfo->getKeyCenterYOfKeyIdG(keyId, NOT_AN_INDEX, isGeometric);
        }
    }

    // Pushing point information.
    if (size > 0) {
        sampledLengthCache->push_back(
                sampledLengthCache->back() + GeometryUtils::getDistanceInt(
                        x, y, sampledInputXs->back(), sampledInputYs->back()));
    } else {
        sampledLengthCache->push_back(0);
    }
    sampledInputXs->push_back(x);
    sampledInputYs->push_back(y);
    sampledInputTimes->push_back(time);
    sampledInputIndice->push_back(inputIndex);
    if (DEBUG_GEO_FULL) {
        AKLOGI("pushTouchPoint: x = %03d, y = %03d, time = %d, index = %d, popped ? %01d",
                x, y, time, inputIndex, popped);
    }
    return popped;
}

/* static */ float ProximityInfoStateUtils::calculateBeelineSpeedRate(const int mostCommonKeyWidth,
        const float averageSpeed, const int id, const int inputSize, const int *const xCoordinates,
        const int *const yCoordinates, const int *times, const int sampledInputSize,
        const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs,
        const std::vector<int> *const sampledInputIndices) {
    if (sampledInputSize <= 0 || averageSpeed < 0.001f) {
        if (DEBUG_SAMPLING_POINTS) {
            AKLOGI("--- invalid state: cancel. size = %d, ave = %f",
                    sampledInputSize, averageSpeed);
        }
        return 1.0f;
    }
    const int lookupRadius = mostCommonKeyWidth
            * ProximityInfoParams::LOOKUP_RADIUS_PERCENTILE / MAX_PERCENTILE;
    const int x0 = (*sampledInputXs)[id];
    const int y0 = (*sampledInputYs)[id];
    const int actualInputIndex = (*sampledInputIndices)[id];
    int tempBeelineDistance = 0;
    int start = actualInputIndex;
    // lookup forward
    while (start > 0 && tempBeelineDistance < lookupRadius) {
        --start;
        tempBeelineDistance = GeometryUtils::getDistanceInt(x0, y0, xCoordinates[start],
                yCoordinates[start]);
    }
    // Exclusive unless this is an edge point
    if (start > 0 && start < actualInputIndex) {
        ++start;
    }
    tempBeelineDistance = 0;
    int end = actualInputIndex;
    // lookup backward
    while (end < (inputSize - 1) && tempBeelineDistance < lookupRadius) {
        ++end;
        tempBeelineDistance = GeometryUtils::getDistanceInt(x0, y0, xCoordinates[end],
                yCoordinates[end]);
    }
    // Exclusive unless this is an edge point
    if (end > actualInputIndex && end < (inputSize - 1)) {
        --end;
    }

    if (start >= end) {
        if (DEBUG_DOUBLE_LETTER) {
            AKLOGI("--- double letter: start == end %d", start);
        }
        return 1.0f;
    }

    const int x2 = xCoordinates[start];
    const int y2 = yCoordinates[start];
    const int x3 = xCoordinates[end];
    const int y3 = yCoordinates[end];
    const int beelineDistance = GeometryUtils::getDistanceInt(x2, y2, x3, y3);
    int adjustedStartTime = times[start];
    if (start == 0 && actualInputIndex == 0 && inputSize > 1) {
        adjustedStartTime += ProximityInfoParams::FIRST_POINT_TIME_OFFSET_MILLIS;
    }
    int adjustedEndTime = times[end];
    if (end == (inputSize - 1) && inputSize > 1) {
        adjustedEndTime -= ProximityInfoParams::FIRST_POINT_TIME_OFFSET_MILLIS;
    }
    const int time = adjustedEndTime - adjustedStartTime;
    if (time <= 0) {
        return 1.0f;
    }

    if (time >= ProximityInfoParams::STRONG_DOUBLE_LETTER_TIME_MILLIS){
        return 0.0f;
    }
    if (DEBUG_DOUBLE_LETTER) {
        AKLOGI("--- (%d, %d) double letter: start = %d, end = %d, dist = %d, time = %d,"
                " speed = %f, ave = %f, val = %f, start time = %d, end time = %d",
                id, (*sampledInputIndices)[id], start, end, beelineDistance, time,
                (static_cast<float>(beelineDistance) / static_cast<float>(time)), averageSpeed,
                ((static_cast<float>(beelineDistance) / static_cast<float>(time))
                        / averageSpeed), adjustedStartTime, adjustedEndTime);
    }
    // Offset 1%
    // TODO: Detect double letter more smartly
    return 0.01f + static_cast<float>(beelineDistance) / static_cast<float>(time) / averageSpeed;
}

/* static */ float ProximityInfoStateUtils::getPointAngle(
        const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs, const int index) {
    if (!sampledInputXs || !sampledInputYs) {
        return 0.0f;
    }
    const int sampledInputSize = sampledInputXs->size();
    if (index <= 0 || index >= sampledInputSize - 1) {
        return 0.0f;
    }
    const float previousDirection = getDirection(sampledInputXs, sampledInputYs, index - 1, index);
    const float nextDirection = getDirection(sampledInputXs, sampledInputYs, index, index + 1);
    const float directionDiff = GeometryUtils::getAngleDiff(previousDirection, nextDirection);
    return directionDiff;
}

/* static */ float ProximityInfoStateUtils::getPointsAngle(
        const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs,
        const int index0, const int index1, const int index2) {
    if (!sampledInputXs || !sampledInputYs) {
        return 0.0f;
    }
    const int sampledInputSize = sampledInputXs->size();
    if (index0 < 0 || index0 > sampledInputSize - 1) {
        return 0.0f;
    }
    if (index1 < 0 || index1 > sampledInputSize - 1) {
        return 0.0f;
    }
    if (index2 < 0 || index2 > sampledInputSize - 1) {
        return 0.0f;
    }
    const float previousDirection = getDirection(sampledInputXs, sampledInputYs, index0, index1);
    const float nextDirection = getDirection(sampledInputXs, sampledInputYs, index1, index2);
    return GeometryUtils::getAngleDiff(previousDirection, nextDirection);
}

// This function basically converts from a length to an edit distance. Accordingly, it's obviously
// wrong to compare with mMaxPointToKeyLength.
/* static */ float ProximityInfoStateUtils::getPointToKeyByIdLength(const float maxPointToKeyLength,
        const std::vector<float> *const sampledNormalizedSquaredLengthCache, const int keyCount,
        const int inputIndex, const int keyId) {
    if (keyId != NOT_AN_INDEX) {
        const int index = inputIndex * keyCount + keyId;
        return std::min((*sampledNormalizedSquaredLengthCache)[index], maxPointToKeyLength);
    }
    // If the char is not a key on the keyboard then return the max length.
    return static_cast<float>(MAX_VALUE_FOR_WEIGHTING);
}

// Updates probabilities of aligning to some keys and skipping.
// Word suggestion should be based on this probabilities.
/* static */ void ProximityInfoStateUtils::updateAlignPointProbabilities(
        const float maxPointToKeyLength, const int mostCommonKeyWidth, const int keyCount,
        const int start, const int sampledInputSize, const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs,
        const std::vector<float> *const sampledSpeedRates,
        const std::vector<int> *const sampledLengthCache,
        const std::vector<float> *const sampledNormalizedSquaredLengthCache,
        const ProximityInfo *const proximityInfo,
        std::vector<std::unordered_map<int, float>> *charProbabilities) {
    charProbabilities->resize(sampledInputSize);
    // Calculates probabilities of using a point as a correlated point with the character
    // for each point.
    for (int i = start; i < sampledInputSize; ++i) {
        (*charProbabilities)[i].clear();
        // First, calculates skip probability. Starts from MAX_SKIP_PROBABILITY.
        // Note that all values that are multiplied to this probability should be in [0.0, 1.0];
        float skipProbability = ProximityInfoParams::MAX_SKIP_PROBABILITY;

        const float currentAngle = getPointAngle(sampledInputXs, sampledInputYs, i);
        const float speedRate = (*sampledSpeedRates)[i];

        float nearestKeyDistance = static_cast<float>(MAX_VALUE_FOR_WEIGHTING);
        for (int j = 0; j < keyCount; ++j) {
            const float distance = getPointToKeyByIdLength(
                    maxPointToKeyLength, sampledNormalizedSquaredLengthCache, keyCount, i, j);
            if (distance < nearestKeyDistance) {
                nearestKeyDistance = distance;
            }
        }

        if (i == 0) {
            skipProbability *= std::min(1.0f,
                    nearestKeyDistance * ProximityInfoParams::NEAREST_DISTANCE_WEIGHT
                            + ProximityInfoParams::NEAREST_DISTANCE_BIAS);
            // Promote the first point
            skipProbability *= ProximityInfoParams::SKIP_FIRST_POINT_PROBABILITY;
        } else if (i == sampledInputSize - 1) {
            skipProbability *= std::min(1.0f,
                    nearestKeyDistance * ProximityInfoParams::NEAREST_DISTANCE_WEIGHT_FOR_LAST
                            + ProximityInfoParams::NEAREST_DISTANCE_BIAS_FOR_LAST);
            // Promote the last point
            skipProbability *= ProximityInfoParams::SKIP_LAST_POINT_PROBABILITY;
        } else {
            // If the current speed is relatively slower than adjacent keys, we promote this point.
            if ((*sampledSpeedRates)[i - 1] - ProximityInfoParams::SPEED_MARGIN > speedRate
                    && speedRate
                            < (*sampledSpeedRates)[i + 1] - ProximityInfoParams::SPEED_MARGIN) {
                if (currentAngle < ProximityInfoParams::CORNER_ANGLE_THRESHOLD) {
                    skipProbability *= std::min(1.0f, speedRate
                            * ProximityInfoParams::SLOW_STRAIGHT_WEIGHT_FOR_SKIP_PROBABILITY);
                } else {
                    // If the angle is small enough, we promote this point more. (e.g. pit vs put)
                    skipProbability *= std::min(1.0f,
                            speedRate * ProximityInfoParams::SPEED_WEIGHT_FOR_SKIP_PROBABILITY
                                    + ProximityInfoParams::MIN_SPEED_RATE_FOR_SKIP_PROBABILITY);
                }
            }

            skipProbability *= std::min(1.0f,
                    speedRate * nearestKeyDistance * ProximityInfoParams::NEAREST_DISTANCE_WEIGHT
                            + ProximityInfoParams::NEAREST_DISTANCE_BIAS);

            // Adjusts skip probability by a rate depending on angle.
            // ANGLE_RATE of skipProbability is adjusted by current angle.
            skipProbability *= (M_PI_F - currentAngle) / M_PI_F * ProximityInfoParams::ANGLE_WEIGHT
                    + (1.0f - ProximityInfoParams::ANGLE_WEIGHT);
            if (currentAngle > ProximityInfoParams::DEEP_CORNER_ANGLE_THRESHOLD) {
                skipProbability *= ProximityInfoParams::SKIP_DEEP_CORNER_PROBABILITY;
            }
            // We assume the angle of this point is the angle for point[i], point[i - 2]
            // and point[i - 3]. The reason why we don't use the angle for point[i], point[i - 1]
            // and point[i - 2] is this angle can be more affected by the noise.
            const float prevAngle = getPointsAngle(sampledInputXs, sampledInputYs, i, i - 2, i - 3);
            if (i >= 3 && prevAngle < ProximityInfoParams::STRAIGHT_ANGLE_THRESHOLD
                    && currentAngle > ProximityInfoParams::CORNER_ANGLE_THRESHOLD) {
                skipProbability *= ProximityInfoParams::SKIP_CORNER_PROBABILITY;
            }
        }

        // probabilities must be in [0.0, ProximityInfoParams::MAX_SKIP_PROBABILITY];
        ASSERT(skipProbability >= 0.0f);
        ASSERT(skipProbability <= ProximityInfoParams::MAX_SKIP_PROBABILITY);
        (*charProbabilities)[i][NOT_AN_INDEX] = skipProbability;

        // Second, calculates key probabilities by dividing the rest probability
        // (1.0f - skipProbability).
        const float inputCharProbability = 1.0f - skipProbability;

        const float speedMultipliedByAngleRate = std::min(speedRate * currentAngle / M_PI_F
                * ProximityInfoParams::SPEEDxANGLE_WEIGHT_FOR_STANDARD_DEVIATION,
                        ProximityInfoParams::MAX_SPEEDxANGLE_RATE_FOR_STANDARD_DEVIATION);
        const float speedMultipliedByNearestKeyDistanceRate = std::min(
                speedRate * nearestKeyDistance
                        * ProximityInfoParams::SPEEDxNEAREST_WEIGHT_FOR_STANDARD_DEVIATION,
                                ProximityInfoParams::MAX_SPEEDxNEAREST_RATE_FOR_STANDARD_DEVIATION);
        const float sigma = (speedMultipliedByAngleRate + speedMultipliedByNearestKeyDistanceRate
                + ProximityInfoParams::MIN_STANDARD_DEVIATION) * mostCommonKeyWidth;
        float theta = 0.0f;
        // TODO: Use different metrics to compute sigmas.
        float sigmaX = sigma;
        float sigmaY = sigma;
        if (i == 0 && i != sampledInputSize - 1) {
            // First point
            theta = getDirection(sampledInputXs, sampledInputYs, i + 1, i);
            sigmaX *= ProximityInfoParams::STANDARD_DEVIATION_X_WEIGHT_FOR_FIRST;
            sigmaY *= ProximityInfoParams::STANDARD_DEVIATION_Y_WEIGHT_FOR_FIRST;
        } else {
            if (i == sampledInputSize - 1) {
                // Last point
                sigmaX *= ProximityInfoParams::STANDARD_DEVIATION_X_WEIGHT_FOR_LAST;
                sigmaY *= ProximityInfoParams::STANDARD_DEVIATION_Y_WEIGHT_FOR_LAST;
            } else {
                sigmaX *= ProximityInfoParams::STANDARD_DEVIATION_X_WEIGHT;
                sigmaY *= ProximityInfoParams::STANDARD_DEVIATION_Y_WEIGHT;
            }
            theta = getDirection(sampledInputXs, sampledInputYs, i, i - 1);
        }
        NormalDistribution2D distribution((*sampledInputXs)[i], sigmaX, (*sampledInputYs)[i],
                sigmaY, theta);
        // Summing up probability densities of all near keys.
        float sumOfProbabilityDensities = 0.0f;
        for (int j = 0; j < keyCount; ++j) {
            sumOfProbabilityDensities += distribution.getProbabilityDensity(
                    proximityInfo->getKeyCenterXOfKeyIdG(j,
                            NOT_A_COORDINATE /* referencePointX */, true /* isGeometric */),
                    proximityInfo->getKeyCenterYOfKeyIdG(j,
                            NOT_A_COORDINATE /* referencePointY */, true /* isGeometric */));
        }

        // Split the probability of an input point to keys that are close to the input point.
        for (int j = 0; j < keyCount; ++j) {
            const float probabilityDensity = distribution.getProbabilityDensity(
                    proximityInfo->getKeyCenterXOfKeyIdG(j,
                            NOT_A_COORDINATE /* referencePointX */, true /* isGeometric */),
                    proximityInfo->getKeyCenterYOfKeyIdG(j,
                            NOT_A_COORDINATE /* referencePointY */, true /* isGeometric */));
            const float probability = inputCharProbability * probabilityDensity
                    / sumOfProbabilityDensities;
            (*charProbabilities)[i][j] = probability;
        }
    }

    if (DEBUG_POINTS_PROBABILITY) {
        for (int i = 0; i < sampledInputSize; ++i) {
            std::stringstream sstream;
            sstream << i << ", ";
            sstream << "(" << (*sampledInputXs)[i] << ", " << (*sampledInputYs)[i] << "), ";
            sstream << "Speed: "<< (*sampledSpeedRates)[i] << ", ";
            sstream << "Angle: "<< getPointAngle(sampledInputXs, sampledInputYs, i) << ", \n";

            for (std::unordered_map<int, float>::iterator it = (*charProbabilities)[i].begin();
                    it != (*charProbabilities)[i].end(); ++it) {
                if (it->first == NOT_AN_INDEX) {
                    sstream << it->first
                            << "(skip):"
                            << it->second
                            << "\n";
                } else {
                    sstream << it->first
                            << "("
                            //<< static_cast<char>(mProximityInfo->getCodePointOf(it->first))
                            << "):"
                            << it->second
                            << "\n";
                }
            }
            AKLOGI("%s", sstream.str().c_str());
        }
    }

    // Decrease key probabilities of points which don't have the highest probability of that key
    // among nearby points. Probabilities of the first point and the last point are not suppressed.
    for (int i = std::max(start, 1); i < sampledInputSize; ++i) {
        for (int j = i + 1; j < sampledInputSize; ++j) {
            if (!suppressCharProbabilities(
                    mostCommonKeyWidth, sampledInputSize, sampledLengthCache, i, j,
                    charProbabilities)) {
                break;
            }
        }
        for (int j = i - 1; j >= std::max(start, 0); --j) {
            if (!suppressCharProbabilities(
                    mostCommonKeyWidth, sampledInputSize, sampledLengthCache, i, j,
                    charProbabilities)) {
                break;
            }
        }
    }

    // Converting from raw probabilities to log probabilities to calculate spatial distance.
    for (int i = start; i < sampledInputSize; ++i) {
        for (int j = 0; j < keyCount; ++j) {
            std::unordered_map<int, float>::iterator it = (*charProbabilities)[i].find(j);
            if (it == (*charProbabilities)[i].end()){
                continue;
            } else if(it->second < ProximityInfoParams::MIN_PROBABILITY) {
                // Erases from near keys vector because it has very low probability.
                (*charProbabilities)[i].erase(j);
            } else {
                it->second = -logf(it->second);
            }
        }
        (*charProbabilities)[i][NOT_AN_INDEX] = -logf((*charProbabilities)[i][NOT_AN_INDEX]);
    }
}

/* static */ void ProximityInfoStateUtils::updateSampledSearchKeySets(
        const ProximityInfo *const proximityInfo, const int sampledInputSize,
        const int lastSavedInputSize, const std::vector<int> *const sampledLengthCache,
        const std::vector<std::unordered_map<int, float>> *const charProbabilities,
        std::vector<NearKeycodesSet> *sampledSearchKeySets,
        std::vector<std::vector<int>> *sampledSearchKeyVectors) {
    sampledSearchKeySets->resize(sampledInputSize);
    sampledSearchKeyVectors->resize(sampledInputSize);
    const int readForwordLength = static_cast<int>(
            hypotf(proximityInfo->getKeyboardWidth(), proximityInfo->getKeyboardHeight())
                    * ProximityInfoParams::SEARCH_KEY_RADIUS_RATIO);
    for (int i = 0; i < sampledInputSize; ++i) {
        if (i >= lastSavedInputSize) {
            (*sampledSearchKeySets)[i].reset();
        }
        for (int j = std::max(i, lastSavedInputSize); j < sampledInputSize; ++j) {
            // TODO: Investigate if this is required. This may not fail.
            if ((*sampledLengthCache)[j] - (*sampledLengthCache)[i] >= readForwordLength) {
                break;
            }
            for(const auto& charProbability : charProbabilities->at(j)) {
                if (charProbability.first == NOT_AN_INDEX) {
                    continue;
                }
                (*sampledSearchKeySets)[i].set(charProbability.first);
            }
        }
    }
    const int keyCount = proximityInfo->getKeyCount();
    for (int i = 0; i < sampledInputSize; ++i) {
        std::vector<int> *searchKeyVector = &(*sampledSearchKeyVectors)[i];
        searchKeyVector->clear();
        for (int j = 0; j < keyCount; ++j) {
            if ((*sampledSearchKeySets)[i].test(j)) {
                const int keyCodePoint = proximityInfo->getCodePointOf(j);
                if (std::find(searchKeyVector->begin(), searchKeyVector->end(), keyCodePoint)
                        == searchKeyVector->end()) {
                    searchKeyVector->push_back(keyCodePoint);
                }
            }
        }
    }
}

// Decreases char probabilities of index0 by checking probabilities of a near point (index1) and
// increases char probabilities of index1 by checking probabilities of index0.
/* static */ bool ProximityInfoStateUtils::suppressCharProbabilities(const int mostCommonKeyWidth,
        const int sampledInputSize, const std::vector<int> *const lengthCache,
        const int index0, const int index1,
        std::vector<std::unordered_map<int, float>> *charProbabilities) {
    ASSERT(0 <= index0 && index0 < sampledInputSize);
    ASSERT(0 <= index1 && index1 < sampledInputSize);
    const float keyWidthFloat = static_cast<float>(mostCommonKeyWidth);
    const float diff = fabsf(static_cast<float>((*lengthCache)[index0] - (*lengthCache)[index1]));
    if (diff > keyWidthFloat * ProximityInfoParams::SUPPRESSION_LENGTH_WEIGHT) {
        return false;
    }
    const float suppressionRate = ProximityInfoParams::MIN_SUPPRESSION_RATE
            + diff / keyWidthFloat / ProximityInfoParams::SUPPRESSION_LENGTH_WEIGHT
                    * ProximityInfoParams::SUPPRESSION_WEIGHT;
    for (std::unordered_map<int, float>::iterator it = (*charProbabilities)[index0].begin();
            it != (*charProbabilities)[index0].end(); ++it) {
        std::unordered_map<int, float>::iterator it2 = (*charProbabilities)[index1].find(it->first);
        if (it2 != (*charProbabilities)[index1].end() && it->second < it2->second) {
            const float newProbability = it->second * suppressionRate;
            const float suppression = it->second - newProbability;
            it->second = newProbability;
            // mCharProbabilities[index0][NOT_AN_INDEX] is the probability of skipping this point.
            (*charProbabilities)[index0][NOT_AN_INDEX] += suppression;

            // Add the probability of the same key nearby index1
            const float probabilityGain = std::min(suppression
                    * ProximityInfoParams::SUPPRESSION_WEIGHT_FOR_PROBABILITY_GAIN,
                    (*charProbabilities)[index1][NOT_AN_INDEX]
                            * ProximityInfoParams::SKIP_PROBABALITY_WEIGHT_FOR_PROBABILITY_GAIN);
            it2->second += probabilityGain;
            (*charProbabilities)[index1][NOT_AN_INDEX] -= probabilityGain;
        }
    }
    return true;
}

/* static */ bool ProximityInfoStateUtils::checkAndReturnIsContinuousSuggestionPossible(
        const int inputSize, const int *const xCoordinates, const int *const yCoordinates,
        const int *const times, const int sampledInputSize,
        const std::vector<int> *const sampledInputXs, const std::vector<int> *const sampledInputYs,
        const std::vector<int> *const sampledTimes,
        const std::vector<int> *const sampledInputIndices) {
    if (inputSize < sampledInputSize) {
        return false;
    }
    for (int i = 0; i < sampledInputSize; ++i) {
        const int index = (*sampledInputIndices)[i];
        if (index >= inputSize) {
            return false;
        }
        if (xCoordinates[index] != (*sampledInputXs)[i]
                || yCoordinates[index] != (*sampledInputYs)[i]) {
            return false;
        }
        if (!times) {
            continue;
        }
        if (times[index] != (*sampledTimes)[i]) {
            return false;
        }
    }
    return true;
}

// Get a word that is detected by tracing the most probable string into codePointBuf and
// returns probability of generating the word.
/* static */ float ProximityInfoStateUtils::getMostProbableString(
        const ProximityInfo *const proximityInfo, const int sampledInputSize,
        const std::vector<std::unordered_map<int, float>> *const charProbabilities,
        int *const codePointBuf) {
    ASSERT(sampledInputSize >= 0);
    memset(codePointBuf, 0, sizeof(codePointBuf[0]) * MAX_WORD_LENGTH);
    int index = 0;
    float sumLogProbability = 0.0f;
    // TODO: Current implementation is greedy algorithm. DP would be efficient for many cases.
    for (int i = 0; i < sampledInputSize && index < MAX_WORD_LENGTH - 1; ++i) {
        float minLogProbability = static_cast<float>(MAX_VALUE_FOR_WEIGHTING);
        int character = NOT_AN_INDEX;
        for (std::unordered_map<int, float>::const_iterator it = (*charProbabilities)[i].begin();
                it != (*charProbabilities)[i].end(); ++it) {
            const float logProbability = (it->first != NOT_AN_INDEX)
                    ? it->second + ProximityInfoParams::DEMOTION_LOG_PROBABILITY : it->second;
            if (logProbability < minLogProbability) {
                minLogProbability = logProbability;
                character = it->first;
            }
        }
        if (character != NOT_AN_INDEX) {
            const int codePoint = proximityInfo->getCodePointOf(character);
            if (codePoint == NOT_A_CODE_POINT) {
                AKLOGE("Key index(%d) is not found. Cannot construct most probable string",
                        character);
                ASSERT(false);
                // Make the length zero, which means most probable string won't be used.
                index = 0;
                break;
            }
            codePointBuf[index] = codePoint;
            index++;
        }
        sumLogProbability += minLogProbability;
    }
    codePointBuf[index] = '\0';
    return sumLogProbability;
}

/* static */ void ProximityInfoStateUtils::dump(const bool isGeometric, const int inputSize,
        const int *const inputXCoordinates, const int *const inputYCoordinates,
        const int sampledInputSize, const std::vector<int> *const sampledInputXs,
        const std::vector<int> *const sampledInputYs,
        const std::vector<int> *const sampledTimes,
        const std::vector<float> *const sampledSpeedRates,
        const std::vector<int> *const sampledBeelineSpeedPercentiles) {
    if (DEBUG_GEO_FULL) {
        for (int i = 0; i < sampledInputSize; ++i) {
            AKLOGI("Sampled(%d): x = %d, y = %d, time = %d", i, (*sampledInputXs)[i],
                    (*sampledInputYs)[i], sampledTimes ? (*sampledTimes)[i] : -1);
        }
    }

    std::stringstream originalX, originalY, sampledX, sampledY;
    for (int i = 0; i < inputSize; ++i) {
        originalX << inputXCoordinates[i];
        originalY << inputYCoordinates[i];
        if (i != inputSize - 1) {
            originalX << ";";
            originalY << ";";
        }
    }
    AKLOGI("===== sampled points =====");
    for (int i = 0; i < sampledInputSize; ++i) {
        if (isGeometric) {
            AKLOGI("%d: x = %d, y = %d, time = %d, relative speed = %.4f, beeline speed = %d",
                    i, (*sampledInputXs)[i], (*sampledInputYs)[i], (*sampledTimes)[i],
                    (*sampledSpeedRates)[i], (*sampledBeelineSpeedPercentiles)[i]);
        }
        sampledX << (*sampledInputXs)[i];
        sampledY << (*sampledInputYs)[i];
        if (i != sampledInputSize - 1) {
            sampledX << ";";
            sampledY << ";";
        }
    }
    AKLOGI("original points:\n%s, %s,\nsampled points:\n%s, %s,\n",
            originalX.str().c_str(), originalY.str().c_str(), sampledX.str().c_str(),
            sampledY.str().c_str());
}
} // namespace latinime
