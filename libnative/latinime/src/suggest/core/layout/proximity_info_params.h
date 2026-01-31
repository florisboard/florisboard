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

#ifndef LATINIME_PROXIMITY_INFO_PARAMS_H
#define LATINIME_PROXIMITY_INFO_PARAMS_H

#include "defines.h"

namespace latinime {

class ProximityInfoParams {
 public:
    static const float NOT_A_DISTANCE_FLOAT;
    static const int MIN_DOUBLE_LETTER_BEELINE_SPEED_PERCENTILE;
    static const float VERTICAL_SWEET_SPOT_SCALE;
    static const float VERTICAL_SWEET_SPOT_SCALE_G;

    // Used by ProximityInfoStateUtils::updateNearKeysDistances()
    static const float NEAR_KEY_THRESHOLD_FOR_DISTANCE;

    // Used by ProximityInfoStateUtils::isPrevLocalMin()
    static const float MARGIN_FOR_PREV_LOCAL_MIN;

    // Used by ProximityInfoStateUtils::getPointScore()
    static const int DISTANCE_BASE_SCALE;
    static const float NEAR_KEY_THRESHOLD_FOR_POINT_SCORE;
    static const int CORNER_CHECK_DISTANCE_THRESHOLD_SCALE;
    static const float NOT_LOCALMIN_DISTANCE_SCORE;
    static const float LOCALMIN_DISTANCE_AND_NEAR_TO_KEY_SCORE;
    static const float CORNER_ANGLE_THRESHOLD_FOR_POINT_SCORE;
    static const float CORNER_SUM_ANGLE_THRESHOLD;
    static const float CORNER_SCORE;

    // Used by ProximityInfoStateUtils::refreshSpeedRates()
    static const int NUM_POINTS_FOR_SPEED_CALCULATION;

    // Used by ProximityInfoStateUtils::pushTouchPoint()
    static const int LAST_POINT_SKIP_DISTANCE_SCALE;

    // Used by ProximityInfoStateUtils::updateAlignPointProbabilities()
    static const float MIN_PROBABILITY;
    static const float MAX_SKIP_PROBABILITY;
    static const float SKIP_FIRST_POINT_PROBABILITY;
    static const float SKIP_LAST_POINT_PROBABILITY;
    static const float MIN_SPEED_RATE_FOR_SKIP_PROBABILITY;
    static const float SPEED_WEIGHT_FOR_SKIP_PROBABILITY;
    static const float SLOW_STRAIGHT_WEIGHT_FOR_SKIP_PROBABILITY;
    static const float NEAREST_DISTANCE_WEIGHT;
    static const float NEAREST_DISTANCE_BIAS;
    static const float NEAREST_DISTANCE_WEIGHT_FOR_LAST;
    static const float NEAREST_DISTANCE_BIAS_FOR_LAST;
    static const float ANGLE_WEIGHT;
    static const float DEEP_CORNER_ANGLE_THRESHOLD;
    static const float SKIP_DEEP_CORNER_PROBABILITY;
    static const float CORNER_ANGLE_THRESHOLD;
    static const float STRAIGHT_ANGLE_THRESHOLD;
    static const float SKIP_CORNER_PROBABILITY;
    static const float SPEED_MARGIN;
    static const float CENTER_VALUE_OF_NORMALIZED_DISTRIBUTION;
    static const float SPEEDxANGLE_WEIGHT_FOR_STANDARD_DEVIATION;
    static const float MAX_SPEEDxANGLE_RATE_FOR_STANDARD_DEVIATION;
    static const float SPEEDxNEAREST_WEIGHT_FOR_STANDARD_DEVIATION;
    static const float MAX_SPEEDxNEAREST_RATE_FOR_STANDARD_DEVIATION;
    static const float MIN_STANDARD_DEVIATION;
    // X means gesture's direction. Y means gesture's orthogonal direction.
    static const float STANDARD_DEVIATION_X_WEIGHT_FOR_FIRST;
    static const float STANDARD_DEVIATION_Y_WEIGHT_FOR_FIRST;
    static const float STANDARD_DEVIATION_X_WEIGHT_FOR_LAST;
    static const float STANDARD_DEVIATION_Y_WEIGHT_FOR_LAST;
    static const float STANDARD_DEVIATION_X_WEIGHT;
    static const float STANDARD_DEVIATION_Y_WEIGHT;

    // Used by ProximityInfoStateUtils::suppressCharProbabilities()
    static const float SUPPRESSION_LENGTH_WEIGHT;
    static const float MIN_SUPPRESSION_RATE;
    static const float SUPPRESSION_WEIGHT;
    static const float SUPPRESSION_WEIGHT_FOR_PROBABILITY_GAIN;
    static const float SKIP_PROBABALITY_WEIGHT_FOR_PROBABILITY_GAIN;

    // Used by ProximityInfoStateUtils::getMostProbableString()
    static const float DEMOTION_LOG_PROBABILITY;

    // Used by ProximityInfoStateUtils::updateSampledSearchKeySets()
    static const float SEARCH_KEY_RADIUS_RATIO;

    // Used by ProximityInfoStateUtils::calculateBeelineSpeedRate()
    static const int LOOKUP_RADIUS_PERCENTILE;
    static const int FIRST_POINT_TIME_OFFSET_MILLIS;
    static const int STRONG_DOUBLE_LETTER_TIME_MILLIS;

    // Used by ProximityInfoStateUtils::calculateNormalizedSquaredDistance()
    static const int NORMALIZED_SQUARED_DISTANCE_SCALING_FACTOR;

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(ProximityInfoParams);
};
} // namespace latinime
#endif // LATINIME_PROXIMITY_INFO_PARAMS_H
