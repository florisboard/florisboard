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

#include "suggest/policyimpl/utils/damerau_levenshtein_edit_distance_policy.h"

#include <gtest/gtest.h>

#include <vector>

#include "suggest/policyimpl/utils/edit_distance.h"
#include "utils/int_array_view.h"

namespace latinime {
namespace {

TEST(DamerauLevenshteinEditDistancePolicyTest, TestConstructPolicy) {
    const std::vector<int> codePoints0 = { 0x20, 0x40, 0x60 };
    const std::vector<int> codePoints1 = { 0x10, 0x20, 0x30, 0x40, 0x50, 0x60 };
    DamerauLevenshteinEditDistancePolicy policy(codePoints0.data(), codePoints0.size(),
            codePoints1.data(), codePoints1.size());

    EXPECT_EQ(static_cast<int>(codePoints0.size()), policy.getString0Length());
    EXPECT_EQ(static_cast<int>(codePoints1.size()), policy.getString1Length());
}

float getEditDistance(const std::vector<int> &codePoints0, const std::vector<int> &codePoints1) {
    DamerauLevenshteinEditDistancePolicy policy(codePoints0.data(), codePoints0.size(),
            codePoints1.data(), codePoints1.size());
    return EditDistance::getEditDistance(&policy);
}

TEST(DamerauLevenshteinEditDistancePolicyTest, TestEditDistance) {
    EXPECT_FLOAT_EQ(0.0f, getEditDistance({}, {}));
    EXPECT_FLOAT_EQ(0.0f, getEditDistance({ 1 }, { 1 }));
    EXPECT_FLOAT_EQ(0.0f, getEditDistance({ 1, 2, 3 }, { 1, 2, 3 }));

    EXPECT_FLOAT_EQ(1.0f, getEditDistance({ 1 }, { }));
    EXPECT_FLOAT_EQ(1.0f, getEditDistance({}, { 100 }));
    EXPECT_FLOAT_EQ(5.0f, getEditDistance({}, { 1, 2, 3, 4, 5 }));

    EXPECT_FLOAT_EQ(1.0f, getEditDistance({ 0 }, { 100 }));
    EXPECT_FLOAT_EQ(5.0f, getEditDistance({ 1, 2, 3, 4, 5 }, { 11, 12, 13, 14, 15 }));

    EXPECT_FLOAT_EQ(1.0f, getEditDistance({ 1 }, { 1, 2 }));
    EXPECT_FLOAT_EQ(2.0f, getEditDistance({ 1, 2 }, { 0, 1, 2, 3 }));
    EXPECT_FLOAT_EQ(2.0f, getEditDistance({ 0, 1, 2, 3 }, { 1, 2 }));

    EXPECT_FLOAT_EQ(1.0f, getEditDistance({ 1, 2 }, { 2, 1 }));
    EXPECT_FLOAT_EQ(2.0f, getEditDistance({ 1, 2, 3, 4 }, { 2, 1, 4, 3 }));
}
}  // namespace
}  // namespace latinime
