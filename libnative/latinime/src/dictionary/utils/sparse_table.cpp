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

#include "dictionary/utils/sparse_table.h"

namespace latinime {

const int SparseTable::NOT_EXIST = -1;
const int SparseTable::INDEX_SIZE = 4;

bool SparseTable::contains(const int id) const {
    const int readingPos = getPosInIndexTable(id);
    if (id < 0 || mIndexTableBuffer->getTailPosition() <= readingPos) {
        return false;
    }
    const int index = mIndexTableBuffer->readUint(INDEX_SIZE, readingPos);
    return index != NOT_EXIST;
}

uint32_t SparseTable::get(const int id) const {
    const int indexTableReadingPos = getPosInIndexTable(id);
    const int index = mIndexTableBuffer->readUint(INDEX_SIZE, indexTableReadingPos);
    const int contentTableReadingPos = getPosInContentTable(id, index);
    if (contentTableReadingPos < 0
            || contentTableReadingPos >= mContentTableBuffer->getTailPosition()) {
        AKLOGE("contentTableReadingPos(%d) is invalid. id: %d, index: %d",
                contentTableReadingPos, id, index);
        return NOT_A_DICT_POS;
    }
    const int contentValue = mContentTableBuffer->readUint(mDataSize, contentTableReadingPos);
    return contentValue == NOT_EXIST ? NOT_A_DICT_POS : contentValue;
}

bool SparseTable::set(const int id, const uint32_t value) {
    const int posInIndexTable = getPosInIndexTable(id);
    // Extends the index table if needed.
    int tailPos = mIndexTableBuffer->getTailPosition();
    while (tailPos <= posInIndexTable) {
        if (!mIndexTableBuffer->writeUintAndAdvancePosition(NOT_EXIST, INDEX_SIZE, &tailPos)) {
            AKLOGE("cannot extend index table. tailPos: %d to: %d", tailPos, posInIndexTable);
            return false;
        }
    }
    if (contains(id)) {
        // The entry is already in the content table.
        const int index = mIndexTableBuffer->readUint(INDEX_SIZE, posInIndexTable);
        if (!mContentTableBuffer->writeUint(value, mDataSize, getPosInContentTable(id, index))) {
            AKLOGE("cannot update value %d. pos: %d, tailPos: %d, mDataSize: %d", value,
                    getPosInContentTable(id, index), mContentTableBuffer->getTailPosition(),
                    mDataSize);
            return false;
        }
        return true;
    }
    // The entry is not in the content table.
    // Create new entry in the content table.
    const int index = getIndexFromContentTablePos(mContentTableBuffer->getTailPosition());
    if (!mIndexTableBuffer->writeUint(index, INDEX_SIZE, posInIndexTable)) {
        AKLOGE("cannot write index %d. pos %d", index, posInIndexTable);
        return false;
    }
    // Write a new block that containing the entry to be set.
    int writingPos = getPosInContentTable(0 /* id */, index);
    for (int i = 0; i < mBlockSize; ++i) {
        if (!mContentTableBuffer->writeUintAndAdvancePosition(NOT_EXIST, mDataSize,
                &writingPos)) {
            AKLOGE("cannot write content table to extend. writingPos: %d, tailPos: %d, "
                    "mDataSize: %d", writingPos, mContentTableBuffer->getTailPosition(), mDataSize);
            return false;
        }
    }
    return mContentTableBuffer->writeUint(value, mDataSize, getPosInContentTable(id, index));
}

int SparseTable::getIndexFromContentTablePos(const int contentTablePos) const {
    return contentTablePos / mDataSize / mBlockSize;
}

int SparseTable::getPosInIndexTable(const int id) const {
    return (id / mBlockSize) * INDEX_SIZE;
}

int SparseTable::getPosInContentTable(const int id, const int index) const {
    const int offset = id % mBlockSize;
    return (index * mBlockSize + offset) * mDataSize;
}

} // namespace latinime
