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

#include "dictionary/structure/pt_common/bigram/bigram_list_read_write_utils.h"

#include "dictionary/utils/byte_array_utils.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

const BigramListReadWriteUtils::BigramFlags BigramListReadWriteUtils::MASK_ATTRIBUTE_ADDRESS_TYPE =
        0x30;
const BigramListReadWriteUtils::BigramFlags
        BigramListReadWriteUtils::FLAG_ATTRIBUTE_ADDRESS_TYPE_ONEBYTE = 0x10;
const BigramListReadWriteUtils::BigramFlags
        BigramListReadWriteUtils::FLAG_ATTRIBUTE_ADDRESS_TYPE_TWOBYTES = 0x20;
const BigramListReadWriteUtils::BigramFlags
        BigramListReadWriteUtils::FLAG_ATTRIBUTE_ADDRESS_TYPE_THREEBYTES = 0x30;
const BigramListReadWriteUtils::BigramFlags
        BigramListReadWriteUtils::FLAG_ATTRIBUTE_OFFSET_NEGATIVE = 0x40;
// Flag for presence of more attributes
const BigramListReadWriteUtils::BigramFlags BigramListReadWriteUtils::FLAG_ATTRIBUTE_HAS_NEXT =
        0x80;
// Mask for attribute probability, stored on 4 bits inside the flags byte.
const BigramListReadWriteUtils::BigramFlags
        BigramListReadWriteUtils::MASK_ATTRIBUTE_PROBABILITY = 0x0F;

/* static */ bool BigramListReadWriteUtils::getBigramEntryPropertiesAndAdvancePosition(
        const ReadOnlyByteArrayView buffer, BigramFlags *const outBigramFlags,
        int *const outTargetPtNodePos, int *const bigramEntryPos) {
    if (static_cast<int>(buffer.size()) <= *bigramEntryPos) {
        AKLOGE("Read invalid pos in getBigramEntryPropertiesAndAdvancePosition(). bufSize: %zd, "
                "bigramEntryPos: %d.", buffer.size(), *bigramEntryPos);
        return false;
    }
    const BigramFlags bigramFlags = ByteArrayUtils::readUint8AndAdvancePosition(buffer.data(),
            bigramEntryPos);
    if (outBigramFlags) {
        *outBigramFlags = bigramFlags;
    }
    const int targetPos = getBigramAddressAndAdvancePosition(buffer, bigramFlags, bigramEntryPos);
    if (outTargetPtNodePos) {
        *outTargetPtNodePos = targetPos;
    }
    return true;
}

/* static */ bool BigramListReadWriteUtils::skipExistingBigrams(const ReadOnlyByteArrayView buffer,
        int *const bigramListPos) {
    BigramFlags flags;
    do {
        if (!getBigramEntryPropertiesAndAdvancePosition(buffer, &flags, 0 /* outTargetPtNodePos */,
                bigramListPos)) {
            return false;
        }
    } while(hasNext(flags));
    return true;
}

/* static */ int BigramListReadWriteUtils::getBigramAddressAndAdvancePosition(
        const ReadOnlyByteArrayView buffer, const BigramFlags flags, int *const pos) {
    int offset = 0;
    const int origin = *pos;
    switch (MASK_ATTRIBUTE_ADDRESS_TYPE & flags) {
        case FLAG_ATTRIBUTE_ADDRESS_TYPE_ONEBYTE:
            offset = ByteArrayUtils::readUint8AndAdvancePosition(buffer.data(), pos);
            break;
        case FLAG_ATTRIBUTE_ADDRESS_TYPE_TWOBYTES:
            offset = ByteArrayUtils::readUint16AndAdvancePosition(buffer.data(), pos);
            break;
        case FLAG_ATTRIBUTE_ADDRESS_TYPE_THREEBYTES:
            offset = ByteArrayUtils::readUint24AndAdvancePosition(buffer.data(), pos);
            break;
    }
    if (isOffsetNegative(flags)) {
        return origin - offset;
    } else {
        return origin + offset;
    }
}

} // namespace latinime
