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

#include "suggest/core/dicnode/dic_node_pool.h"

#include <gtest/gtest.h>

namespace latinime {
namespace {

TEST(DicNodePoolTest, TestGet) {
    static const int CAPACITY = 10;
    DicNodePool dicNodePool(CAPACITY);

    for (int i = 0; i < CAPACITY; ++i) {
        EXPECT_NE(nullptr, dicNodePool.getInstance());
    }
    EXPECT_EQ(nullptr, dicNodePool.getInstance());
}

TEST(DicNodePoolTest, TestPlaceBack) {
    static const int CAPACITY = 1;
    DicNodePool dicNodePool(CAPACITY);

    DicNode *const dicNode = dicNodePool.getInstance();
    EXPECT_NE(nullptr, dicNode);
    EXPECT_EQ(nullptr, dicNodePool.getInstance());
    dicNodePool.placeBackInstance(dicNode);
    EXPECT_EQ(dicNode, dicNodePool.getInstance());
}

TEST(DicNodePoolTest, TestReset) {
    static const int CAPACITY_SMALL = 2;
    static const int CAPACITY_LARGE = 10;
    DicNodePool dicNodePool(CAPACITY_SMALL);

    for (int i = 0; i < CAPACITY_SMALL; ++i) {
        EXPECT_NE(nullptr, dicNodePool.getInstance());
    }
    EXPECT_EQ(nullptr, dicNodePool.getInstance());

    dicNodePool.reset(CAPACITY_LARGE);
    for (int i = 0; i < CAPACITY_LARGE; ++i) {
        EXPECT_NE(nullptr, dicNodePool.getInstance());
    }
    EXPECT_EQ(nullptr, dicNodePool.getInstance());

    dicNodePool.reset(CAPACITY_SMALL);
    for (int i = 0; i < CAPACITY_SMALL; ++i) {
        EXPECT_NE(nullptr, dicNodePool.getInstance());
    }
    EXPECT_EQ(nullptr, dicNodePool.getInstance());
}

}  // namespace
}  // namespace latinime
