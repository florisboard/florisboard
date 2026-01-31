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

#include "utils/char_utils.h"

#include <gtest/gtest.h>

#include "defines.h"

namespace latinime {
namespace {

TEST(CharUtilsTest, TestIsAsciiUpper) {
    EXPECT_TRUE(CharUtils::isAsciiUpper('A'));
    EXPECT_TRUE(CharUtils::isAsciiUpper('Z'));
    EXPECT_FALSE(CharUtils::isAsciiUpper('a'));
    EXPECT_FALSE(CharUtils::isAsciiUpper('z'));
    EXPECT_FALSE(CharUtils::isAsciiUpper('@'));
    EXPECT_FALSE(CharUtils::isAsciiUpper(' '));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x00C0 /* LATIN CAPITAL LETTER A WITH GRAVE */));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x00E0 /* LATIN SMALL LETTER A WITH GRAVE */));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x0410 /* CYRILLIC CAPITAL LETTER A */));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x0430 /* CYRILLIC SMALL LETTER A */));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x3042 /* HIRAGANA LETTER A */));
    EXPECT_FALSE(CharUtils::isAsciiUpper(0x1F36A /* COOKIE */));
}

TEST(CharUtilsTest, TestToLowerCase) {
    EXPECT_EQ('a', CharUtils::toLowerCase('A'));
    EXPECT_EQ('z', CharUtils::toLowerCase('Z'));
    EXPECT_EQ('a', CharUtils::toLowerCase('a'));
    EXPECT_EQ('z', CharUtils::toLowerCase('z'));
    EXPECT_EQ('@', CharUtils::toLowerCase('@'));
    EXPECT_EQ(' ', CharUtils::toLowerCase(' '));
    EXPECT_EQ(0x00E0 /* LATIN SMALL LETTER A WITH GRAVE */,
            CharUtils::toLowerCase(0x00C0 /* LATIN CAPITAL LETTER A WITH GRAVE */));
    EXPECT_EQ(0x00E0 /* LATIN SMALL LETTER A WITH GRAVE */,
            CharUtils::toLowerCase(0x00E0 /* LATIN SMALL LETTER A WITH GRAVE */));
    EXPECT_EQ(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */,
            CharUtils::toLowerCase(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */));
    EXPECT_EQ(0x0430 /* CYRILLIC SMALL LETTER A */,
            CharUtils::toLowerCase(0x0410 /* CYRILLIC CAPITAL LETTER A */));
    EXPECT_EQ(0x0430 /* CYRILLIC SMALL LETTER A */,
            CharUtils::toLowerCase(0x0430 /* CYRILLIC SMALL LETTER A */));
    EXPECT_EQ(0x3042 /* HIRAGANA LETTER A */,
            CharUtils::toLowerCase(0x3042 /* HIRAGANA LETTER A */));
    EXPECT_EQ(0x1F36A /* COOKIE */, CharUtils::toLowerCase(0x1F36A /* COOKIE */));
}

TEST(CharUtilsTest, TestToBaseLowerCase) {
    EXPECT_EQ('a', CharUtils::toBaseLowerCase('A'));
    EXPECT_EQ('z', CharUtils::toBaseLowerCase('Z'));
    EXPECT_EQ('a', CharUtils::toBaseLowerCase('a'));
    EXPECT_EQ('z', CharUtils::toBaseLowerCase('z'));
    EXPECT_EQ('@', CharUtils::toBaseLowerCase('@'));
    EXPECT_EQ(' ', CharUtils::toBaseLowerCase(' '));
    EXPECT_EQ('a', CharUtils::toBaseLowerCase(0x00C0 /* LATIN CAPITAL LETTER A WITH GRAVE */));
    EXPECT_EQ('a', CharUtils::toBaseLowerCase(0x00E0 /* LATIN SMALL LETTER A WITH GRAVE */));
    EXPECT_EQ(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */,
            CharUtils::toBaseLowerCase(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */));
    EXPECT_EQ(0x0430 /* CYRILLIC SMALL LETTER A */,
            CharUtils::toBaseLowerCase(0x0410 /* CYRILLIC CAPITAL LETTER A */));
    EXPECT_EQ(0x0430 /* CYRILLIC SMALL LETTER A */,
            CharUtils::toBaseLowerCase(0x0430 /* CYRILLIC SMALL LETTER A */));
    EXPECT_EQ(0x3042 /* HIRAGANA LETTER A */,
            CharUtils::toBaseLowerCase(0x3042 /* HIRAGANA LETTER A */));
    EXPECT_EQ(0x1F36A /* COOKIE */, CharUtils::toBaseLowerCase(0x1F36A /* COOKIE */));
}

