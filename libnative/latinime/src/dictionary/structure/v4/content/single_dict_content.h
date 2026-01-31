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

#ifndef LATINIME_SINGLE_DICT_CONTENT_H
#define LATINIME_SINGLE_DICT_CONTENT_H

#include <cstdio>

#include "defines.h"
#include "dictionary/structure/v4/ver4_dict_constants.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "dictionary/utils/dict_file_writing_utils.h"
#include "utils/byte_array_view.h"

namespace latinime {

class SingleDictContent {
 public:
    SingleDictContent(const ReadWriteByteArrayView buffer)
            : mExpandableContentBuffer(buffer,
                    BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE) {}

    SingleDictContent()
            : mExpandableContentBuffer(Ver4DictConstants::MAX_DICTIONARY_SIZE) {}

    virtual ~SingleDictContent() {}

    bool isNearSizeLimit() const {
        return mExpandableContentBuffer.isNearSizeLimit();
    }

 protected:
    BufferWithExtendableBuffer *getWritableBuffer() {
        return &mExpandableContentBuffer;
    }

    const BufferWithExtendableBuffer *getBuffer() const {
        return &mExpandableContentBuffer;
    }

    bool flush(FILE *const file) const {
        return DictFileWritingUtils::writeBufferToFileTail(file, &mExpandableContentBuffer);
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(SingleDictContent);

    BufferWithExtendableBuffer mExpandableContentBuffer;
};
} // namespace latinime
#endif /* LATINIME_SINGLE_DICT_CONTENT_H */
