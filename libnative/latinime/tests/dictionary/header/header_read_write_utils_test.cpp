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

#include "dictionary/header/header_read_write_utils.h"

#include <gtest/gtest.h>

#include <cstring>
#include <vector>

#include "dictionary/interface/dictionary_header_structure_policy.h"

namespace latinime {
namespace {

TEST(HeaderReadWriteUtilsTest, TestInsertCharactersIntoVector) {
    DictionaryHeaderStructurePolicy::AttributeMap::key_type vector;

    HeaderReadWriteUtils::insertCharactersIntoVector("", &vector);
    EXPECT_TRUE(vector.empty());

    static const char *str = "abc-xyz!?";
    HeaderReadWriteUtils::insertCharactersIntoVector(str, &vector);
    EXPECT_EQ(strlen(str) , vector.size());
    for (size_t i = 0; i < vector.size(); ++i) {
        EXPECT_EQ(str[i], vector[i]);
    }
}

TEST(HeaderReadWriteUtilsTest, TestAttributeMapForInt) {
    DictionaryHeaderStructurePolicy::AttributeMap attributeMap;

    // Returns default value if not exists.
    EXPECT_EQ(-1, HeaderReadWriteUtils::readIntAttributeValue(&attributeMap, "", -1));
    EXPECT_EQ(100, HeaderReadWriteUtils::readIntAttributeValue(&attributeMap, "abc", 100));

    HeaderReadWriteUtils::setIntAttribute(&attributeMap, "abc", 10);
    EXPECT_EQ(10, HeaderReadWriteUtils::readIntAttributeValue(&attributeMap, "abc", 100));
    HeaderReadWriteUtils::setIntAttribute(&attributeMap, "abc", 20);
    EXPECT_EQ(20, HeaderReadWriteUtils::readIntAttributeValue(&attributeMap, "abc", 100));
    HeaderReadWriteUtils::setIntAttribute(&attributeMap, "abcd", 30);
    EXPECT_EQ(30, HeaderReadWriteUtils::readIntAttributeValue(&attributeMap, "abcd", 100));
    EXPECT_EQ(20, HeaderReadWriteUtils::readIntAttributeValue(&attributeMap, "abc", 100));
}

TEST(HeaderReadWriteUtilsTest, TestAttributeMapCodeForPoints) {
    DictionaryHeaderStructurePolicy::AttributeMap attributeMap;

    // Returns empty vector if not exists.
    EXPECT_TRUE(HeaderReadWriteUtils::readCodePointVectorAttributeValue(&attributeMap, "").empty());
    EXPECT_TRUE(HeaderReadWriteUtils::readCodePointVectorAttributeValue(
            &attributeMap, "abc").empty());

    HeaderReadWriteUtils::setCodePointVectorAttribute(&attributeMap, "abc", {});
    EXPECT_TRUE(HeaderReadWriteUtils::readCodePointVectorAttributeValue(
            &attributeMap, "abc").empty());

    const std::vector<int> codePoints = { 0x0, 0x20, 0x1F, 0x100000 };
    HeaderReadWriteUtils::setCodePointVectorAttribute(&attributeMap, "abc", codePoints);
    EXPECT_EQ(codePoints, HeaderReadWriteUtils::readCodePointVectorAttributeValue(
            &attributeMap, "abc"));
}

}  // namespace
}  // namespace latinime
