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

#include "dictionary/utils/probability_utils.h"

#include <gtest/gtest.h>

#include "defines.h"

namespace latinime {
namespace {

TEST(ProbabilityUtilsTest, TestEncodeRawProbability) {
    EXPECT_EQ(MAX_PROBABILITY, ProbabilityUtils::encodeRawProbability(1.0f));
    EXPECT_EQ(MAX_PROBABILITY - 9, ProbabilityUtils::encodeRawProbability(0.5f));
    EXPECT_EQ(0, ProbabilityUtils::encodeRawProbability(0.0f));
}

}  // namespace
}  // namespace latinime
