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

#ifndef LATINIME_PATRICIA_TRIE_READING_UTILS_H
#define LATINIME_PATRICIA_TRIE_READING_UTILS_H

#include <cstdint>

#include "defines.h"

namespace latinime {

class DictionaryShortcutsStructurePolicy;
class DictionaryBigramsStructurePolicy;

class PatriciaTrieReadingUtils {
 public:
    typedef uint8_t NodeFlags;

    static int getPtNodeArraySizeAndAdvancePosition(const uint8_t *const buffer, int *const pos);

    static NodeFlags getFlagsAndAdvancePosition(const uint8_t *const buffer, int *const pos);

    static int getCodePointAndAdvancePosition(const uint8_t *const buffer,
            const int *const codePointTable, int *const pos);

    // Returns the number of read characters.
    static int getCharsAndAdvancePosition(const uint8_t *const buffer, const NodeFlags flags,
            const int maxLength, const int *const codePointTable, int *const outBuffer,
            int *const pos);

    // Returns the number of skipped characters.
    static int skipCharacters(const uint8_t *const buffer, const NodeFlags flags,
            const int maxLength, const int *const codePointTable, int *const pos);

    static int readProbabilityAndAdvancePosition(const uint8_t *const buffer, int *const pos);

    static int readChildrenPositionAndAdvancePosition(const uint8_t *const buffer,
            const NodeFlags flags, int *const pos);

    /**
     * Node Flags
     */
    static AK_FORCE_INLINE bool isPossiblyOffensive(const NodeFlags flags) {
        return (flags & FLAG_IS_POSSIBLY_OFFENSIVE) != 0;
    }

    static AK_FORCE_INLINE bool isNotAWord(const NodeFlags flags) {
        return (flags & FLAG_IS_NOT_A_WORD) != 0;
    }

    static AK_FORCE_INLINE bool isTerminal(const NodeFlags flags) {
        return (flags & FLAG_IS_TERMINAL) != 0;
    }

    static AK_FORCE_INLINE bool hasShortcutTargets(const NodeFlags flags) {
        return (flags & FLAG_HAS_SHORTCUT_TARGETS) != 0;
    }

    static AK_FORCE_INLINE bool hasBigrams(const NodeFlags flags) {
        return (flags & FLAG_HAS_BIGRAMS) != 0;
    }

    static AK_FORCE_INLINE bool hasMultipleChars(const NodeFlags flags) {
        return (flags & FLAG_HAS_MULTIPLE_CHARS) != 0;
    }

    static AK_FORCE_INLINE bool hasChildrenInFlags(const NodeFlags flags) {
        return FLAG_CHILDREN_POSITION_TYPE_NOPOSITION != (MASK_CHILDREN_POSITION_TYPE & flags);
    }

    static AK_FORCE_INLINE NodeFlags createAndGetFlags(const bool isPossiblyOffensive,
            const bool isNotAWord, const bool isTerminal, const bool hasShortcutTargets,
            const bool hasBigrams, const bool hasMultipleChars,
            const int childrenPositionFieldSize) {
        NodeFlags nodeFlags = 0;
        nodeFlags = isPossiblyOffensive ? (nodeFlags | FLAG_IS_POSSIBLY_OFFENSIVE) : nodeFlags;
        nodeFlags = isNotAWord ? (nodeFlags | FLAG_IS_NOT_A_WORD) : nodeFlags;
        nodeFlags = isTerminal ? (nodeFlags | FLAG_IS_TERMINAL) : nodeFlags;
        nodeFlags = hasShortcutTargets ? (nodeFlags | FLAG_HAS_SHORTCUT_TARGETS) : nodeFlags;
        nodeFlags = hasBigrams ? (nodeFlags | FLAG_HAS_BIGRAMS) : nodeFlags;
        nodeFlags = hasMultipleChars ? (nodeFlags | FLAG_HAS_MULTIPLE_CHARS) : nodeFlags;
        if (childrenPositionFieldSize == 1) {
            nodeFlags |= FLAG_CHILDREN_POSITION_TYPE_ONEBYTE;
        } else if (childrenPositionFieldSize == 2) {
            nodeFlags |= FLAG_CHILDREN_POSITION_TYPE_TWOBYTES;
        } else if (childrenPositionFieldSize == 3) {
            nodeFlags |= FLAG_CHILDREN_POSITION_TYPE_THREEBYTES;
        } else {
            nodeFlags |= FLAG_CHILDREN_POSITION_TYPE_NOPOSITION;
        }
        return nodeFlags;
    }

    static void readPtNodeInfo(const uint8_t *const dictBuf, const int ptNodePos,
            const DictionaryShortcutsStructurePolicy *const shortcutPolicy,
            const DictionaryBigramsStructurePolicy *const bigramPolicy,
            const int *const codePointTable, NodeFlags *const outFlags,
            int *const outCodePointCount, int *const outCodePoint, int *const outProbability,
            int *const outChildrenPos, int *const outShortcutPos, int *const outBigramPos,
            int *const outSiblingPos);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(PatriciaTrieReadingUtils);

    static const NodeFlags MASK_CHILDREN_POSITION_TYPE;
    static const NodeFlags FLAG_CHILDREN_POSITION_TYPE_NOPOSITION;
    static const NodeFlags FLAG_CHILDREN_POSITION_TYPE_ONEBYTE;
    static const NodeFlags FLAG_CHILDREN_POSITION_TYPE_TWOBYTES;
    static const NodeFlags FLAG_CHILDREN_POSITION_TYPE_THREEBYTES;

    static const NodeFlags FLAG_HAS_MULTIPLE_CHARS;
    static const NodeFlags FLAG_IS_TERMINAL;
    static const NodeFlags FLAG_HAS_SHORTCUT_TARGETS;
    static const NodeFlags FLAG_HAS_BIGRAMS;
    static const NodeFlags FLAG_IS_NOT_A_WORD;
    static const NodeFlags FLAG_IS_POSSIBLY_OFFENSIVE;
};
} // namespace latinime
#endif /* LATINIME_PATRICIA_TRIE_NODE_READING_UTILS_H */
