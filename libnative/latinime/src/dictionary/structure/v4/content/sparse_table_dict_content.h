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

#ifndef LATINIME_SPARSE_TABLE_DICT_CONTENT_H
#define LATINIME_SPARSE_TABLE_DICT_CONTENT_H

#include <cstdio>

#include "defines.h"
#include "dictionary/structure/v4/ver4_dict_constants.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "dictionary/utils/sparse_table.h"
#include "utils/byte_array_view.h"

namespace latinime {

// TODO: Support multiple contents.
class SparseTableDictContent {
 public:
    AK_FORCE_INLINE SparseTableDictContent(const ReadWriteByteArrayView *const buffers,
            const int sparseTableBlockSize, const int sparseTableDataSize)
            : mExpandableLookupTableBuffer(buffers[LOOKUP_TABLE_BUFFER_INDEX],
                      BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE),
              mExpandableAddressTableBuffer(buffers[ADDRESS_TABLE_BUFFER_INDEX],
                      BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE),
              mExpandableContentBuffer(buffers[CONTENT_BUFFER_INDEX],
                      BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE),
              mAddressLookupTable(&mExpandableLookupTableBuffer, &mExpandableAddressTableBuffer,
                      sparseTableBlockSize, sparseTableDataSize) {}

    SparseTableDictContent(const int sparseTableBlockSize, const int sparseTableDataSize)
            : mExpandableLookupTableBuffer(Ver4DictConstants::MAX_DICTIONARY_SIZE),
              mExpandableAddressTableBuffer(Ver4DictConstants::MAX_DICTIONARY_SIZE),
              mExpandableContentBuffer(Ver4DictConstants::MAX_DICTIONARY_SIZE),
              mAddressLookupTable(&mExpandableLookupTableBuffer, &mExpandableAddressTableBuffer,
                      sparseTableBlockSize, sparseTableDataSize) {}

    virtual ~SparseTableDictContent() {}

    bool isNearSizeLimit() const {
        return mExpandableLookupTableBuffer.isNearSizeLimit()
                || mExpandableAddressTableBuffer.isNearSizeLimit()
                || mExpandableContentBuffer.isNearSizeLimit();
    }

 protected:
    SparseTable *getUpdatableAddressLookupTable() {
        return &mAddressLookupTable;
    }

    const SparseTable *getAddressLookupTable() const {
        return &mAddressLookupTable;
    }

    BufferWithExtendableBuffer *getWritableContentBuffer() {
        return &mExpandableContentBuffer;
    }

    const BufferWithExtendableBuffer *getContentBuffer() const {
        return &mExpandableContentBuffer;
    }

    bool flush(FILE *const file) const;

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(SparseTableDictContent);

    static const int LOOKUP_TABLE_BUFFER_INDEX;
    static const int ADDRESS_TABLE_BUFFER_INDEX;
    static const int CONTENT_BUFFER_INDEX;

    BufferWithExtendableBuffer mExpandableLookupTableBuffer;
    BufferWithExtendableBuffer mExpandableAddressTableBuffer;
    BufferWithExtendableBuffer mExpandableContentBuffer;
    SparseTable mAddressLookupTable;
};
} // namespace latinime
#endif /* LATINIME_SPARSE_TABLE_DICT_CONTENT_H */
