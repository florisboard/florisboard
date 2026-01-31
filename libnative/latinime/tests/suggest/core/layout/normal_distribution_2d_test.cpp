/*
 * Copyright (C) 2014 The Android Open Source Project
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

#include "suggest/core/layout/normal_distribution_2d.h"

#include <gtest/gtest.h>

#include <vector>

namespace latinime {
namespace {

static const float ORIGIN_X = 0.0f;
static const float ORIGIN_Y = 0.0f;
static const float LARGE_STANDARD_DEVIATION = 100.0f;
static const float SMALL_STANDARD_DEVIATION = 10.0f;
static const float ZERO_RADIAN = 0.0f;

TEST(NormalDistribution2DTest, ProbabilityDensity) {
    const NormalDistribution2D distribution(ORIGIN_X, LARGE_STANDARD_DEVIATION, ORIGIN_Y,
            SMALL_STANDARD_DEVIATION, ZERO_RADIAN);

    static const float SMALL_COORDINATE = 10.0f;
    static const float LARGE_COORDINATE = 20.0f;
    // The probability density of the point near the distribution center is larger than the
    // probability density of the point that is far from distribution center.
    EXPECT_GE(distribution.getProbabilityDensity(SMALL_COORDINATE, SMALL_COORDINATE),
            distribution.getProbabilityDensity(LARGE_COORDINATE, LARGE_COORDINATE));
    // The probability density of the point shifted toward the direction that has larger standard
    // deviation is larger than the probability density of the point shifted towards another
    // direction.
    EXPECT_GE(distribution.getProbabilityDensity(LARGE_COORDINATE, SMALL_COORDINATE),
            distribution.getProbabilityDensity(SMALL_COORDINATE, LARGE_COORDINATE));
}

TEST(NormalDistribution2DTest, Rotate) {
    static const float COORDINATES[] = {0.0f, 10.0f, 100.0f, -20.0f};
    static const float EPSILON = 0.01f;
    const NormalDistribution2D distribution(ORIGIN_X, LARGE_STANDARD_DEVIATION, ORIGIN_Y,
            SMALL_STANDARD_DEVIATION, ZERO_RADIAN);
    const NormalDistribution2D rotatedDistribution(ORIGIN_X, LARGE_STANDARD_DEVIATION, ORIGIN_Y,
            SMALL_STANDARD_DEVIATION, M_PI_4);
    for (const float x : COORDINATES) {
        for (const float y : COORDINATES) {
            // The probability density of the rotated distribution at the point and the probability
            // density of the original distribution at the rotated point are the same.
            const float probabilityDensity0 = distribution.getProbabilityDensity(x, y);
            const float probabilityDensity1 = rotatedDistribution.getProbabilityDensity(-y, x);
            EXPECT_NEAR(probabilityDensity0, probabilityDensity1, EPSILON);
        }
    }
}

}  // namespace
}  // namespace latinime
