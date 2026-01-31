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

#include "dictionary/structure/pt_common/patricia_trie_reading_utils.h"

#include "defines.h"
#include "dictionary/interface/dictionary_bigrams_structure_policy.h"
#include "dictionary/interface/dictionary_shortcuts_structure_policy.h"
#include "dictionary/utils/byte_array_utils.h"

namespace latinime {

typedef PatriciaTrieReadingUtils PtReadingUtils;

const PtReadingUtils::NodeFlags PtReadingUtils::MASK_CHILDREN_POSITION_TYPE = 0xC0;
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_CHILDREN_POSITION_TYPE_NOPOSITION = 0x00;
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_CHILDREN_POSITION_TYPE_ONEBYTE = 0x40;
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_CHILDREN_POSITION_TYPE_TWOBYTES = 0x80;
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_CHILDREN_POSITION_TYPE_THREEBYTES = 0xC0;

// Flag for single/multiple char group
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_HAS_MULTIPLE_CHARS = 0x20;
// Flag for terminal PtNodes
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_IS_TERMINAL = 0x10;
// Flag for shortcut targets presence
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_HAS_SHORTCUT_TARGETS = 0x08;
// Flag for bigram presence
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_HAS_BIGRAMS = 0x04;
// Flag for non-words (typically, shortcut only entries)
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_IS_NOT_A_WORD = 0x02;
// Flag for possibly offensive words
const PtReadingUtils::NodeFlags PtReadingUtils::FLAG_IS_POSSIBLY_OFFENSIVE = 0x01;

/* static */ int PtReadingUtils::getPtNodeArraySizeAndAdvancePosition(
        const uint8_t *const buffer, int *const pos) {
    const uint8_t firstByte = ByteArrayUtils::readUint8AndAdvancePosition(buffer, pos);
    if (firstByte < 0x80) {
        return firstByte;
    } else {
        return ((firstByte & 0x7F) << 8) ^ ByteArrayUtils::readUint8AndAdvancePosition(
                buffer, pos);
    }
}

/* static */ PtReadingUtils::NodeFlags PtReadingUtils::getFlagsAndAdvancePosition(
        const uint8_t *const buffer, int *const pos) {
    return ByteArrayUtils::readUint8AndAdvancePosition(buffer, pos);
}

/* static */ int PtReadingUtils::getCodePointAndAdvancePosition(const uint8_t *const buffer,
        const int *const codePointTable, int *const pos) {
    return ByteArrayUtils::readCodePointAndAdvancePosition(buffer, codePointTable, pos);
}

// Returns the number of read characters.
/* static */ int PtReadingUtils::getCharsAndAdvancePosition(const uint8_t *const buffer,
        const NodeFlags flags, const int maxLength, const int *const codePointTable,
        int *const outBuffer, int *const pos) {
    int length = 0;
    if (hasMultipleChars(flags)) {
        length = ByteArrayUtils::readStringAndAdvancePosition(buffer, maxLength, codePointTable,
                outBuffer, pos);
    } else {
        const int codePoint = getCodePointAndAdvancePosition(buffer, codePointTable, pos);
        if (codePoint == NOT_A_CODE_POINT) {
            // CAVEAT: codePoint == NOT_A_CODE_POINT means the code point is
            // CHARACTER_ARRAY_TERMINATOR. The code point must not be CHARACTER_ARRAY_TERMINATOR
            // when the PtNode has a single code point.
            length = 0;
            AKLOGE("codePoint is NOT_A_CODE_POINT. pos: %d, codePoint: 0x%x, buffer[pos - 1]: 0x%x",
                    *pos - 1, codePoint, buffer[*pos - 1]);
            ASSERT(false);
        } else if (maxLength > 0) {
            outBuffer[0] = codePoint;
            length = 1;
        }
    }
    return length;
}

// Returns the number of skipped characters.
/* static */ int PtReadingUtils::skipCharacters(const uint8_t *const buffer, const NodeFlags flags,
        const int maxLength, const int *const codePointTable, int *const pos) {
    if (hasMultipleChars(flags)) {
        return ByteArrayUtils::advancePositionToBehindString(buffer, maxLength, pos);
    } else {
        if (maxLength > 0) {
            getCodePointAndAdvancePosition(buffer, codePointTable, pos);
            return 1;
        } else {
            return 0;
        }
    }
}

/* static */ int PtReadingUtils::readProbabilityAndAdvancePosition(const uint8_t *const buffer,
        int *const pos) {
    return ByteArrayUtils::readUint8AndAdvancePosition(buffer, pos);
}

/* static */ int PtReadingUtils::readChildrenPositionAndAdvancePosition(
        const uint8_t *const buffer, const NodeFlags flags, int *const pos) {
    const int base = *pos;
    int offset = 0;
    switch (MASK_CHILDREN_POSITION_TYPE & flags) {
        case FLAG_CHILDREN_POSITION_TYPE_ONEBYTE:
            offset = ByteArrayUtils::readUint8AndAdvancePosition(buffer, pos);
            break;
        case FLAG_CHILDREN_POSITION_TYPE_TWOBYTES:
            offset = ByteArrayUtils::readUint16AndAdvancePosition(buffer, pos);
            break;
        case FLAG_CHILDREN_POSITION_TYPE_THREEBYTES:
            offset = ByteArrayUtils::readUint24AndAdvancePosition(buffer, pos);
            break;
        default:
            // If we come here, it means we asked for the children of a word with
            // no children.
            return NOT_A_DICT_POS;
    }
    return base + offset;
}

/* static */ void PtReadingUtils::readPtNodeInfo(const uint8_t *const dictBuf, const int ptNodePos,
        const DictionaryShortcutsStructurePolicy *const shortcutPolicy,
        const DictionaryBigramsStructurePolicy *const bigramPolicy, const int *const codePointTable,
        NodeFlags *const outFlags, int *const outCodePointCount, int *const outCodePoint,
        int *const outProbability, int *const outChildrenPos, int *const outShortcutPos,
        int *const outBigramPos, int *const outSiblingPos) {
    int readingPos = ptNodePos;
    const NodeFlags flags = getFlagsAndAdvancePosition(dictBuf, &readingPos);
    *outFlags = flags;
    *outCodePointCount = getCharsAndAdvancePosition(
            dictBuf, flags, MAX_WORD_LENGTH, codePointTable, outCodePoint, &readingPos);
    *outProbability = isTerminal(flags) ?
            readProbabilityAndAdvancePosition(dictBuf, &readingPos) : NOT_A_PROBABILITY;
    *outChildrenPos = hasChildrenInFlags(flags) ?
            readChildrenPositionAndAdvancePosition(dictBuf, flags, &readingPos) : NOT_A_DICT_POS;
    *outShortcutPos = NOT_A_DICT_POS;
    if (hasShortcutTargets(flags)) {
        *outShortcutPos = readingPos;
        shortcutPolicy->skipAllShortcuts(&readingPos);
    }
    *outBigramPos = NOT_A_DICT_POS;
    if (hasBigrams(flags)) {
        *outBigramPos = readingPos;
        bigramPolicy->skipAllBigrams(&readingPos);
    }
    *outSiblingPos = readingPos;
}

} // namespace latinime
