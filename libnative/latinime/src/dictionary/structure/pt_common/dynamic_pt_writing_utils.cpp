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

#include "dictionary/structure/pt_common/dynamic_pt_writing_utils.h"

#include <cstddef>
#include <cstdint>
#include <cstdlib>

#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

const size_t DynamicPtWritingUtils::MAX_PTNODE_ARRAY_SIZE_TO_USE_SMALL_SIZE_FIELD = 0x7F;
const size_t DynamicPtWritingUtils::MAX_PTNODE_ARRAY_SIZE = 0x7FFF;
const int DynamicPtWritingUtils::SMALL_PTNODE_ARRAY_SIZE_FIELD_SIZE = 1;
const int DynamicPtWritingUtils::LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE = 2;
const int DynamicPtWritingUtils::LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE_FLAG = 0x8000;
const int DynamicPtWritingUtils::DICT_OFFSET_FIELD_SIZE = 3;
const int DynamicPtWritingUtils::MAX_DICT_OFFSET_VALUE = 0x7FFFFF;
const int DynamicPtWritingUtils::MIN_DICT_OFFSET_VALUE = -0x7FFFFF;
const int DynamicPtWritingUtils::DICT_OFFSET_NEGATIVE_FLAG = 0x800000;
const int DynamicPtWritingUtils::NODE_FLAG_FIELD_SIZE = 1;

/* static */ bool DynamicPtWritingUtils::writeEmptyDictionary(
        BufferWithExtendableBuffer *const buffer, const int rootPos) {
    int writingPos = rootPos;
    if (!writePtNodeArraySizeAndAdvancePosition(buffer, 0 /* arraySize */, &writingPos)) {
        return false;
    }
    return writeForwardLinkPositionAndAdvancePosition(buffer, NOT_A_DICT_POS /* forwardLinkPos */,
            &writingPos);
}

/* static */ bool DynamicPtWritingUtils::writeForwardLinkPositionAndAdvancePosition(
        BufferWithExtendableBuffer *const buffer, const int forwardLinkPos,
        int *const forwardLinkFieldPos) {
    return writeDictOffset(buffer, forwardLinkPos, (*forwardLinkFieldPos), forwardLinkFieldPos);
}

/* static */ bool DynamicPtWritingUtils::writePtNodeArraySizeAndAdvancePosition(
        BufferWithExtendableBuffer *const buffer, const size_t arraySize,
        int *const arraySizeFieldPos) {
    // Currently, all array size field to be created has LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE to
    // simplify updating process.
    // TODO: Use SMALL_PTNODE_ARRAY_SIZE_FIELD_SIZE for small arrays.
    /*if (arraySize <= MAX_PTNODE_ARRAY_SIZE_TO_USE_SMALL_SIZE_FIELD) {
        return buffer->writeUintAndAdvancePosition(arraySize, SMALL_PTNODE_ARRAY_SIZE_FIELD_SIZE,
                arraySizeFieldPos);
    } else */
    if (arraySize <= MAX_PTNODE_ARRAY_SIZE) {
        uint32_t data = arraySize | LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE_FLAG;
        return buffer->writeUintAndAdvancePosition(data, LARGE_PTNODE_ARRAY_SIZE_FIELD_SIZE,
                arraySizeFieldPos);
    } else {
        AKLOGI("PtNode array size cannot be written because arraySize is too large: %zd",
                arraySize);
        ASSERT(false);
        return false;
    }
}

/* static */ bool DynamicPtWritingUtils::writeFlagsAndAdvancePosition(
        BufferWithExtendableBuffer *const buffer,
        const DynamicPtReadingUtils::NodeFlags nodeFlags, int *const nodeFlagsFieldPos) {
    return buffer->writeUintAndAdvancePosition(nodeFlags, NODE_FLAG_FIELD_SIZE, nodeFlagsFieldPos);
}

// Note that parentOffset is offset from node's head position.
/* static */ bool DynamicPtWritingUtils::writeParentPosOffsetAndAdvancePosition(
        BufferWithExtendableBuffer *const buffer, const int parentPos, const int basePos,
        int *const parentPosFieldPos) {
    return writeDictOffset(buffer, parentPos, basePos, parentPosFieldPos);
}

/* static */ bool DynamicPtWritingUtils::writeCodePointsAndAdvancePosition(
        BufferWithExtendableBuffer *const buffer, const int *const codePoints,
        const int codePointCount, int *const codePointFieldPos) {
    if (codePointCount <= 0) {
        AKLOGI("code points cannot be written because codePointCount is invalid: %d",
                codePointCount);
        ASSERT(false);
        return false;
    }
    const bool hasMultipleCodePoints = codePointCount > 1;
    return buffer->writeCodePointsAndAdvancePosition(codePoints, codePointCount,
            hasMultipleCodePoints, codePointFieldPos);
}

/* static */ bool DynamicPtWritingUtils::writeChildrenPositionAndAdvancePosition(
        BufferWithExtendableBuffer *const buffer, const int childrenPosition,
        int *const childrenPositionFieldPos) {
    return writeDictOffset(buffer, childrenPosition, (*childrenPositionFieldPos),
            childrenPositionFieldPos);
}

/* static */ bool DynamicPtWritingUtils::writeDictOffset(BufferWithExtendableBuffer *const buffer,
        const int targetPos, const int basePos, int *const offsetFieldPos) {
    int offset = targetPos - basePos;
    if (targetPos == NOT_A_DICT_POS) {
        offset = DynamicPtReadingUtils::DICT_OFFSET_INVALID;
    } else if (offset == 0) {
        offset = DynamicPtReadingUtils::DICT_OFFSET_ZERO_OFFSET;
    }
    if (offset > MAX_DICT_OFFSET_VALUE || offset < MIN_DICT_OFFSET_VALUE) {
        AKLOGI("offset cannot be written because the offset is too large or too small: %d",
                offset);
        ASSERT(false);
        return false;
    }
    uint32_t data = 0;
    if (offset >= 0) {
        data = offset;
    } else {
        data = abs(offset) | DICT_OFFSET_NEGATIVE_FLAG;
    }
    return buffer->writeUintAndAdvancePosition(data, DICT_OFFSET_FIELD_SIZE, offsetFieldPos);
}
}
