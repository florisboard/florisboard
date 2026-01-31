/*
 * Copyright (C) 2013 The Android Open Source Project
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

namespace latinime {

const size_t BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE = 1024 * 1024;
const int BufferWithExtendableBuffer::NEAR_BUFFER_LIMIT_THRESHOLD_PERCENTILE = 90;
// TODO: Needs to allocate larger memory corresponding to the current vector size.
const size_t BufferWithExtendableBuffer::EXTEND_ADDITIONAL_BUFFER_SIZE_STEP = 128 * 1024;

uint32_t BufferWithExtendableBuffer::readUint(const int size, const int pos) const {
    const bool readingPosIsInAdditionalBuffer = isInAdditionalBuffer(pos);
    const int posInBuffer = readingPosIsInAdditionalBuffer ? pos - mOriginalBuffer.size() : pos;
    return ByteArrayUtils::readUint(getBuffer(readingPosIsInAdditionalBuffer), size, posInBuffer);
}

uint32_t BufferWithExtendableBuffer::readUintAndAdvancePosition(const int size,
        int *const pos) const {
    const uint32_t value = readUint(size, *pos);
    *pos += size;
    return value;
}

void BufferWithExtendableBuffer::readCodePointsAndAdvancePosition(const int maxCodePointCount,
        int *const outCodePoints, int *outCodePointCount, int *const pos) const {
    const bool readingPosIsInAdditionalBuffer = isInAdditionalBuffer(*pos);
    if (readingPosIsInAdditionalBuffer) {
        *pos -= mOriginalBuffer.size();
    }
    // Code point table is not used for dynamic format.
    *outCodePointCount = ByteArrayUtils::readStringAndAdvancePosition(
            getBuffer(readingPosIsInAdditionalBuffer), maxCodePointCount,
            nullptr /* codePointTable */, outCodePoints, pos);
    if (readingPosIsInAdditionalBuffer) {
        *pos += mOriginalBuffer.size();
    }
}

bool BufferWithExtendableBuffer::extend(const int size) {
    return checkAndPrepareWriting(getTailPosition(), size);
}

bool BufferWithExtendableBuffer::writeUint(const uint32_t data, const int size, const int pos) {
    int writingPos = pos;
    return writeUintAndAdvancePosition(data, size, &writingPos);
}

bool BufferWithExtendableBuffer::writeUintAndAdvancePosition(const uint32_t data, const int size,
        int *const pos) {
    if (!(size >= 1 && size <= 4)) {
        AKLOGI("writeUintAndAdvancePosition() is called with invalid size: %d", size);
        ASSERT(false);
        return false;
    }
    if (!checkAndPrepareWriting(*pos, size)) {
        return false;
    }
    const bool usesAdditionalBuffer = isInAdditionalBuffer(*pos);
    uint8_t *const buffer =
            usesAdditionalBuffer ? mAdditionalBuffer.data() : mOriginalBuffer.data();
    if (usesAdditionalBuffer) {
        *pos -= mOriginalBuffer.size();
    }
    ByteArrayUtils::writeUintAndAdvancePosition(buffer, data, size, pos);
    if (usesAdditionalBuffer) {
        *pos += mOriginalBuffer.size();
    }
    return true;
}

bool BufferWithExtendableBuffer::writeCodePointsAndAdvancePosition(const int *const codePoints,
        const int codePointCount, const bool writesTerminator, int *const pos) {
    const size_t size = ByteArrayUtils::calculateRequiredByteCountToStoreCodePoints(
            codePoints, codePointCount, writesTerminator);
    if (!checkAndPrepareWriting(*pos, size)) {
        return false;
    }
    const bool usesAdditionalBuffer = isInAdditionalBuffer(*pos);
    uint8_t *const buffer =
            usesAdditionalBuffer ? mAdditionalBuffer.data() : mOriginalBuffer.data();
    if (usesAdditionalBuffer) {
        *pos -= mOriginalBuffer.size();
    }
    ByteArrayUtils::writeCodePointsAndAdvancePosition(buffer, codePoints, codePointCount,
            writesTerminator, pos);
    if (usesAdditionalBuffer) {
        *pos += mOriginalBuffer.size();
    }
    return true;
}

bool BufferWithExtendableBuffer::extendBuffer(const size_t size) {
    const size_t extendSize = std::max(EXTEND_ADDITIONAL_BUFFER_SIZE_STEP, size);
    const size_t sizeAfterExtending =
            std::min(mAdditionalBuffer.size() + extendSize, mMaxAdditionalBufferSize);
    if (sizeAfterExtending < mAdditionalBuffer.size() + size) {
        return false;
    }
    mAdditionalBuffer.resize(sizeAfterExtending);
    return true;
}

bool BufferWithExtendableBuffer::checkAndPrepareWriting(const int pos, const int size) {
    if (pos < 0 || size < 0) {
        // Invalid position or size.
        return false;
    }
    const size_t totalRequiredSize = static_cast<size_t>(pos + size);
    if (!isInAdditionalBuffer(pos)) {
        // Here don't need to care about the additional buffer.
        if (mOriginalBuffer.size() < totalRequiredSize) {
            // Violate the boundary.
            return false;
        }
        // The buffer has sufficient capacity.
        return true;
    }
    // Hereafter, pos is in the additional buffer.
    const size_t tailPosition = static_cast<size_t>(getTailPosition());
    if (totalRequiredSize <= tailPosition) {
        // The buffer has sufficient capacity.
        return true;
    }
    if (static_cast<size_t>(pos) != tailPosition) {
        // The additional buffer must be extended from the tail position.
        return false;
    }
    const size_t extendSize = totalRequiredSize -
            std::min(mAdditionalBuffer.size() + mOriginalBuffer.size(), totalRequiredSize);
    if (extendSize > 0 && !extendBuffer(extendSize)) {
        // Failed to extend the buffer.
        return false;
    }
    mUsedAdditionalBufferSize += size;
    return true;
}

bool BufferWithExtendableBuffer::copy(const BufferWithExtendableBuffer *const sourceBuffer) {
    int copyingPos = 0;
    const int tailPos = sourceBuffer->getTailPosition();
    const int maxDataChunkSize = sizeof(uint32_t);
    while (copyingPos < tailPos) {
        const int remainingSize = tailPos - copyingPos;
        const int copyingSize = (remainingSize >= maxDataChunkSize) ?
                maxDataChunkSize : remainingSize;
        const uint32_t data = sourceBuffer->readUint(copyingSize, copyingPos);
        if (!writeUint(data, copyingSize, copyingPos)) {
            return false;
        }
        copyingPos += copyingSize;
    }
    return true;
}

}
