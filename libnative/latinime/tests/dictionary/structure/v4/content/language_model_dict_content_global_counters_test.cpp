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

#include "dictionary/structure/v4/content/language_model_dict_content_global_counters.h"

#include <gtest/gtest.h>

#include "dictionary/structure/v4/ver4_dict_constants.h"

namespace latinime {
namespace {

TEST(LanguageModelDictContentGlobalCountersTest, TestUpdateMaxValueOfCounters) {
    LanguageModelDictContentGlobalCounters globalCounters;

    EXPECT_FALSE(globalCounters.needsToHalveCounters());
    globalCounters.updateMaxValueOfCounters(10);
    EXPECT_FALSE(globalCounters.needsToHalveCounters());
    const int count = (1 << (Ver4DictConstants::WORD_COUNT_FIELD_SIZE * CHAR_BIT)) - 1;
    globalCounters.updateMaxValueOfCounters(count);
    EXPECT_TRUE(globalCounters.needsToHalveCounters());
    globalCounters.halveCounters();
    EXPECT_FALSE(globalCounters.needsToHalveCounters());
}

TEST(LanguageModelDictContentGlobalCountersTest, TestIncrementTotalCount) {
    LanguageModelDictContentGlobalCounters globalCounters;

    EXPECT_EQ(0, globalCounters.getTotalCount());
    globalCounters.incrementTotalCount();
    EXPECT_EQ(1, globalCounters.getTotalCount());
    for (int i = 1; i < 50; ++i) {
        globalCounters.incrementTotalCount();
    }
    EXPECT_EQ(50, globalCounters.getTotalCount());
    globalCounters.halveCounters();
    EXPECT_EQ(25, globalCounters.getTotalCount());
    globalCounters.halveCounters();
    EXPECT_EQ(12, globalCounters.getTotalCount());
    for (int i = 0; i < 4; ++i) {
        globalCounters.halveCounters();
    }
    EXPECT_EQ(0, globalCounters.getTotalCount());
}

}  // namespace
}  // namespace latinime
