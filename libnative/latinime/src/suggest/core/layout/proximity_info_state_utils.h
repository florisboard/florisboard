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

#ifndef LATINIME_PROXIMITY_INFO_STATE_UTILS_H
#define LATINIME_PROXIMITY_INFO_STATE_UTILS_H

#include <bitset>
#include <unordered_map>
#include <vector>

#include "defines.h"

namespace latinime {
class ProximityInfo;
class ProximityInfoParams;

class ProximityInfoStateUtils {
 public:
    typedef std::unordered_map<int, float> NearKeysDistanceMap;
    typedef std::bitset<MAX_KEY_COUNT_IN_A_KEYBOARD> NearKeycodesSet;

    static int trimLastTwoTouchPoints(std::vector<int> *sampledInputXs,
            std::vector<int> *sampledInputYs, std::vector<int> *sampledInputTimes,
            std::vector<int> *sampledLengthCache, std::vector<int> *sampledInputIndice);
    static int updateTouchPoints(const ProximityInfo *const proximityInfo,
            const int maxPointToKeyLength, const int *const inputProximities,
            const int *const inputXCoordinates, const int *const inputYCoordinates,
            const int *const times, const int *const pointerIds, const int inputSize,
            const bool isGeometric, const int pointerId, const int pushTouchPointStartIndex,
            std::vector<int> *sampledInputXs, std::vector<int> *sampledInputYs,
            std::vector<int> *sampledInputTimes, std::vector<int> *sampledLengthCache,
            std::vector<int> *sampledInputIndice);
    static const int *getProximityCodePointsAt(const int *const inputProximities, const int index);
    static int getPrimaryCodePointAt(const int *const inputProximities, const int index);
    static void popInputData(std::vector<int> *sampledInputXs, std::vector<int> *sampledInputYs,
            std::vector<int> *sampledInputTimes, std::vector<int> *sampledLengthCache,
            std::vector<int> *sampledInputIndice);
    static float refreshSpeedRates(const int inputSize, const int *const xCoordinates,
            const int *const yCoordinates, const int *const times, const int lastSavedInputSize,
            const int sampledInputSize, const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs,
            const std::vector<int> *const sampledInputTimes,
            const std::vector<int> *const sampledLengthCache,
            const std::vector<int> *const sampledInputIndice,
            std::vector<float> *sampledSpeedRates, std::vector<float> *sampledDirections);
    static void refreshBeelineSpeedRates(const int mostCommonKeyWidth, const float averageSpeed,
            const int inputSize, const int *const xCoordinates, const int *const yCoordinates,
            const int *times, const int sampledInputSize,
            const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs, const std::vector<int> *const inputIndice,
            std::vector<int> *beelineSpeedPercentiles);
    static float getDirection(const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs, const int index0, const int index1);
    static void updateAlignPointProbabilities(const float maxPointToKeyLength,
            const int mostCommonKeyWidth, const int keyCount, const int start,
            const int sampledInputSize, const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs,
            const std::vector<float> *const sampledSpeedRates,
            const std::vector<int> *const sampledLengthCache,
            const std::vector<float> *const sampledNormalizedSquaredLengthCache,
            const ProximityInfo *const proximityInfo,
            std::vector<std::unordered_map<int, float>> *charProbabilities);
    static void updateSampledSearchKeySets(const ProximityInfo *const proximityInfo,
            const int sampledInputSize, const int lastSavedInputSize,
            const std::vector<int> *const sampledLengthCache,
            const std::vector<std::unordered_map<int, float>> *const charProbabilities,
            std::vector<NearKeycodesSet> *sampledSearchKeySets,
            std::vector<std::vector<int>> *sampledSearchKeyVectors);
    static float getPointToKeyByIdLength(const float maxPointToKeyLength,
            const std::vector<float> *const sampledNormalizedSquaredLengthCache, const int keyCount,
            const int inputIndex, const int keyId);
    static void initGeometricDistanceInfos(const ProximityInfo *const proximityInfo,
            const int sampledInputSize, const int lastSavedInputSize, const bool isGeometric,
            const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs,
            std::vector<float> *sampledNormalizedSquaredLengthCache);
    static void initPrimaryInputWord(const int inputSize, const int *const inputProximities,
            int *primaryInputWord);
    static void dump(const bool isGeometric, const int inputSize,
            const int *const inputXCoordinates, const int *const inputYCoordinates,
            const int sampledInputSize, const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs,
            const std::vector<int> *const sampledTimes,
            const std::vector<float> *const sampledSpeedRates,
            const std::vector<int> *const sampledBeelineSpeedPercentiles);
    static bool checkAndReturnIsContinuousSuggestionPossible(const int inputSize,
            const int *const xCoordinates, const int *const yCoordinates, const int *const times,
            const int sampledInputSize, const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs,
            const std::vector<int> *const sampledTimes,
            const std::vector<int> *const sampledInputIndices);
    // TODO: Move to most_probable_string_utils.h
    static float getMostProbableString(const ProximityInfo *const proximityInfo,
            const int sampledInputSize,
            const std::vector<std::unordered_map<int, float>> *const charProbabilities,
            int *const codePointBuf);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(ProximityInfoStateUtils);

