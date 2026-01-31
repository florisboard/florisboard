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

#include "dictionary/utils/sparse_table.h"

#include <gtest/gtest.h>

#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {
namespace {

TEST(SparseTableTest, TestSetAndGet) {
    static const int BLOCK_SIZE = 64;
    static const int DATA_SIZE = 4;
    BufferWithExtendableBuffer indexTableBuffer(
            BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE);
    BufferWithExtendableBuffer contentTableBuffer(
            BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE);
    SparseTable sparseTable(&indexTableBuffer, &contentTableBuffer, BLOCK_SIZE, DATA_SIZE);

    EXPECT_FALSE(sparseTable.contains(10));
    EXPECT_TRUE(sparseTable.set(10, 100u));
    EXPECT_EQ(100u, sparseTable.get(10));
    EXPECT_TRUE(sparseTable.contains(10));
    EXPECT_TRUE(sparseTable.contains(BLOCK_SIZE - 1));
    EXPECT_FALSE(sparseTable.contains(BLOCK_SIZE));
    EXPECT_TRUE(sparseTable.set(11, 101u));
    EXPECT_EQ(100u, sparseTable.get(10));
    EXPECT_EQ(101u, sparseTable.get(11));
}

}  // namespace
}  // namespace latinime
