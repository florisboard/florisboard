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

#include "suggest/core/layout/geometry_utils.h"

#include <gtest/gtest.h>

namespace latinime {
namespace {

::testing::AssertionResult ExpectAngleDiffEq(const char* expectedExpression,
      const char* actualExpression, float expected, float actual) {
    if (actual < 0.0f || M_PI_F < actual) {
        return ::testing::AssertionFailure()
              << "Must be in the range of [0.0f, M_PI_F]."
              << " expected: " << expected
              << " actual: " << actual;
    }
    return ::testing::internal::CmpHelperFloatingPointEQ<float>(
            expectedExpression, actualExpression, expected, actual);
}

#define EXPECT_ANGLE_DIFF_EQ(expected, actual) \
        EXPECT_PRED_FORMAT2(ExpectAngleDiffEq, expected, actual);

TEST(GeometryUtilsTest, testSquareFloat) {
    const float test_data[] = { 0.0f, 1.0f, 123.456f, -1.0f, -9876.54321f };
    for (const float value : test_data) {
        EXPECT_FLOAT_EQ(value * value, GeometryUtils::SQUARE_FLOAT(value));
    }
}

TEST(GeometryUtilsTest, testGetAngle) {
    EXPECT_FLOAT_EQ(0.0f, GeometryUtils::getAngle(0, 0, 0, 0));
    EXPECT_FLOAT_EQ(0.0f, GeometryUtils::getAngle(100, -10, 100, -10));

    EXPECT_FLOAT_EQ(M_PI_F / 4.0f, GeometryUtils::getAngle(1, 1, 0, 0));
    EXPECT_FLOAT_EQ(M_PI_F, GeometryUtils::getAngle(-1, 0, 0, 0));

    EXPECT_FLOAT_EQ(GeometryUtils::getAngle(0, 0, -1, 0), GeometryUtils::getAngle(1, 0, 0, 0));
    EXPECT_FLOAT_EQ(GeometryUtils::getAngle(1, 2, 3, 4),
            GeometryUtils::getAngle(100, 200, 300, 400));
}

TEST(GeometryUtilsTest, testGetAngleDiff) {
    EXPECT_ANGLE_DIFF_EQ(0.0f, GeometryUtils::getAngleDiff(0.0f, 0.0f));
    EXPECT_ANGLE_DIFF_EQ(0.0f, GeometryUtils::getAngleDiff(10000.0f, 10000.0f));
    EXPECT_ANGLE_DIFF_EQ(ROUND_FLOAT_10000(M_PI_F),
            GeometryUtils::getAngleDiff(0.0f, M_PI_F));
    EXPECT_ANGLE_DIFF_EQ(ROUND_FLOAT_10000(M_PI_F / 6.0f),
            GeometryUtils::getAngleDiff(M_PI_F / 3.0f, M_PI_F / 2.0f));
    EXPECT_ANGLE_DIFF_EQ(ROUND_FLOAT_10000(M_PI_F / 2.0f),
            GeometryUtils::getAngleDiff(0.0f, M_PI_F * 1.5f));
    EXPECT_ANGLE_DIFF_EQ(0.0f, GeometryUtils::getAngleDiff(0.0f, M_PI_F * 1024.0f));
    EXPECT_ANGLE_DIFF_EQ(0.0f, GeometryUtils::getAngleDiff(-M_PI_F, M_PI_F));
}

TEST(GeometryUtilsTest, testGetDistanceInt) {
    EXPECT_EQ(0, GeometryUtils::getDistanceInt(0, 0, 0, 0));
    EXPECT_EQ(0, GeometryUtils::getAngle(100, -10, 100, -10));

    EXPECT_EQ(5, GeometryUtils::getDistanceInt(0, 0, 5, 0));
    EXPECT_EQ(5, GeometryUtils::getDistanceInt(0, 0, 3, 4));
    EXPECT_EQ(5, GeometryUtils::getDistanceInt(0, -4, 3, 0));
    EXPECT_EQ(5, GeometryUtils::getDistanceInt(0, 0, -3, -4));
    EXPECT_EQ(500, GeometryUtils::getDistanceInt(0, 0, 300, -400));
}

}  // namespace
}  // namespace latinime
