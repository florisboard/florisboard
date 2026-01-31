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

#include "dictionary/structure/v4/content/shortcut_dict_content.h"

#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

void ShortcutDictContent::getShortcutEntryAndAdvancePosition(const int maxCodePointCount,
        int *const outCodePoint, int *const outCodePointCount, int *const outProbability,
        bool *const outhasNext, int *const shortcutEntryPos) const {
    const BufferWithExtendableBuffer *const shortcutListBuffer = getContentBuffer();
    if (*shortcutEntryPos < 0 || *shortcutEntryPos >=  shortcutListBuffer->getTailPosition()) {
        AKLOGE("Invalid shortcut entry position. shortcutEntryPos: %d, bufSize: %d",
                *shortcutEntryPos, shortcutListBuffer->getTailPosition());
        ASSERT(false);
        if (outhasNext) {
            *outhasNext = false;
        }
        if (outCodePointCount) {
            *outCodePointCount = 0;
        }
        return;
    }

    const int shortcutFlags = shortcutListBuffer->readUintAndAdvancePosition(
            Ver4DictConstants::SHORTCUT_FLAGS_FIELD_SIZE, shortcutEntryPos);
    if (outProbability) {
        *outProbability = shortcutFlags & Ver4DictConstants::SHORTCUT_PROBABILITY_MASK;
    }
    if (outhasNext) {
        *outhasNext = shortcutFlags & Ver4DictConstants::SHORTCUT_HAS_NEXT_MASK;
    }
    if (outCodePoint && outCodePointCount) {
        shortcutListBuffer->readCodePointsAndAdvancePosition(
                maxCodePointCount, outCodePoint, outCodePointCount, shortcutEntryPos);
    }
}

int ShortcutDictContent::getShortcutListHeadPos(const int terminalId) const {
    const SparseTable *const addressLookupTable = getAddressLookupTable();
    if (!addressLookupTable->contains(terminalId)) {
        return NOT_A_DICT_POS;
    }
    return addressLookupTable->get(terminalId);
}

bool ShortcutDictContent::runGC(
        const TerminalPositionLookupTable::TerminalIdMap *const terminalIdMap,
        const ShortcutDictContent *const originalShortcutDictContent) {
   for (TerminalPositionLookupTable::TerminalIdMap::const_iterator it = terminalIdMap->begin();
           it != terminalIdMap->end(); ++it) {
       const int originalShortcutListPos =
               originalShortcutDictContent->getShortcutListHeadPos(it->first);
       if (originalShortcutListPos == NOT_A_DICT_POS) {
           continue;
       }
       const int shortcutListPos = getContentBuffer()->getTailPosition();
       // Copy shortcut list from original content.
       if (!copyShortcutListFromDictContent(originalShortcutListPos, originalShortcutDictContent,
               shortcutListPos)) {
           AKLOGE("Cannot copy shortcut list during GC. original pos: %d, pos: %d",
                   originalShortcutListPos, shortcutListPos);
           return false;
       }
       // Set shortcut list position to the lookup table.
       if (!getUpdatableAddressLookupTable()->set(it->second, shortcutListPos)) {
           AKLOGE("Cannot set shortcut list position. terminal id: %d, pos: %d",
                   it->second, shortcutListPos);
           return false;
       }
   }
   return true;
}

bool ShortcutDictContent::createNewShortcutList(const int terminalId) {
    const int shortcutListListPos = getContentBuffer()->getTailPosition();
    return getUpdatableAddressLookupTable()->set(terminalId, shortcutListListPos);
}

bool ShortcutDictContent::copyShortcutList(const int shortcutListPos, const int toPos) {
    return copyShortcutListFromDictContent(shortcutListPos, this, toPos);
}

