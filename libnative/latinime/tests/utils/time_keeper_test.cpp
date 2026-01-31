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

#include "utils/time_keeper.h"

#include <gtest/gtest.h>

namespace latinime {
namespace {

TEST(TimeKeeperTest, TestTestMode) {
    TimeKeeper::setCurrentTime();
    const int startTime = TimeKeeper::peekCurrentTime();
    static const int TEST_CURRENT_TIME = 100;
    TimeKeeper::startTestModeWithForceCurrentTime(TEST_CURRENT_TIME);
    EXPECT_EQ(TEST_CURRENT_TIME, TimeKeeper::peekCurrentTime());
    TimeKeeper::setCurrentTime();
    EXPECT_EQ(TEST_CURRENT_TIME, TimeKeeper::peekCurrentTime());
    TimeKeeper::stopTestMode();
    TimeKeeper::setCurrentTime();
    EXPECT_LE(startTime, TimeKeeper::peekCurrentTime());
}

}  // namespace
}  // namespace latinime
