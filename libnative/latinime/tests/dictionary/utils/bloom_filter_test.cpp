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

#include "dictionary/utils/bloom_filter.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cstdlib>
#include <functional>
#include <random>
#include <unordered_set>
#include <vector>

namespace latinime {
namespace {

TEST(BloomFilterTest, TestFilter) {
    static const int TEST_RANDOM_DATA_MAX = 65536;
    static const int ELEMENT_COUNT = 1000;
    std::vector<int> elements;

    // Initialize data set with random integers.
    {
        // Use the uniform integer distribution [0, TEST_RANDOM_DATA_MAX].
        std::uniform_int_distribution<int> distribution(0, TEST_RANDOM_DATA_MAX);
        auto randomNumberGenerator = std::bind(distribution, std::mt19937());
        for (int i = 0; i < ELEMENT_COUNT; ++i) {
            elements.push_back(randomNumberGenerator());
        }
    }

    // Make sure BloomFilter contains nothing by default.
    BloomFilter bloomFilter;
    for (const int elem : elements) {
        ASSERT_FALSE(bloomFilter.isInFilter(elem));
    }

    // Copy some of the test vector into bloom filter.
    std::unordered_set<int> elementsThatHaveBeenSetInFilter;
    {
        // Use the uniform integer distribution [0, 1].
        std::uniform_int_distribution<int> distribution(0, 1);
        auto randomBitGenerator = std::bind(distribution, std::mt19937());
        for (const int elem : elements) {
            if (randomBitGenerator() == 0) {
                bloomFilter.setInFilter(elem);
                elementsThatHaveBeenSetInFilter.insert(elem);
            }
        }
    }

    for (const int elem : elements) {
        const bool existsInFilter = bloomFilter.isInFilter(elem);
        const bool hasBeenSetInFilter =
                elementsThatHaveBeenSetInFilter.find(elem) != elementsThatHaveBeenSetInFilter.end();
        if (hasBeenSetInFilter) {
            EXPECT_TRUE(existsInFilter) << "elem: " << elem;
        }
        if (!existsInFilter) {
            EXPECT_FALSE(hasBeenSetInFilter) << "elem: " << elem;
        }
    }
}

}  // namespace
}  // namespace latinime
