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

#include "defines.h"

#include <gtest/gtest.h>

namespace latinime {
namespace {

TEST(DefinesTest, NELEMSForFixedLengthArray) {
    const size_t SMALL_ARRAY_SIZE = 1;
    const size_t LARGE_ARRAY_SIZE = 100;
    int smallArray[SMALL_ARRAY_SIZE];
    int largeArray[LARGE_ARRAY_SIZE];
    EXPECT_EQ(SMALL_ARRAY_SIZE, NELEMS(smallArray));
    EXPECT_EQ(LARGE_ARRAY_SIZE, NELEMS(largeArray));
}

}  // namespace
}  // namespace latinime
