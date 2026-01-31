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

#ifndef LATINIME_BYTE_ARRAY_VIEW_H
#define LATINIME_BYTE_ARRAY_VIEW_H

#include <cstdint>
#include <cstdlib>

#include "defines.h"

namespace latinime {

/**
 * Helper class used to keep track of read accesses for a given memory region.
 */
class ReadOnlyByteArrayView {
 public:
    ReadOnlyByteArrayView() : mPtr(nullptr), mSize(0) {}

    ReadOnlyByteArrayView(const uint8_t *const ptr, const size_t size)
            : mPtr(ptr), mSize(size) {}

    AK_FORCE_INLINE size_t size() const {
        return mSize;
    }

    AK_FORCE_INLINE const uint8_t *data() const {
        return mPtr;
    }

    AK_FORCE_INLINE const ReadOnlyByteArrayView skip(const size_t n) const {
        if (mSize <= n) {
            return ReadOnlyByteArrayView();
        }
        return ReadOnlyByteArrayView(mPtr + n, mSize - n);
    }

 private:
    DISALLOW_ASSIGNMENT_OPERATOR(ReadOnlyByteArrayView);

    const uint8_t *const mPtr;
    const size_t mSize;
};

/**
 * Helper class used to keep track of read-write accesses for a given memory region.
 */
class ReadWriteByteArrayView {
 public:
    ReadWriteByteArrayView() : mPtr(nullptr), mSize(0) {}

    ReadWriteByteArrayView(uint8_t *const ptr, const size_t size)
            : mPtr(ptr), mSize(size) {}

    AK_FORCE_INLINE size_t size() const {
        return mSize;
    }

    AK_FORCE_INLINE uint8_t *data() const {
        return mPtr;
    }

    AK_FORCE_INLINE ReadOnlyByteArrayView getReadOnlyView() const {
        return ReadOnlyByteArrayView(mPtr, mSize);
    }

    ReadWriteByteArrayView subView(const size_t start, const size_t n) const {
        ASSERT(start + n <= mSize);
        return ReadWriteByteArrayView(mPtr + start, n);
    }

 private:
    // Default copy constructor and assignment operator are used for using this class with STL
    // containers.

    // These members cannot be const to have the assignment operator.
    uint8_t *mPtr;
    size_t mSize;
};

} // namespace latinime
#endif // LATINIME_BYTE_ARRAY_VIEW_H
