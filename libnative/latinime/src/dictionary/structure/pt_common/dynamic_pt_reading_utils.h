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

#ifndef LATINIME_DYNAMIC_PT_READING_UTILS_H
#define LATINIME_DYNAMIC_PT_READING_UTILS_H

#include <cstdint>

#include "defines.h"

namespace latinime {

class DynamicPtReadingUtils {
 public:
    typedef uint8_t NodeFlags;

    static const int DICT_OFFSET_INVALID;
    static const int DICT_OFFSET_ZERO_OFFSET;

    static int getForwardLinkPosition(const uint8_t *const buffer, const int pos);

    static AK_FORCE_INLINE bool isValidForwardLinkPosition(const int forwardLinkAddress) {
        return forwardLinkAddress != 0;
    }

    static int getParentPtNodePosOffsetAndAdvancePosition(const uint8_t *const buffer,
            int *const pos);

    static int getParentPtNodePos(const int parentOffset, const int ptNodePos);

    static int readChildrenPositionAndAdvancePosition(const uint8_t *const buffer, int *const pos);

    /**
     * Node Flags
     */
    static AK_FORCE_INLINE bool isMoved(const NodeFlags flags) {
        return FLAG_IS_MOVED == (MASK_MOVED & flags);
    }

    static AK_FORCE_INLINE bool isDeleted(const NodeFlags flags) {
        return FLAG_IS_DELETED == (MASK_MOVED & flags);
    }

    static AK_FORCE_INLINE bool willBecomeNonTerminal(const NodeFlags flags) {
        return FLAG_WILL_BECOME_NON_TERMINAL == (MASK_MOVED & flags);
    }

    static AK_FORCE_INLINE NodeFlags updateAndGetFlags(const NodeFlags originalFlags,
            const bool isMoved, const bool isDeleted, const bool willBecomeNonTerminal) {
        NodeFlags flags = originalFlags;
        flags = willBecomeNonTerminal ?
                ((flags & (~MASK_MOVED)) | FLAG_WILL_BECOME_NON_TERMINAL) : flags;
        flags = isMoved ? ((flags & (~MASK_MOVED)) | FLAG_IS_MOVED) : flags;
        flags = isDeleted ? ((flags & (~MASK_MOVED)) | FLAG_IS_DELETED) : flags;
        flags = (!isMoved && !isDeleted && !willBecomeNonTerminal) ?
                ((flags & (~MASK_MOVED)) | FLAG_IS_NOT_MOVED) : flags;
        return flags;
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(DynamicPtReadingUtils);

    static const NodeFlags MASK_MOVED;
    static const NodeFlags FLAG_IS_NOT_MOVED;
    static const NodeFlags FLAG_IS_MOVED;
    static const NodeFlags FLAG_IS_DELETED;
    static const NodeFlags FLAG_WILL_BECOME_NON_TERMINAL;
};
} // namespace latinime
#endif /* LATINIME_DYNAMIC_PT_READING_UTILS_H */