bool ShortcutDictContent::copyShortcutListFromDictContent(const int shortcutListPos,
        const ShortcutDictContent *const sourceShortcutDictContent, const int toPos) {
    bool hasNext = true;
    int readingPos = shortcutListPos;
    int writingPos = toPos;
    int codePoints[MAX_WORD_LENGTH];
    while (hasNext) {
        int probability = 0;
        int codePointCount = 0;
        sourceShortcutDictContent->getShortcutEntryAndAdvancePosition(MAX_WORD_LENGTH,
                codePoints, &codePointCount, &probability, &hasNext, &readingPos);
        if (!writeShortcutEntryAndAdvancePosition(codePoints, codePointCount, probability,
                hasNext, &writingPos)) {
            AKLOGE("Cannot write shortcut entry to copy. pos: %d", writingPos);
            return false;
        }
    }
    return true;
}

bool ShortcutDictContent::setProbability(const int probability, const int shortcutEntryPos) {
    BufferWithExtendableBuffer *const shortcutListBuffer = getWritableContentBuffer();
    const int shortcutFlags = shortcutListBuffer->readUint(
            Ver4DictConstants::SHORTCUT_FLAGS_FIELD_SIZE, shortcutEntryPos);
    const bool hasNext = shortcutFlags & Ver4DictConstants::SHORTCUT_HAS_NEXT_MASK;
    const int shortcutFlagsToWrite = createAndGetShortcutFlags(probability, hasNext);
    return shortcutListBuffer->writeUint(shortcutFlagsToWrite,
            Ver4DictConstants::SHORTCUT_FLAGS_FIELD_SIZE, shortcutEntryPos);
}

bool ShortcutDictContent::writeShortcutEntryAndAdvancePosition(const int *const codePoint,
        const int codePointCount, const int probability, const bool hasNext,
        int *const shortcutEntryPos) {
    BufferWithExtendableBuffer *const shortcutListBuffer = getWritableContentBuffer();
    const int shortcutFlags = createAndGetShortcutFlags(probability, hasNext);
    if (!shortcutListBuffer->writeUintAndAdvancePosition(shortcutFlags,
            Ver4DictConstants::SHORTCUT_FLAGS_FIELD_SIZE, shortcutEntryPos)) {
        AKLOGE("Cannot write shortcut flags. flags; %x, pos: %d", shortcutFlags, *shortcutEntryPos);
        return false;
    }
    if (!shortcutListBuffer->writeCodePointsAndAdvancePosition(codePoint, codePointCount,
            true /* writesTerminator */, shortcutEntryPos)) {
        AKLOGE("Cannot write shortcut target code points. pos: %d", *shortcutEntryPos);
        return false;
    }
    return true;
}

// Find a shortcut entry that has specified target and return its position.
int ShortcutDictContent::findShortcutEntryAndGetPos(const int shortcutListPos,
        const int *const targetCodePointsToFind, const int codePointCount) const {
    bool hasNext = true;
    int readingPos = shortcutListPos;
    int targetCodePoints[MAX_WORD_LENGTH];
    while (hasNext) {
        const int entryPos = readingPos;
        int probability = 0;
        int targetCodePointCount = 0;
        getShortcutEntryAndAdvancePosition(MAX_WORD_LENGTH, targetCodePoints, &targetCodePointCount,
                &probability, &hasNext, &readingPos);
        if (targetCodePointCount != codePointCount) {
            continue;
        }
        bool matched = true;
        for (int i = 0; i < codePointCount; ++i) {
            if (targetCodePointsToFind[i] != targetCodePoints[i]) {
                matched = false;
                break;
            }
        }
        if (matched) {
            return entryPos;
        }
    }
    return NOT_A_DICT_POS;
}

int ShortcutDictContent::createAndGetShortcutFlags(const int probability,
        const bool hasNext) const {
    return (probability & Ver4DictConstants::SHORTCUT_PROBABILITY_MASK)
            | (hasNext ? Ver4DictConstants::SHORTCUT_HAS_NEXT_MASK : 0);
}

} // namespace latinime
