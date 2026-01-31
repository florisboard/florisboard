/*
 * Copyright (C) 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef LATINIME_SPARSE_TABLE_H
#define LATINIME_SPARSE_TABLE_H

#include <cstdint>

#include "defines.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

// TODO: Support multiple content buffers.
class SparseTable {
 public:
    SparseTable(BufferWithExtendableBuffer *const indexTableBuffer,
            BufferWithExtendableBuffer *const contentTableBuffer, const int blockSize,
            const int dataSize)
            : mIndexTableBuffer(indexTableBuffer), mContentTableBuffer(contentTableBuffer),
              mBlockSize(blockSize), mDataSize(dataSize) {}

    bool contains(const int id) const;

    uint32_t get(const int id) const;

    bool set(const int id, const uint32_t value);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(SparseTable);

    int getIndexFromContentTablePos(const int contentTablePos) const;

    int getPosInIndexTable(const int id) const;

    int getPosInContentTable(const int id, const int index) const;

    static const int NOT_EXIST;
    static const int INDEX_SIZE;

    BufferWithExtendableBuffer *const mIndexTableBuffer;
    BufferWithExtendableBuffer *const mContentTableBuffer;
    const int mBlockSize;
    const int mDataSize;
};
} // namespace latinime
#endif /* LATINIME_SPARSE_TABLE_H */
