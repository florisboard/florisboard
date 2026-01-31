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

#ifndef LATINIME_DICTIONARY_SHORTCUTS_STRUCTURE_POLICY_H
#define LATINIME_DICTIONARY_SHORTCUTS_STRUCTURE_POLICY_H

#include "defines.h"

namespace latinime {

/*
 * This class abstracts structure of shortcuts.
 */
class DictionaryShortcutsStructurePolicy {
 public:
    virtual ~DictionaryShortcutsStructurePolicy() {}

    virtual int getStartPos(const int pos) const = 0;

    virtual void getNextShortcut(const int maxCodePointCount, int *const outCodePoint,
            int *const outCodePointCount, bool *const outIsWhitelist, bool *const outHasNext,
            int *const pos) const = 0;

    virtual void skipAllShortcuts(int *const pos) const = 0;

 protected:
    DictionaryShortcutsStructurePolicy() {}

 private:
    DISALLOW_COPY_AND_ASSIGN(DictionaryShortcutsStructurePolicy);
};
} // namespace latinime
#endif /* LATINIME_DICTIONARY_SHORTCUTS_STRUCTURE_POLICY_H */
