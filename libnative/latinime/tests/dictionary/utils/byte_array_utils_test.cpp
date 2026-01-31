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

#include "dictionary/utils/byte_array_utils.h"

#include <gtest/gtest.h>

#include <cstdint>

namespace latinime {
namespace {

TEST(ByteArrayUtilsTest, TestReadCodePointTable) {
    const int codePointTable[] = { 0x6f, 0x6b };
    const uint8_t buffer[] = { 0x20u, 0x21u, 0x00u, 0x01u, 0x00u };
    int pos = 0;
    // Expect the first entry of codePointTable
    EXPECT_EQ(0x6f, ByteArrayUtils::readCodePointAndAdvancePosition(buffer, codePointTable, &pos));
    // Expect the second entry of codePointTable
    EXPECT_EQ(0x6b, ByteArrayUtils::readCodePointAndAdvancePosition(buffer, codePointTable, &pos));
    // Expect the original code point from buffer[2] to buffer[4], 0x100
    // It isn't picked from the codePointTable, since it exceeds the range of the codePointTable.
    EXPECT_EQ(0x100, ByteArrayUtils::readCodePointAndAdvancePosition(buffer, codePointTable, &pos));
}

TEST(ByteArrayUtilsTest, TestReadInt) {
    const uint8_t buffer[] = { 0x1u, 0x8Au, 0x0u, 0xAAu };

    EXPECT_EQ(0x01u, ByteArrayUtils::readUint8(buffer, 0));
    EXPECT_EQ(0x8Au, ByteArrayUtils::readUint8(buffer, 1));
    EXPECT_EQ(0x0u, ByteArrayUtils::readUint8(buffer, 2));
    EXPECT_EQ(0xAAu, ByteArrayUtils::readUint8(buffer, 3));

    EXPECT_EQ(0x018Au, ByteArrayUtils::readUint16(buffer, 0));
    EXPECT_EQ(0x8A00u, ByteArrayUtils::readUint16(buffer, 1));
    EXPECT_EQ(0xAAu, ByteArrayUtils::readUint16(buffer, 2));

    EXPECT_EQ(0x18A00AAu, ByteArrayUtils::readUint32(buffer, 0));

    int pos = 0;
    EXPECT_EQ(0x18A00, ByteArrayUtils::readSint24AndAdvancePosition(buffer, &pos));
    pos = 1;
    EXPECT_EQ(-0xA00AA, ByteArrayUtils::readSint24AndAdvancePosition(buffer, &pos));
}

TEST(ByteArrayUtilsTest, TestWriteAndReadInt) {
    uint8_t buffer[4];

    int pos = 0;
    const uint8_t data_1B = 0xC8;
    ByteArrayUtils::writeUintAndAdvancePosition(buffer, data_1B, 1, &pos);
    EXPECT_EQ(data_1B, ByteArrayUtils::readUint(buffer, 1, 0));

    pos = 0;
    const uint32_t data_4B = 0xABCD1234;
    ByteArrayUtils::writeUintAndAdvancePosition(buffer, data_4B, 4, &pos);
    EXPECT_EQ(data_4B, ByteArrayUtils::readUint(buffer, 4, 0));
}

TEST(ByteArrayUtilsTest, TestReadCodePoint) {
    const uint8_t buffer[] = { 0x10, 0xFF, 0x00u, 0x20u, 0x41u, 0x1Fu, 0x60 };

    EXPECT_EQ(0x10FF00, ByteArrayUtils::readCodePoint(buffer, 0));
    EXPECT_EQ(0x20, ByteArrayUtils::readCodePoint(buffer, 3));
    EXPECT_EQ(0x41, ByteArrayUtils::readCodePoint(buffer, 4));
    EXPECT_EQ(NOT_A_CODE_POINT, ByteArrayUtils::readCodePoint(buffer, 5));

    int pos = 0;
    int codePointArray[3];
    EXPECT_EQ(3, ByteArrayUtils::readStringAndAdvancePosition(buffer, MAX_WORD_LENGTH, nullptr,
            codePointArray, &pos));
    EXPECT_EQ(0x10FF00, codePointArray[0]);
    EXPECT_EQ(0x20, codePointArray[1]);
    EXPECT_EQ(0x41, codePointArray[2]);
    EXPECT_EQ(0x60, ByteArrayUtils::readCodePoint(buffer, pos));
}

TEST(ByteArrayUtilsTest, TestWriteAndReadCodePoint) {
    uint8_t buffer[10];

    const int codePointArray[] = { 0x10FF00, 0x20, 0x41 };
    int pos = 0;
    ByteArrayUtils::writeCodePointsAndAdvancePosition(buffer, codePointArray, 3,
            true /* writesTerminator */, &pos);
    EXPECT_EQ(0x10FF00, ByteArrayUtils::readCodePoint(buffer, 0));
    EXPECT_EQ(0x20, ByteArrayUtils::readCodePoint(buffer, 3));
    EXPECT_EQ(0x41, ByteArrayUtils::readCodePoint(buffer, 4));
    EXPECT_EQ(NOT_A_CODE_POINT, ByteArrayUtils::readCodePoint(buffer, 5));
}

}  // namespace
}  // namespace latinime
