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

#ifndef LATINIME_MMAPPED_BUFFER_H
#define LATINIME_MMAPPED_BUFFER_H

#include <cstdint>
#include <memory>

#include "defines.h"
#include "utils/byte_array_view.h"

namespace latinime {

class MmappedBuffer {
 public:
    typedef std::unique_ptr<const MmappedBuffer> MmappedBufferPtr;

    static MmappedBufferPtr openBuffer(const char *const path,
            const int bufferOffset, const int bufferSize, const bool isUpdatable);

    // Mmap entire file.
    static MmappedBufferPtr openBuffer(const char *const path, const bool isUpdatable);

    static MmappedBufferPtr openBuffer(const char *const dirPath, const char *const fileName,
            const bool isUpdatable);

    ~MmappedBuffer();

    ReadWriteByteArrayView getReadWriteByteArrayView() const {
        return mByteArrayView;
    }

    ReadOnlyByteArrayView getReadOnlyByteArrayView() const {
        return mByteArrayView.getReadOnlyView();
    }

    AK_FORCE_INLINE bool isUpdatable() const {
        return mIsUpdatable;
    }

 private:
    AK_FORCE_INLINE MmappedBuffer(uint8_t *const buffer, const int bufferSize,
            void *const mmappedBuffer, const int alignedSize, const int mmapFd,
            const bool isUpdatable)
            : mByteArrayView(buffer, bufferSize), mMmappedBuffer(mmappedBuffer),
              mAlignedSize(alignedSize), mMmapFd(mmapFd), mIsUpdatable(isUpdatable) {}

    // Empty file. We have to handle an empty file as a valid part of a dictionary.
    AK_FORCE_INLINE MmappedBuffer(const bool isUpdatable)
            : mByteArrayView(), mMmappedBuffer(nullptr), mAlignedSize(0),
              mMmapFd(0), mIsUpdatable(isUpdatable) {}

    DISALLOW_IMPLICIT_CONSTRUCTORS(MmappedBuffer);

    const ReadWriteByteArrayView mByteArrayView;
    void *const mMmappedBuffer;
    const int mAlignedSize;
    const int mMmapFd;
    const bool mIsUpdatable;
};
}
#endif /* LATINIME_MMAPPED_BUFFER_H */
