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

#ifndef LATINIME_BUFFER_WITH_EXTENDABLE_BUFFER_H
#define LATINIME_BUFFER_WITH_EXTENDABLE_BUFFER_H

#include <cstddef>
#include <cstdint>
#include <vector>

#include "defines.h"
#include "dictionary/utils/byte_array_utils.h"
#include "utils/byte_array_view.h"

namespace latinime {

// This is used as a buffer that can be extended for updatable dictionaries.
// To optimize performance, raw pointer is directly used for reading buffer. The position has to be
// adjusted to access additional buffer. On the other hand, this class does not provide writable
// raw pointer but provides several methods that handle boundary checking for writing data.
class BufferWithExtendableBuffer {
 public:
    static const size_t DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE;

    BufferWithExtendableBuffer(const ReadWriteByteArrayView originalBuffer,
            const int maxAdditionalBufferSize)
            : mOriginalBuffer(originalBuffer), mAdditionalBuffer(), mUsedAdditionalBufferSize(0),
              mMaxAdditionalBufferSize(maxAdditionalBufferSize) {}

    // Without original buffer.
    BufferWithExtendableBuffer(const int maxAdditionalBufferSize)
            : mOriginalBuffer(), mAdditionalBuffer(), mUsedAdditionalBufferSize(0),
              mMaxAdditionalBufferSize(maxAdditionalBufferSize) {}

    AK_FORCE_INLINE int getTailPosition() const {
        return mOriginalBuffer.size() + mUsedAdditionalBufferSize;
    }

    AK_FORCE_INLINE int getUsedAdditionalBufferSize() const {
        return mUsedAdditionalBufferSize;
    }

    /**
     * For reading.
     */
    AK_FORCE_INLINE bool isInAdditionalBuffer(const int position) const {
        return position >= static_cast<int>(mOriginalBuffer.size());
    }

    // TODO: Resolve the issue that the address can be changed when the vector is resized.
    // CAVEAT!: Be careful about array out of bound access with buffers
    AK_FORCE_INLINE const uint8_t *getBuffer(const bool usesAdditionalBuffer) const {
        if (usesAdditionalBuffer) {
            return mAdditionalBuffer.data();
        } else {
            return mOriginalBuffer.data();
        }
    }

    uint32_t readUint(const int size, const int pos) const;

    uint32_t readUintAndAdvancePosition(const int size, int *const pos) const;

    void readCodePointsAndAdvancePosition(const int maxCodePointCount,
            int *const outCodePoints, int *outCodePointCount, int *const pos) const;

    AK_FORCE_INLINE int getOriginalBufferSize() const {
        return mOriginalBuffer.size();
    }

    AK_FORCE_INLINE bool isNearSizeLimit() const {
        return mAdditionalBuffer.size() >= ((mMaxAdditionalBufferSize
                * NEAR_BUFFER_LIMIT_THRESHOLD_PERCENTILE) / 100);
    }

    bool extend(const int size);

    /**
     * For writing.
     *
     * Writing is allowed for original buffer, already written region of additional buffer and the
     * tail of additional buffer.
     */
    bool writeUint(const uint32_t data, const int size, const int pos);

    bool writeUintAndAdvancePosition(const uint32_t data, const int size, int *const pos);

    bool writeCodePointsAndAdvancePosition(const int *const codePoints, const int codePointCount,
            const bool writesTerminator, int *const pos);

    bool copy(const BufferWithExtendableBuffer *const sourceBuffer);

 private:
    DISALLOW_COPY_AND_ASSIGN(BufferWithExtendableBuffer);

    static const int NEAR_BUFFER_LIMIT_THRESHOLD_PERCENTILE;
    static const size_t EXTEND_ADDITIONAL_BUFFER_SIZE_STEP;

    const ReadWriteByteArrayView mOriginalBuffer;
    std::vector<uint8_t> mAdditionalBuffer;
    int mUsedAdditionalBufferSize;
    const size_t mMaxAdditionalBufferSize;

    // Return if the buffer is successfully extended or not.
    bool extendBuffer(const size_t size);

    // Returns if it is possible to write size-bytes from pos. When pos is at the tail position of
    // the additional buffer, try extending the buffer.
    bool checkAndPrepareWriting(const int pos, const int size);
};
}
#endif /* LATINIME_BUFFER_WITH_EXTENDABLE_BUFFER_H */
