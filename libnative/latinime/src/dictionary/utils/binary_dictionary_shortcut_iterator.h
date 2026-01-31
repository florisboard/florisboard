/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef LATINIME_BINARY_DICTIONARY_SHORTCUT_ITERATOR_H
#define LATINIME_BINARY_DICTIONARY_SHORTCUT_ITERATOR_H

#include "defines.h"
#include "dictionary/interface/dictionary_shortcuts_structure_policy.h"

namespace latinime {

class BinaryDictionaryShortcutIterator {
 public:
    BinaryDictionaryShortcutIterator(
            const DictionaryShortcutsStructurePolicy *const shortcutStructurePolicy,
            const int shortcutPos)
            : mShortcutStructurePolicy(shortcutStructurePolicy),
              mPos(shortcutStructurePolicy->getStartPos(shortcutPos)),
              mHasNextShortcutTarget(shortcutPos != NOT_A_DICT_POS) {}

    BinaryDictionaryShortcutIterator(const BinaryDictionaryShortcutIterator &&shortcutIterator) noexcept
            : mShortcutStructurePolicy(shortcutIterator.mShortcutStructurePolicy),
              mPos(shortcutIterator.mPos),
              mHasNextShortcutTarget(shortcutIterator.mHasNextShortcutTarget) {}

    AK_FORCE_INLINE bool hasNextShortcutTarget() const {
        return mHasNextShortcutTarget;
    }

    // Gets the shortcut target itself as an int string and put it to outTarget, put its length
    // to outTargetLength, put whether it is allowlist to outIsAllowed.
    AK_FORCE_INLINE void nextShortcutTarget(
            const int maxDepth, int *const outTarget, int *const outTargetLength,
            bool *const outIsAllowed) {
        mShortcutStructurePolicy->getNextShortcut(maxDepth, outTarget, outTargetLength,
                outIsAllowed, &mHasNextShortcutTarget, &mPos);
    }

 private:
    DISALLOW_DEFAULT_CONSTRUCTOR(BinaryDictionaryShortcutIterator);
    DISALLOW_ASSIGNMENT_OPERATOR(BinaryDictionaryShortcutIterator);

    const DictionaryShortcutsStructurePolicy *const mShortcutStructurePolicy;
    int mPos;
    bool mHasNextShortcutTarget;
};
} // namespace latinime
#endif // LATINIME_BINARY_DICTIONARY_SHORTCUT_ITERATOR_H
