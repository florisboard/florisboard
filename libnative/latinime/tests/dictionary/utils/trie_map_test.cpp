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

#include "dictionary/utils/trie_map.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cstdlib>
#include <functional>
#include <map>
#include <random>
#include <unordered_map>

namespace latinime {
namespace {

TEST(TrieMapTest, TestSetAndGet) {
    TrieMap trieMap;
    trieMap.putRoot(10, 10);
    EXPECT_EQ(10ull, trieMap.getRoot(10).mValue);
    trieMap.putRoot(0x10A, 10);
    EXPECT_EQ(10ull, trieMap.getRoot(10).mValue);
    EXPECT_EQ(10ull, trieMap.getRoot(0x10A).mValue);
    trieMap.putRoot(10, 1000);
    EXPECT_EQ(1000ull, trieMap.getRoot(10).mValue);
    trieMap.putRoot(11, 1000);
    EXPECT_EQ(1000ull, trieMap.getRoot(11).mValue);
    const int next = trieMap.getNextLevelBitmapEntryIndex(10);
    EXPECT_EQ(1000ull, trieMap.getRoot(10).mValue);
    trieMap.put(9, 9, next);
    EXPECT_EQ(9ull, trieMap.get(9, next).mValue);
    EXPECT_FALSE(trieMap.get(11, next).mIsValid);
    trieMap.putRoot(0, 0xFFFFFFFFFull);
    EXPECT_EQ(0xFFFFFFFFFull, trieMap.getRoot(0).mValue);
}

TEST(TrieMapTest, TestRemove) {
    TrieMap trieMap;
    trieMap.putRoot(10, 10);
    EXPECT_EQ(10ull, trieMap.getRoot(10).mValue);
    EXPECT_TRUE(trieMap.remove(10, trieMap.getRootBitmapEntryIndex()));
    EXPECT_FALSE(trieMap.getRoot(10).mIsValid);
    for (const auto &element : trieMap.getEntriesInRootLevel()) {
        (void)element; // not used
        EXPECT_TRUE(false);
    }
    EXPECT_TRUE(trieMap.putRoot(10, 0x3FFFFF));
    EXPECT_FALSE(trieMap.remove(11, trieMap.getRootBitmapEntryIndex()))
            << "Should fail if the key does not exist.";
    EXPECT_EQ(0x3FFFFFull, trieMap.getRoot(10).mValue);
    trieMap.putRoot(12, 11);
    const int nextLevel = trieMap.getNextLevelBitmapEntryIndex(10);
    trieMap.put(10, 10, nextLevel);
    EXPECT_EQ(0x3FFFFFull, trieMap.getRoot(10).mValue);
    EXPECT_EQ(10ull, trieMap.get(10, nextLevel).mValue);
    EXPECT_TRUE(trieMap.remove(10, trieMap.getRootBitmapEntryIndex()));
    const TrieMap::Result result = trieMap.getRoot(10);
    EXPECT_FALSE(result.mIsValid);
    EXPECT_EQ(TrieMap::INVALID_INDEX, result.mNextLevelBitmapEntryIndex);
    EXPECT_EQ(11ull, trieMap.getRoot(12).mValue);
    EXPECT_TRUE(trieMap.putRoot(S_INT_MAX, 0xFFFFFFFFFull));
    EXPECT_TRUE(trieMap.remove(S_INT_MAX, trieMap.getRootBitmapEntryIndex()));
}

TEST(TrieMapTest, TestSetAndGetLarge) {
    static const int ELEMENT_COUNT = 200000;
    TrieMap trieMap;
    for (int i = 0; i < ELEMENT_COUNT; ++i) {
        EXPECT_TRUE(trieMap.putRoot(i, i));
    }
    for (int i = 0; i < ELEMENT_COUNT; ++i) {
        EXPECT_EQ(static_cast<uint64_t>(i), trieMap.getRoot(i).mValue);
    }
}

TEST(TrieMapTest, TestRandSetAndGetLarge) {
    static const int ELEMENT_COUNT = 100000;
    TrieMap trieMap;
    std::unordered_map<int, uint64_t> testKeyValuePairs;

    // Use the uniform integer distribution [S_INT_MIN, S_INT_MAX].
    std::uniform_int_distribution<int> keyDistribution(S_INT_MIN, S_INT_MAX);
    auto keyRandomNumberGenerator = std::bind(keyDistribution, std::mt19937());

    // Use the uniform distribution [0, TrieMap::MAX_VALUE].
    std::uniform_int_distribution<uint64_t> valueDistribution(0, TrieMap::MAX_VALUE);
    auto valueRandomNumberGenerator = std::bind(valueDistribution, std::mt19937());

    for (int i = 0; i < ELEMENT_COUNT; ++i) {
        const int key = keyRandomNumberGenerator();
        const uint64_t value = valueRandomNumberGenerator();
        EXPECT_TRUE(trieMap.putRoot(key, value)) << key << " " << value;
        testKeyValuePairs[key] = value;
    }
    for (const auto &v : testKeyValuePairs) {
        EXPECT_EQ(v.second, trieMap.getRoot(v.first).mValue);
    }
}

TEST(TrieMapTest, TestMultiLevel) {
    static const int FIRST_LEVEL_ENTRY_COUNT = 10000;
    static const int SECOND_LEVEL_ENTRY_COUNT = 20000;
    static const int THIRD_LEVEL_ENTRY_COUNT = 40000;

    TrieMap trieMap;
    std::vector<int> firstLevelKeys;
    std::map<int, uint64_t> firstLevelEntries;
    std::vector<std::pair<int, int>> secondLevelKeys;
    std::map<int, std::map<int, uint64_t>> twoLevelMap;
    std::map<int, std::map<int, std::map<int, uint64_t>>> threeLevelMap;

    // Use the uniform integer distribution [0, S_INT_MAX].
    std::uniform_int_distribution<int> distribution(0, S_INT_MAX);
    auto keyRandomNumberGenerator = std::bind(distribution, std::mt19937());
    auto randomNumberGeneratorForKeySelection = std::bind(distribution, std::mt19937());

    // Use the uniform distribution [0, TrieMap::MAX_VALUE].
    std::uniform_int_distribution<uint64_t> valueDistribution(0, TrieMap::MAX_VALUE);
    auto valueRandomNumberGenerator = std::bind(valueDistribution, std::mt19937());

    for (int i = 0; i < FIRST_LEVEL_ENTRY_COUNT; ++i) {
        const int key = keyRandomNumberGenerator();
        const uint64_t value = valueRandomNumberGenerator();
        EXPECT_TRUE(trieMap.putRoot(key, value));
        firstLevelKeys.push_back(key);
        firstLevelEntries[key] = value;
    }

    for (int i = 0; i < SECOND_LEVEL_ENTRY_COUNT; ++i) {
        const int key = keyRandomNumberGenerator();
        const uint64_t value = valueRandomNumberGenerator();
        const int firstLevelKey =
                firstLevelKeys[randomNumberGeneratorForKeySelection() % FIRST_LEVEL_ENTRY_COUNT];
        const int nextLevelBitmapEntryIndex = trieMap.getNextLevelBitmapEntryIndex(firstLevelKey);
        EXPECT_NE(TrieMap::INVALID_INDEX, nextLevelBitmapEntryIndex);
        EXPECT_TRUE(trieMap.put(key, value, nextLevelBitmapEntryIndex));
        secondLevelKeys.push_back(std::make_pair(firstLevelKey, key));
        twoLevelMap[firstLevelKey][key] = value;
    }

    for (int i = 0; i < THIRD_LEVEL_ENTRY_COUNT; ++i) {
        const int key = keyRandomNumberGenerator();
        const uint64_t value = valueRandomNumberGenerator();
        const std::pair<int, int> secondLevelKey =
                secondLevelKeys[randomNumberGeneratorForKeySelection() % SECOND_LEVEL_ENTRY_COUNT];
        const int secondLevel = trieMap.getNextLevelBitmapEntryIndex(secondLevelKey.first);
        EXPECT_NE(TrieMap::INVALID_INDEX, secondLevel);
        const int thirdLevel = trieMap.getNextLevelBitmapEntryIndex(
                secondLevelKey.second, secondLevel);
        EXPECT_NE(TrieMap::INVALID_INDEX, thirdLevel);
        EXPECT_TRUE(trieMap.put(key, value, thirdLevel));
        threeLevelMap[secondLevelKey.first][secondLevelKey.second][key] = value;
    }

    for (const auto &firstLevelEntry : firstLevelEntries) {
        EXPECT_EQ(firstLevelEntry.second, trieMap.getRoot(firstLevelEntry.first).mValue);
    }

    for (const auto &firstLevelEntry : twoLevelMap) {
        const int secondLevel = trieMap.getNextLevelBitmapEntryIndex(firstLevelEntry.first);
        EXPECT_NE(TrieMap::INVALID_INDEX, secondLevel);
        for (const auto &secondLevelEntry : firstLevelEntry.second) {
            EXPECT_EQ(secondLevelEntry.second,
                    trieMap.get(secondLevelEntry.first, secondLevel).mValue);
        }
    }

    for (const auto &firstLevelEntry : threeLevelMap) {
        const int secondLevel = trieMap.getNextLevelBitmapEntryIndex(firstLevelEntry.first);
        EXPECT_NE(TrieMap::INVALID_INDEX, secondLevel);
        for (const auto &secondLevelEntry : firstLevelEntry.second) {
            const int thirdLevel =
                    trieMap.getNextLevelBitmapEntryIndex(secondLevelEntry.first, secondLevel);
            EXPECT_NE(TrieMap::INVALID_INDEX, thirdLevel);
            for (const auto &thirdLevelEntry : secondLevelEntry.second) {
                EXPECT_EQ(thirdLevelEntry.second,
                        trieMap.get(thirdLevelEntry.first, thirdLevel).mValue);
            }
        }
    }

    // Iteration
    for (const auto &firstLevelEntry : trieMap.getEntriesInRootLevel()) {
        EXPECT_EQ(trieMap.getRoot(firstLevelEntry.key()).mValue, firstLevelEntry.value());
        EXPECT_EQ(firstLevelEntries[firstLevelEntry.key()], firstLevelEntry.value());
        firstLevelEntries.erase(firstLevelEntry.key());
        for (const auto &secondLevelEntry : firstLevelEntry.getEntriesInNextLevel()) {
            EXPECT_EQ(twoLevelMap[firstLevelEntry.key()][secondLevelEntry.key()],
                    secondLevelEntry.value());
            twoLevelMap[firstLevelEntry.key()].erase(secondLevelEntry.key());
            for (const auto &thirdLevelEntry : secondLevelEntry.getEntriesInNextLevel()) {
                EXPECT_EQ(threeLevelMap[firstLevelEntry.key()][secondLevelEntry.key()]
                        [thirdLevelEntry.key()], thirdLevelEntry.value());
                threeLevelMap[firstLevelEntry.key()][secondLevelEntry.key()].erase(
                        thirdLevelEntry.key());
            }
        }
    }

    // Ensure all entries have been traversed.
    EXPECT_TRUE(firstLevelEntries.empty());
    for (const auto &secondLevelEntry : twoLevelMap) {
        EXPECT_TRUE(secondLevelEntry.second.empty());
    }
    for (const auto &secondLevelEntry : threeLevelMap) {
        for (const auto &thirdLevelEntry : secondLevelEntry.second) {
            EXPECT_TRUE(thirdLevelEntry.second.empty());
        }
    }
}

TEST(TrieMapTest, TestIteration) {
    static const int ELEMENT_COUNT = 200000;
    TrieMap trieMap;
    std::unordered_map<int, uint64_t> testKeyValuePairs;

    // Use the uniform integer distribution [S_INT_MIN, S_INT_MAX].
    std::uniform_int_distribution<int> keyDistribution(S_INT_MIN, S_INT_MAX);
    auto keyRandomNumberGenerator = std::bind(keyDistribution, std::mt19937());

    // Use the uniform distribution [0, TrieMap::MAX_VALUE].
    std::uniform_int_distribution<uint64_t> valueDistribution(0, TrieMap::MAX_VALUE);
    auto valueRandomNumberGenerator = std::bind(valueDistribution, std::mt19937());
    for (int i = 0; i < ELEMENT_COUNT; ++i) {
        const int key = keyRandomNumberGenerator();
        const uint64_t value = valueRandomNumberGenerator();
        EXPECT_TRUE(trieMap.putRoot(key, value));
        testKeyValuePairs[key] = value;
    }
    for (const auto &entry : trieMap.getEntriesInRootLevel()) {
        EXPECT_EQ(trieMap.getRoot(entry.key()).mValue, entry.value());
        EXPECT_EQ(testKeyValuePairs[entry.key()], entry.value());
        testKeyValuePairs.erase(entry.key());
    }
    EXPECT_TRUE(testKeyValuePairs.empty());
}

}  // namespace
}  // namespace latinime
