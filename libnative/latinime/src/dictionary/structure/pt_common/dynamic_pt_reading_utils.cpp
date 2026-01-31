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

#include "dictionary/structure/pt_common/dynamic_pt_reading_utils.h"

#include "defines.h"
#include "dictionary/utils/byte_array_utils.h"

namespace latinime {

const DynamicPtReadingUtils::NodeFlags DynamicPtReadingUtils::MASK_MOVED = 0xC0;
const DynamicPtReadingUtils::NodeFlags DynamicPtReadingUtils::FLAG_IS_NOT_MOVED = 0xC0;
const DynamicPtReadingUtils::NodeFlags DynamicPtReadingUtils::FLAG_IS_MOVED = 0x40;
const DynamicPtReadingUtils::NodeFlags DynamicPtReadingUtils::FLAG_IS_DELETED = 0x80;
const DynamicPtReadingUtils::NodeFlags DynamicPtReadingUtils::FLAG_WILL_BECOME_NON_TERMINAL = 0x00;

// TODO: Make DICT_OFFSET_ZERO_OFFSET = 0.
// Currently, DICT_OFFSET_INVALID is 0 in Java side but offset can be 0 during GC. So, the maximum
// value of offsets, which is 0x7FFFFF is used to represent 0 offset.
const int DynamicPtReadingUtils::DICT_OFFSET_INVALID = 0;
const int DynamicPtReadingUtils::DICT_OFFSET_ZERO_OFFSET = 0x7FFFFF;

/* static */ int DynamicPtReadingUtils::getForwardLinkPosition(const uint8_t *const buffer,
        const int pos) {
    int linkAddressPos = pos;
    return ByteArrayUtils::readSint24AndAdvancePosition(buffer, &linkAddressPos);
}

/* static */ int DynamicPtReadingUtils::getParentPtNodePosOffsetAndAdvancePosition(
        const uint8_t *const buffer, int *const pos) {
    return ByteArrayUtils::readSint24AndAdvancePosition(buffer, pos);
}

/* static */ int DynamicPtReadingUtils::getParentPtNodePos(const int parentOffset,
        const int ptNodePos) {
    if (parentOffset == DICT_OFFSET_INVALID) {
        return NOT_A_DICT_POS;
    } else if (parentOffset == DICT_OFFSET_ZERO_OFFSET) {
        return ptNodePos;
    } else {
        return parentOffset + ptNodePos;
    }
}

/* static */ int DynamicPtReadingUtils::readChildrenPositionAndAdvancePosition(
        const uint8_t *const buffer, int *const pos) {
    const int base = *pos;
    const int offset = ByteArrayUtils::readSint24AndAdvancePosition(buffer, pos);
    if (offset == DICT_OFFSET_INVALID) {
        // The PtNode does not have children.
        return NOT_A_DICT_POS;
    } else if (offset == DICT_OFFSET_ZERO_OFFSET) {
        return base;
    } else {
        return base + offset;
    }
}

} // namespace latinime
