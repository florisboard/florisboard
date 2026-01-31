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

#include "dictionary/utils/buffer_with_extendable_buffer.h"

#include <gtest/gtest.h>

namespace latinime {
namespace {

const int DEFAULT_MAX_BUFFER_SIZE = 1024;

TEST(BufferWithExtendablebufferTest, TestWriteAndRead) {
    BufferWithExtendableBuffer buffer(DEFAULT_MAX_BUFFER_SIZE);
    int pos = 0;
    // 1 byte
    const uint32_t data_1 = 0xFF;
    EXPECT_TRUE(buffer.writeUint(data_1, 1 /* size */, pos));
    EXPECT_EQ(data_1, buffer.readUint(1, pos));
    pos += 1;
    // 2 byte
    const uint32_t data_2 = 0xFFFF;
    EXPECT_TRUE(buffer.writeUint(data_2, 2 /* size */, pos));
    EXPECT_EQ(data_2, buffer.readUint(2, pos));
    pos += 2;
    // 3 byte
    const uint32_t data_3 = 0xFFFFFF;
    EXPECT_TRUE(buffer.writeUint(data_3, 3 /* size */, pos));
    EXPECT_EQ(data_3, buffer.readUint(3, pos));
    pos += 3;
    // 4 byte
    const uint32_t data_4 = 0xFFFFFFFF;
    EXPECT_TRUE(buffer.writeUint(data_4, 4 /* size */, pos));
    EXPECT_EQ(data_4, buffer.readUint(4, pos));
}

TEST(BufferWithExtendablebufferTest, TestExtend) {
    BufferWithExtendableBuffer buffer(DEFAULT_MAX_BUFFER_SIZE);
    EXPECT_EQ(0, buffer.getTailPosition());
    EXPECT_TRUE(buffer.writeUint(0xFF /* data */, 4 /* size */, 0 /* pos */));
    EXPECT_EQ(4, buffer.getTailPosition());
    EXPECT_TRUE(buffer.extend(8 /* size */));
    EXPECT_EQ(12, buffer.getTailPosition());
    EXPECT_TRUE(buffer.writeUint(0xFFFF /* data */, 4 /* size */, 8 /* pos */));
    EXPECT_TRUE(buffer.writeUint(0xFF /* data */, 4 /* size */, 0 /* pos */));
}

TEST(BufferWithExtendablebufferTest, TestCopy) {
    BufferWithExtendableBuffer buffer(DEFAULT_MAX_BUFFER_SIZE);
    EXPECT_TRUE(buffer.writeUint(0xFF /* data */, 4 /* size */, 0 /* pos */));
    EXPECT_TRUE(buffer.writeUint(0xFFFF /* data */, 4 /* size */, 4 /* pos */));
    BufferWithExtendableBuffer targetBuffer(DEFAULT_MAX_BUFFER_SIZE);
    EXPECT_TRUE(targetBuffer.copy(&buffer));
    EXPECT_EQ(0xFFu, targetBuffer.readUint(4 /* size */, 0 /* pos */));
    EXPECT_EQ(0xFFFFu, targetBuffer.readUint(4 /* size */, 4 /* pos */));
}

TEST(BufferWithExtendablebufferTest, TestSizeLimit) {
    BufferWithExtendableBuffer emptyBuffer(0 /* maxAdditionalBufferSize */);
    EXPECT_FALSE(emptyBuffer.writeUint(0 /* data */, 1 /* size */, 0 /* pos */));
    EXPECT_FALSE(emptyBuffer.extend(1 /* size */));

    BufferWithExtendableBuffer smallBuffer(4 /* maxAdditionalBufferSize */);
    EXPECT_TRUE(smallBuffer.writeUint(0 /* data */, 4 /* size */, 0 /* pos */));
    EXPECT_FALSE(smallBuffer.writeUint(0 /* data */, 1 /* size */, 4 /* pos */));

    EXPECT_TRUE(smallBuffer.copy(&emptyBuffer));
    EXPECT_FALSE(emptyBuffer.copy(&smallBuffer));

    BufferWithExtendableBuffer buffer(DEFAULT_MAX_BUFFER_SIZE);
    EXPECT_FALSE(buffer.isNearSizeLimit());
    int pos = 0;
    while (!buffer.isNearSizeLimit()) {
        EXPECT_TRUE(buffer.writeUintAndAdvancePosition(0 /* data */, 4 /* size */, &pos));
    }
    EXPECT_GT(pos, 0);
    EXPECT_LE(pos, DEFAULT_MAX_BUFFER_SIZE);
}

}  // namespace
}  // namespace latinime