TEST(CharUtilsTest, TestToBaseCodePoint) {
    EXPECT_EQ('A', CharUtils::toBaseCodePoint('A'));
    EXPECT_EQ('Z', CharUtils::toBaseCodePoint('Z'));
    EXPECT_EQ('a', CharUtils::toBaseCodePoint('a'));
    EXPECT_EQ('z', CharUtils::toBaseCodePoint('z'));
    EXPECT_EQ('@', CharUtils::toBaseCodePoint('@'));
    EXPECT_EQ(' ', CharUtils::toBaseCodePoint(' '));
    EXPECT_EQ('A', CharUtils::toBaseCodePoint(0x00C0 /* LATIN CAPITAL LETTER A WITH GRAVE */));
    EXPECT_EQ('a', CharUtils::toBaseCodePoint(0x00E0 /* LATIN SMALL LETTER A WITH GRAVE */));
    EXPECT_EQ(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */,
            CharUtils::toBaseLowerCase(0x03C2 /* GREEK SMALL LETTER FINAL SIGMA */));
    EXPECT_EQ(0x0410 /* CYRILLIC CAPITAL LETTER A */,
            CharUtils::toBaseCodePoint(0x0410 /* CYRILLIC CAPITAL LETTER A */));
    EXPECT_EQ(0x0430 /* CYRILLIC SMALL LETTER A */,
            CharUtils::toBaseCodePoint(0x0430 /* CYRILLIC SMALL LETTER A */));
    EXPECT_EQ(0x3042 /* HIRAGANA LETTER A */,
            CharUtils::toBaseCodePoint(0x3042 /* HIRAGANA LETTER A */));
    EXPECT_EQ(0x1F36A /* COOKIE */, CharUtils::toBaseCodePoint(0x1F36A /* COOKIE */));
}

TEST(CharUtilsTest, TestIsIntentionalOmissionCodePoint) {
    EXPECT_TRUE(CharUtils::isIntentionalOmissionCodePoint('\''));
    EXPECT_TRUE(CharUtils::isIntentionalOmissionCodePoint('-'));
    EXPECT_FALSE(CharUtils::isIntentionalOmissionCodePoint('a'));
    EXPECT_FALSE(CharUtils::isIntentionalOmissionCodePoint('?'));
    EXPECT_FALSE(CharUtils::isIntentionalOmissionCodePoint('/'));
}

TEST(CharUtilsTest, TestIsInUnicodeSpace) {
    EXPECT_FALSE(CharUtils::isInUnicodeSpace(NOT_A_CODE_POINT));
    EXPECT_FALSE(CharUtils::isInUnicodeSpace(CODE_POINT_BEGINNING_OF_SENTENCE));
    EXPECT_TRUE(CharUtils::isInUnicodeSpace('a'));
    EXPECT_TRUE(CharUtils::isInUnicodeSpace(0x0410 /* CYRILLIC CAPITAL LETTER A */));
    EXPECT_TRUE(CharUtils::isInUnicodeSpace(0x3042 /* HIRAGANA LETTER A */));
    EXPECT_TRUE(CharUtils::isInUnicodeSpace(0x1F36A /* COOKIE */));
}

}  // namespace
}  // namespace latinime
