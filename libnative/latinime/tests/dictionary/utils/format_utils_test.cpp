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

#include "dictionary/utils/format_utils.h"

#include <gtest/gtest.h>

#include <vector>

#include "utils/byte_array_view.h"

namespace latinime {
namespace {

TEST(FormatUtilsTest, TestMagicNumber) {
    EXPECT_EQ(0x9BC13AFE, FormatUtils::MAGIC_NUMBER) << "Magic number must not be changed.";
}

const std::vector<uint8_t> getBuffer(const int magicNumber, const int version, const uint16_t flags,
        const size_t headerSize) {
    std::vector<uint8_t> buffer;
    buffer.push_back(magicNumber >> 24);
    buffer.push_back(magicNumber >> 16);
    buffer.push_back(magicNumber >> 8);
    buffer.push_back(magicNumber);

    buffer.push_back(version >> 8);
    buffer.push_back(version);

    buffer.push_back(flags >> 8);
    buffer.push_back(flags);

    buffer.push_back(headerSize >> 24);
    buffer.push_back(headerSize >> 16);
    buffer.push_back(headerSize >> 8);
    buffer.push_back(headerSize);
    return buffer;
}

TEST(FormatUtilsTest, TestDetectFormatVersion) {
    EXPECT_EQ(FormatUtils::UNKNOWN_VERSION,
            FormatUtils::detectFormatVersion(ReadOnlyByteArrayView()));

    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER, FormatUtils::VERSION_2, 0, 0);
        EXPECT_EQ(FormatUtils::UNKNOWN_VERSION, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size())));
    }
    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER, FormatUtils::VERSION_202, 0, 0);
        EXPECT_EQ(FormatUtils::VERSION_202, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size())));
    }
    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER, FormatUtils::VERSION_402, 0, 0);
        EXPECT_EQ(FormatUtils::VERSION_402, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size())));
    }
    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER, FormatUtils::VERSION_403, 0, 0);
        EXPECT_EQ(FormatUtils::VERSION_403, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size())));
    }

    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER - 1, FormatUtils::VERSION_402, 0, 0);
        EXPECT_EQ(FormatUtils::UNKNOWN_VERSION, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size())));
    }
    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER, 100, 0, 0);
        EXPECT_EQ(FormatUtils::UNKNOWN_VERSION, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size())));
    }
    {
        const std::vector<uint8_t> buffer =
                getBuffer(FormatUtils::MAGIC_NUMBER, FormatUtils::VERSION_402, 0, 0);
        EXPECT_EQ(FormatUtils::UNKNOWN_VERSION, FormatUtils::detectFormatVersion(
                ReadOnlyByteArrayView(buffer.data(), buffer.size() - 1)));
    }
}

}  // namespace
}  // namespace latinime