    static float updateNearKeysDistances(const ProximityInfo *const proximityInfo,
            const float maxPointToKeyLength, const int x, const int y,
            const bool isGeometric,
            NearKeysDistanceMap *const currentNearKeysDistances);
    static bool isPrevLocalMin(const NearKeysDistanceMap *const currentNearKeysDistances,
            const NearKeysDistanceMap *const prevNearKeysDistances,
            const NearKeysDistanceMap *const prevPrevNearKeysDistances);
    static float getPointScore(const int mostCommonKeyWidth, const int x, const int y,
            const int time, const bool lastPoint, const float nearest, const float sumAngle,
            const NearKeysDistanceMap *const currentNearKeysDistances,
            const NearKeysDistanceMap *const prevNearKeysDistances,
            const NearKeysDistanceMap *const prevPrevNearKeysDistances,
            std::vector<int> *sampledInputXs, std::vector<int> *sampledInputYs);
    static bool pushTouchPoint(const ProximityInfo *const proximityInfo,
            const int maxPointToKeyLength, const int inputIndex, const int nodeCodePoint, int x,
            int y, const int time, const bool isGeometric,
            const bool doSampling, const bool isLastPoint,
            const float sumAngle, NearKeysDistanceMap *const currentNearKeysDistances,
            const NearKeysDistanceMap *const prevNearKeysDistances,
            const NearKeysDistanceMap *const prevPrevNearKeysDistances,
            std::vector<int> *sampledInputXs, std::vector<int> *sampledInputYs,
            std::vector<int> *sampledInputTimes, std::vector<int> *sampledLengthCache,
            std::vector<int> *sampledInputIndice);
    static float calculateBeelineSpeedRate(const int mostCommonKeyWidth, const float averageSpeed,
            const int id, const int inputSize, const int *const xCoordinates,
            const int *const yCoordinates, const int *times, const int sampledInputSize,
            const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs,
            const std::vector<int> *const inputIndice);
    static float getPointAngle(const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs, const int index);
    static float getPointsAngle(const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs, const int index0, const int index1,
            const int index2);
    static bool suppressCharProbabilities(const int mostCommonKeyWidth,
            const int sampledInputSize, const std::vector<int> *const lengthCache, const int index0,
            const int index1, std::vector<std::unordered_map<int, float>> *charProbabilities);
    static float calculateSquaredDistanceFromSweetSpotCenter(
            const ProximityInfo *const proximityInfo, const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs, const int keyIndex,
            const int inputIndex);
     static float calculateNormalizedSquaredDistance(const ProximityInfo *const proximityInfo,
            const std::vector<int> *const sampledInputXs,
            const std::vector<int> *const sampledInputYs, const int keyIndex, const int inputIndex);
};
} // namespace latinime
#endif // LATINIME_PROXIMITY_INFO_STATE_UTILS_H
