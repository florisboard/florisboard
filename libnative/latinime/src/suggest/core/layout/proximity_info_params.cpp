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

#include "defines.h"
#include "suggest/core/layout/proximity_info_params.h"

namespace latinime {
const float ProximityInfoParams::NOT_A_DISTANCE_FLOAT = -1.0f;
const int ProximityInfoParams::MIN_DOUBLE_LETTER_BEELINE_SPEED_PERCENTILE = 5;
const float ProximityInfoParams::VERTICAL_SWEET_SPOT_SCALE = 1.0f;
const float ProximityInfoParams::VERTICAL_SWEET_SPOT_SCALE_G = 0.5f;

/* Per method constants */
// Used by ProximityInfoStateUtils::updateNearKeysDistances()
const float ProximityInfoParams::NEAR_KEY_THRESHOLD_FOR_DISTANCE = 2.0f;

// Used by ProximityInfoStateUtils::isPrevLocalMin()
const float ProximityInfoParams::MARGIN_FOR_PREV_LOCAL_MIN = 0.01f;

// Used by ProximityInfoStateUtils::getPointScore()
const int ProximityInfoParams::DISTANCE_BASE_SCALE = 100;
const float ProximityInfoParams::NEAR_KEY_THRESHOLD_FOR_POINT_SCORE = 0.6f;
const int ProximityInfoParams::CORNER_CHECK_DISTANCE_THRESHOLD_SCALE = 25;
const float ProximityInfoParams::NOT_LOCALMIN_DISTANCE_SCORE = -1.0f;
const float ProximityInfoParams::LOCALMIN_DISTANCE_AND_NEAR_TO_KEY_SCORE = 1.0f;
const float ProximityInfoParams::CORNER_ANGLE_THRESHOLD_FOR_POINT_SCORE = M_PI_F * 2.0f / 3.0f;
const float ProximityInfoParams::CORNER_SUM_ANGLE_THRESHOLD = M_PI_F / 4.0f;
const float ProximityInfoParams::CORNER_SCORE = 1.0f;

// Used by ProximityInfoStateUtils::refreshSpeedRates()
const int ProximityInfoParams::NUM_POINTS_FOR_SPEED_CALCULATION = 2;

// Used by ProximityInfoStateUtils::pushTouchPoint()
const int ProximityInfoParams::LAST_POINT_SKIP_DISTANCE_SCALE = 4;

// Used by ProximityInfoStateUtils::updateAlignPointProbabilities()
const float ProximityInfoParams::MIN_PROBABILITY = 0.000005f;
const float ProximityInfoParams::MAX_SKIP_PROBABILITY = 0.95f;
const float ProximityInfoParams::SKIP_FIRST_POINT_PROBABILITY = 0.01f;
const float ProximityInfoParams::SKIP_LAST_POINT_PROBABILITY = 0.1f;
const float ProximityInfoParams::MIN_SPEED_RATE_FOR_SKIP_PROBABILITY = 0.15f;
const float ProximityInfoParams::SPEED_WEIGHT_FOR_SKIP_PROBABILITY = 0.9f;
const float ProximityInfoParams::SLOW_STRAIGHT_WEIGHT_FOR_SKIP_PROBABILITY = 0.6f;
const float ProximityInfoParams::NEAREST_DISTANCE_WEIGHT = 0.5f;
const float ProximityInfoParams::NEAREST_DISTANCE_BIAS = 0.5f;
const float ProximityInfoParams::NEAREST_DISTANCE_WEIGHT_FOR_LAST = 0.6f;
const float ProximityInfoParams::NEAREST_DISTANCE_BIAS_FOR_LAST = 0.4f;
const float ProximityInfoParams::ANGLE_WEIGHT = 0.90f;
const float ProximityInfoParams::DEEP_CORNER_ANGLE_THRESHOLD = M_PI_F * 60.0f / 180.0f;
const float ProximityInfoParams::SKIP_DEEP_CORNER_PROBABILITY = 0.1f;
const float ProximityInfoParams::CORNER_ANGLE_THRESHOLD = M_PI_F * 30.0f / 180.0f;
const float ProximityInfoParams::STRAIGHT_ANGLE_THRESHOLD = M_PI_F * 15.0f / 180.0f;
const float ProximityInfoParams::SKIP_CORNER_PROBABILITY = 0.4f;
const float ProximityInfoParams::SPEED_MARGIN = 0.1f;
const float ProximityInfoParams::CENTER_VALUE_OF_NORMALIZED_DISTRIBUTION = 0.0f;
// TODO: The variance is critical for accuracy; thus, adjusting these parameters by machine
// learning or something would be efficient.
const float ProximityInfoParams::SPEEDxANGLE_WEIGHT_FOR_STANDARD_DEVIATION = 0.3f;
const float ProximityInfoParams::MAX_SPEEDxANGLE_RATE_FOR_STANDARD_DEVIATION = 0.25f;
const float ProximityInfoParams::SPEEDxNEAREST_WEIGHT_FOR_STANDARD_DEVIATION = 0.5f;
const float ProximityInfoParams::MAX_SPEEDxNEAREST_RATE_FOR_STANDARD_DEVIATION = 0.15f;
const float ProximityInfoParams::MIN_STANDARD_DEVIATION = 0.37f;
const float ProximityInfoParams::STANDARD_DEVIATION_X_WEIGHT_FOR_FIRST = 1.25f;
const float ProximityInfoParams::STANDARD_DEVIATION_Y_WEIGHT_FOR_FIRST = 0.85f;
const float ProximityInfoParams::STANDARD_DEVIATION_X_WEIGHT_FOR_LAST = 1.4f;
const float ProximityInfoParams::STANDARD_DEVIATION_Y_WEIGHT_FOR_LAST = 0.95f;
const float ProximityInfoParams::STANDARD_DEVIATION_X_WEIGHT = 1.1f;
const float ProximityInfoParams::STANDARD_DEVIATION_Y_WEIGHT = 0.95f;

// Used by ProximityInfoStateUtils::suppressCharProbabilities()
const float ProximityInfoParams::SUPPRESSION_LENGTH_WEIGHT = 1.5f;
const float ProximityInfoParams::MIN_SUPPRESSION_RATE = 0.1f;
const float ProximityInfoParams::SUPPRESSION_WEIGHT = 0.5f;
const float ProximityInfoParams::SUPPRESSION_WEIGHT_FOR_PROBABILITY_GAIN = 0.1f;
const float ProximityInfoParams::SKIP_PROBABALITY_WEIGHT_FOR_PROBABILITY_GAIN = 0.3f;

// Used by ProximityInfoStateUtils::getMostProbableString()
const float ProximityInfoParams::DEMOTION_LOG_PROBABILITY = 0.3f;

// Used by ProximityInfoStateUtils::updateSampledSearchKeySets()
// TODO: Investigate if this is required
const float ProximityInfoParams::SEARCH_KEY_RADIUS_RATIO = 0.95f;

// Used by ProximityInfoStateUtils::calculateBeelineSpeedRate()
const int ProximityInfoParams::LOOKUP_RADIUS_PERCENTILE = 50;
const int ProximityInfoParams::FIRST_POINT_TIME_OFFSET_MILLIS = 150;
const int ProximityInfoParams::STRONG_DOUBLE_LETTER_TIME_MILLIS = 600;

} // namespace latinime
