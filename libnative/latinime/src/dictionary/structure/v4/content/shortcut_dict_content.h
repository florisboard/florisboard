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

#ifndef LATINIME_SHORTCUT_DICT_CONTENT_H
#define LATINIME_SHORTCUT_DICT_CONTENT_H

#include <cstdio>

#include "defines.h"
#include "dictionary/structure/v4/content/sparse_table_dict_content.h"
#include "dictionary/structure/v4/content/terminal_position_lookup_table.h"
#include "dictionary/structure/v4/ver4_dict_constants.h"

namespace latinime {

class ReadWriteByteArrayView;

class ShortcutDictContent : public SparseTableDictContent {
 public:
    ShortcutDictContent(const ReadWriteByteArrayView *const buffers)
            : SparseTableDictContent(buffers, Ver4DictConstants::SHORTCUT_ADDRESS_TABLE_BLOCK_SIZE,
                      Ver4DictConstants::SHORTCUT_ADDRESS_TABLE_DATA_SIZE) {}

    ShortcutDictContent()
            : SparseTableDictContent(Ver4DictConstants::SHORTCUT_ADDRESS_TABLE_BLOCK_SIZE,
                      Ver4DictConstants::SHORTCUT_ADDRESS_TABLE_DATA_SIZE) {}

    void getShortcutEntry(const int maxCodePointCount, int *const outCodePoint,
            int *const outCodePointCount, int *const outProbability, bool *const outhasNext,
            const int shortcutEntryPos) {
        int readingPos = shortcutEntryPos;
        return getShortcutEntryAndAdvancePosition(maxCodePointCount, outCodePoint,
                outCodePointCount, outProbability, outhasNext, &readingPos);
    }

    void getShortcutEntryAndAdvancePosition(const int maxCodePointCount,
            int *const outCodePoint, int *const outCodePointCount, int *const outProbability,
            bool *const outhasNext, int *const shortcutEntryPos) const;

   // Returns head position of shortcut list for a PtNode specified by terminalId.
   int getShortcutListHeadPos(const int terminalId) const;

   bool flushToFile(FILE *const file) const {
       return flush(file);
   }

   bool runGC(const TerminalPositionLookupTable::TerminalIdMap *const terminalIdMap,
           const ShortcutDictContent *const originalShortcutDictContent);

   bool createNewShortcutList(const int terminalId);

   bool copyShortcutList(const int shortcutListPos, const int toPos);

   bool setProbability(const int probability, const int shortcutEntryPos);

   bool writeShortcutEntry(const int *const codePoint, const int codePointCount,
           const int probability, const bool hasNext, const int shortcutEntryPos) {
       int writingPos = shortcutEntryPos;
       return writeShortcutEntryAndAdvancePosition(codePoint, codePointCount, probability,
               hasNext, &writingPos);
   }

   bool writeShortcutEntryAndAdvancePosition(const int *const codePoint,
           const int codePointCount, const int probability, const bool hasNext,
           int *const shortcutEntryPos);

   int findShortcutEntryAndGetPos(const int shortcutListPos,
           const int *const targetCodePointsToFind, const int codePointCount) const;

 private:
    DISALLOW_COPY_AND_ASSIGN(ShortcutDictContent);

    bool copyShortcutListFromDictContent(const int shortcutListPos,
            const ShortcutDictContent *const sourceShortcutDictContent, const int toPos);

    int createAndGetShortcutFlags(const int probability, const bool hasNext) const;
};
} // namespace latinime
#endif /* LATINIME_SHORTCUT_DICT_CONTENT_H */
