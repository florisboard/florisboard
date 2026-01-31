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

#ifndef LATINIME_VER4_SHORTCUT_LIST_POLICY_H
#define LATINIME_VER4_SHORTCUT_LIST_POLICY_H

#include "defines.h"
#include "dictionary/interface/dictionary_shortcuts_structure_policy.h"
#include "dictionary/structure/pt_common/shortcut/shortcut_list_reading_utils.h"
#include "dictionary/structure/v4/content/shortcut_dict_content.h"
#include "dictionary/structure/v4/content/terminal_position_lookup_table.h"

namespace latinime {

class Ver4ShortcutListPolicy : public DictionaryShortcutsStructurePolicy {
 public:
    Ver4ShortcutListPolicy(ShortcutDictContent *const shortcutDictContent,
            const TerminalPositionLookupTable *const terminalPositionLookupTable)
            : mShortcutDictContent(shortcutDictContent) {}

    ~Ver4ShortcutListPolicy() {}

    int getStartPos(const int pos) const {
        // The first shortcut entry is located at the head position of the shortcut list.
        return pos;
    }

    void getNextShortcut(const int maxCodePointCount, int *const outCodePoint,
            int *const outCodePointCount, bool *const outIsWhitelist, bool *const outHasNext,
            int *const pos) const {
        int probability = 0;
        mShortcutDictContent->getShortcutEntryAndAdvancePosition(maxCodePointCount,
                outCodePoint, outCodePointCount, &probability, outHasNext, pos);
        if (outIsWhitelist) {
            *outIsWhitelist = ShortcutListReadingUtils::isWhitelist(probability);
        }
    }

    void skipAllShortcuts(int *const pos) const {
        // Do nothing because we don't need to skip shortcut lists in ver4 dictionaries.
    }

    bool addNewShortcut(const int terminalId, const int *const codePoints, const int codePointCount,
            const int probability) {
        const int shortcutListPos = mShortcutDictContent->getShortcutListHeadPos(terminalId);
        if (shortcutListPos == NOT_A_DICT_POS) {
            // Create shortcut list.
            if (!mShortcutDictContent->createNewShortcutList(terminalId)) {
                AKLOGE("Cannot create new shortcut list. terminal id: %d", terminalId);
                return false;
            }
            const int writingPos =  mShortcutDictContent->getShortcutListHeadPos(terminalId);
            return mShortcutDictContent->writeShortcutEntry(codePoints, codePointCount, probability,
                    false /* hasNext */, writingPos);
        }
        const int entryPos = mShortcutDictContent->findShortcutEntryAndGetPos(shortcutListPos,
                codePoints, codePointCount);
        if (entryPos == NOT_A_DICT_POS) {
            // Add new entry to the shortcut list.
            // Create new shortcut list.
            if (!mShortcutDictContent->createNewShortcutList(terminalId)) {
                AKLOGE("Cannot create new shortcut list. terminal id: %d", terminalId);
                return false;
            }
            int writingPos =  mShortcutDictContent->getShortcutListHeadPos(terminalId);
            if (!mShortcutDictContent->writeShortcutEntryAndAdvancePosition(codePoints,
                    codePointCount, probability, true /* hasNext */, &writingPos)) {
                AKLOGE("Cannot write shortcut entry. terminal id: %d, pos: %d", terminalId,
                        writingPos);
                return false;
            }
            return mShortcutDictContent->copyShortcutList(shortcutListPos, writingPos);
        }
        // Overwrite existing entry.
        bool hasNext = false;
        mShortcutDictContent->getShortcutEntry(MAX_WORD_LENGTH, 0 /* outCodePoint */,
                0 /* outCodePointCount */ , 0 /* probability */, &hasNext, entryPos);
        if (!mShortcutDictContent->writeShortcutEntry(codePoints,
                codePointCount, probability, hasNext, entryPos)) {
            AKLOGE("Cannot overwrite shortcut entry. terminal id: %d, pos: %d", terminalId,
                    entryPos);
            return false;
        }
        return true;
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(Ver4ShortcutListPolicy);

    ShortcutDictContent *const mShortcutDictContent;
};
} // namespace latinime
#endif // LATINIME_VER4_SHORTCUT_LIST_POLICY_H
