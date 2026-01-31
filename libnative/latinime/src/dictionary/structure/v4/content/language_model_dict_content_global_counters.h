/*
 * Copyright (C) 2014, The Android Open Source Project
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

#ifndef LATINIME_LANGUAGE_MODEL_DICT_CONTENT_GLOBAL_COUNTERS_H
#define LATINIME_LANGUAGE_MODEL_DICT_CONTENT_GLOBAL_COUNTERS_H

#include <cstdio>

#include "defines.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "dictionary/utils/dict_file_writing_utils.h"
#include "utils/byte_array_view.h"

namespace latinime {

class LanguageModelDictContentGlobalCounters {
 public:
    explicit LanguageModelDictContentGlobalCounters(const ReadWriteByteArrayView buffer)
            : mBuffer(buffer, 0 /* maxAdditionalBufferSize */),
              mTotalCount(readValue(mBuffer, TOTAL_COUNT_INDEX)),
              mMaxValueOfCounters(readValue(mBuffer, MAX_VALUE_OF_COUNTERS_INDEX)) {}

    LanguageModelDictContentGlobalCounters()
            : mBuffer(0 /* maxAdditionalBufferSize */), mTotalCount(0), mMaxValueOfCounters(0) {}

    bool needsToHalveCounters() const {
        return mMaxValueOfCounters >= COUNTER_VALUE_NEAR_LIMIT_THRESHOLD
                || mTotalCount >= TOTAL_COUNT_VALUE_NEAR_LIMIT_THRESHOLD;
    }

    int getTotalCount() const {
        return mTotalCount;
    }

    bool save(FILE *const file) const {
        BufferWithExtendableBuffer bufferToWrite(
                BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE);
        if (!bufferToWrite.writeUint(mTotalCount, COUNTER_SIZE_IN_BYTES,
                TOTAL_COUNT_INDEX * COUNTER_SIZE_IN_BYTES)) {
            return false;
        }
        if (!bufferToWrite.writeUint(mMaxValueOfCounters, COUNTER_SIZE_IN_BYTES,
                MAX_VALUE_OF_COUNTERS_INDEX * COUNTER_SIZE_IN_BYTES)) {
            return false;
        }
        return DictFileWritingUtils::writeBufferToFileTail(file, &bufferToWrite);
    }

    void incrementTotalCount() {
        mTotalCount += 1;
    }

    void addToTotalCount(const int count) {
        mTotalCount += count;
    }

    void updateMaxValueOfCounters(const int count) {
        mMaxValueOfCounters = std::max(count, mMaxValueOfCounters);
    }

    void halveCounters() {
        mMaxValueOfCounters /= 2;
        mTotalCount /= 2;
    }

private:
    DISALLOW_COPY_AND_ASSIGN(LanguageModelDictContentGlobalCounters);

    const static int COUNTER_VALUE_NEAR_LIMIT_THRESHOLD;
    const static int TOTAL_COUNT_VALUE_NEAR_LIMIT_THRESHOLD;
    const static int COUNTER_SIZE_IN_BYTES;
    const static int TOTAL_COUNT_INDEX;
    const static int MAX_VALUE_OF_COUNTERS_INDEX;

    BufferWithExtendableBuffer mBuffer;
    int mTotalCount;
    int mMaxValueOfCounters;

    static int readValue(const BufferWithExtendableBuffer &buffer, const int index) {
        const int pos = COUNTER_SIZE_IN_BYTES * index;
        if (pos + COUNTER_SIZE_IN_BYTES > buffer.getTailPosition()) {
            return 0;
        }
        return buffer.readUint(COUNTER_SIZE_IN_BYTES, pos);
    }
};
} // namespace latinime
#endif /* LATINIME_LANGUAGE_MODEL_DICT_CONTENT_GLOBAL_COUNTERS_H */
