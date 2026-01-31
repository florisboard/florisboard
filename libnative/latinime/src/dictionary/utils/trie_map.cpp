/*
 * Copyright (C) 2014, The Android Open Source Project
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

#include "dictionary/utils/trie_map.h"

#include "dictionary/utils/dict_file_writing_utils.h"

namespace latinime {

const int TrieMap::INVALID_INDEX = -1;
const int TrieMap::FIELD0_SIZE = 4;
const int TrieMap::FIELD1_SIZE = 3;
const int TrieMap::ENTRY_SIZE = FIELD0_SIZE + FIELD1_SIZE;
const uint32_t TrieMap::VALUE_FLAG = 0x400000;
const uint32_t TrieMap::VALUE_MASK = 0x3FFFFF;
const uint32_t TrieMap::INVALID_VALUE_IN_KEY_VALUE_ENTRY = VALUE_MASK;
const uint32_t TrieMap::TERMINAL_LINK_FLAG = 0x800000;
const uint32_t TrieMap::TERMINAL_LINK_MASK = 0x7FFFFF;
const int TrieMap::NUM_OF_BITS_USED_FOR_ONE_LEVEL = 5;
const uint32_t TrieMap::LABEL_MASK = 0x1F;
const int TrieMap::MAX_NUM_OF_ENTRIES_IN_ONE_LEVEL = 1 << NUM_OF_BITS_USED_FOR_ONE_LEVEL;
const int TrieMap::ROOT_BITMAP_ENTRY_INDEX = 0;
const int TrieMap::ROOT_BITMAP_ENTRY_POS = MAX_NUM_OF_ENTRIES_IN_ONE_LEVEL * FIELD0_SIZE;
const TrieMap::Entry TrieMap::EMPTY_BITMAP_ENTRY = TrieMap::Entry(0, 0);
const int TrieMap::TERMINAL_LINKED_ENTRY_COUNT = 2; // Value entry and bitmap entry.
const uint64_t TrieMap::MAX_VALUE =
        (static_cast<uint64_t>(1) << ((FIELD0_SIZE + FIELD1_SIZE) * CHAR_BIT)) - 1;
const int TrieMap::MAX_BUFFER_SIZE = TERMINAL_LINK_MASK * ENTRY_SIZE;

TrieMap::TrieMap() : mBuffer(MAX_BUFFER_SIZE) {
    mBuffer.extend(ROOT_BITMAP_ENTRY_POS);
    writeEntry(EMPTY_BITMAP_ENTRY, ROOT_BITMAP_ENTRY_INDEX);
}

TrieMap::TrieMap(const ReadWriteByteArrayView buffer)
        : mBuffer(buffer, BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE) {}

void TrieMap::dump(const int from, const int to) const {
    AKLOGI("BufSize: %d", mBuffer.getTailPosition());
    for (int i = from; i < to; ++i) {
        AKLOGI("Entry[%d]: %x, %x", i, readField0(i), readField1(i));
    }
    [[maybe_unused]] int unusedRegionSize = 0;
    for (int i = 1; i <= MAX_NUM_OF_ENTRIES_IN_ONE_LEVEL; ++i) {
        int index = readEmptyTableLink(i);
        while (index != ROOT_BITMAP_ENTRY_INDEX) {
            index = readField0(index);
            unusedRegionSize += i;
        }
    }
    AKLOGI("Unused Size: %d", unusedRegionSize);
}

int TrieMap::getNextLevelBitmapEntryIndex(const int key, const int bitmapEntryIndex) {
    const Entry bitmapEntry = readEntry(bitmapEntryIndex);
    const uint32_t unsignedKey = static_cast<uint32_t>(key);
    const int terminalEntryIndex = getTerminalEntryIndex(
            unsignedKey, getBitShuffledKey(unsignedKey), bitmapEntry, 0 /* level */);
    if (terminalEntryIndex == INVALID_INDEX) {
        // Not found.
        return INVALID_INDEX;
    }
    const Entry terminalEntry = readEntry(terminalEntryIndex);
    if (terminalEntry.hasTerminalLink()) {
        return terminalEntry.getValueEntryIndex() + 1;
    }
    // Create a value entry and a bitmap entry.
    const int valueEntryIndex = allocateTable(TERMINAL_LINKED_ENTRY_COUNT);
    if (valueEntryIndex == INVALID_INDEX) {
        return INVALID_INDEX;
    }
    if (!writeEntry(Entry(0, terminalEntry.getValue()), valueEntryIndex)) {
        return INVALID_INDEX;
    }
    if (!writeEntry(EMPTY_BITMAP_ENTRY, valueEntryIndex + 1)) {
        return INVALID_INDEX;
    }
    if (!writeField1(valueEntryIndex | TERMINAL_LINK_FLAG, terminalEntryIndex)) {
        return INVALID_INDEX;
    }
    return valueEntryIndex + 1;
}

const TrieMap::Result TrieMap::get(const int key, const int bitmapEntryIndex) const {
    const uint32_t unsignedKey = static_cast<uint32_t>(key);
    return getInternal(unsignedKey, getBitShuffledKey(unsignedKey), bitmapEntryIndex,
            0 /* level */);
}

bool TrieMap::put(const int key, const uint64_t value, const int bitmapEntryIndex) {
    if (value > MAX_VALUE) {
        return false;
    }
    const uint32_t unsignedKey = static_cast<uint32_t>(key);
    return putInternal(unsignedKey, value, getBitShuffledKey(unsignedKey), bitmapEntryIndex,
            readEntry(bitmapEntryIndex), 0 /* level */);
}

bool TrieMap::save(FILE *const file) const {
    return DictFileWritingUtils::writeBufferToFileTail(file, &mBuffer);
}

bool TrieMap::remove(const int key, const int bitmapEntryIndex) {
    const Entry bitmapEntry = readEntry(bitmapEntryIndex);
    const uint32_t unsignedKey = static_cast<uint32_t>(key);
    const int terminalEntryIndex = getTerminalEntryIndex(
            unsignedKey, getBitShuffledKey(unsignedKey), bitmapEntry, 0 /* level */);
    if (terminalEntryIndex == INVALID_INDEX) {
        // Not found.
        return false;
    }
    const Entry terminalEntry = readEntry(terminalEntryIndex);
    if (!writeField1(VALUE_FLAG ^ INVALID_VALUE_IN_KEY_VALUE_ENTRY , terminalEntryIndex)) {
        return false;
    }
    if (terminalEntry.hasTerminalLink()) {
        const Entry nextLevelBitmapEntry = readEntry(terminalEntry.getValueEntryIndex() + 1);
        if (!freeTable(terminalEntry.getValueEntryIndex(), TERMINAL_LINKED_ENTRY_COUNT)) {
            return false;
        }
        if (!removeInner(nextLevelBitmapEntry)){
            return false;
        }
    }
    return true;
}

/**
 * Iterate next entry in a certain level.
 *
 * @param iterationState the iteration state that will be read and updated in this method.
 * @param outKey the output key
 * @return Result instance. mIsValid is false when all entries are iterated.
 */
const TrieMap::Result TrieMap::iterateNext(std::vector<TableIterationState> *const iterationState,
        int *const outKey) const {
    while (!iterationState->empty()) {
        TableIterationState &state = iterationState->back();
        if (state.mTableSize <= state.mCurrentIndex) {
            // Move to parent.
            iterationState->pop_back();
        } else {
            const int entryIndex = state.mTableIndex + state.mCurrentIndex;
            state.mCurrentIndex += 1;
            const Entry entry = readEntry(entryIndex);
            if (entry.isBitmapEntry()) {
                // Move to child.
                iterationState->emplace_back(popCount(entry.getBitmap()), entry.getTableIndex());
            } else if (entry.isValidTerminalEntry()) {
                if (outKey) {
                    *outKey = entry.getKey();
                }
                if (!entry.hasTerminalLink()) {
                    return Result(entry.getValue(), true, INVALID_INDEX);
                }
                const int valueEntryIndex = entry.getValueEntryIndex();
                const Entry valueEntry = readEntry(valueEntryIndex);
                return Result(valueEntry.getValueOfValueEntry(), true, valueEntryIndex + 1);
            }
        }
    }
    // Visited all entries.
    return Result(0, false, INVALID_INDEX);
}

/**
 * Shuffle bits of the key in the fixed order.
 *
 * This method is used as a hash function. This returns different values for different inputs.
 */
uint32_t TrieMap::getBitShuffledKey(const uint32_t key) const {
    uint32_t shuffledKey = 0;
    for (int i = 0; i < 4; ++i) {
        const uint32_t keyPiece = (key >> (i * 8)) & 0xFF;
        shuffledKey ^= ((keyPiece ^ (keyPiece << 7) ^ (keyPiece << 14) ^ (keyPiece << 21))
                & 0x11111111) << i;
    }
    return shuffledKey;
}

bool TrieMap::writeValue(const uint64_t value, const int terminalEntryIndex) {
    if (value < VALUE_MASK) {
        // Write value into the terminal entry.
        return writeField1(value | VALUE_FLAG, terminalEntryIndex);
    }
    // Create value entry and write value.
    const int valueEntryIndex = allocateTable(TERMINAL_LINKED_ENTRY_COUNT);
    if (valueEntryIndex == INVALID_INDEX) {
        return false;
    }
    if (!writeEntry(Entry(value >> (FIELD1_SIZE * CHAR_BIT), value), valueEntryIndex)) {
        return false;
    }
    if (!writeEntry(EMPTY_BITMAP_ENTRY, valueEntryIndex + 1)) {
        return false;
    }
    return writeField1(valueEntryIndex | TERMINAL_LINK_FLAG, terminalEntryIndex);
}

bool TrieMap::updateValue(const Entry &terminalEntry, const uint64_t value,
        const int terminalEntryIndex) {
    if (!terminalEntry.hasTerminalLink()) {
        return writeValue(value, terminalEntryIndex);
    }
    const int valueEntryIndex = terminalEntry.getValueEntryIndex();
    return writeEntry(Entry(value >> (FIELD1_SIZE * CHAR_BIT), value), valueEntryIndex);
}

bool TrieMap::freeTable(const int tableIndex, const int entryCount) {
    if (!writeField0(readEmptyTableLink(entryCount), tableIndex)) {
        return false;
    }
    return writeEmptyTableLink(tableIndex, entryCount);
}

/**
 * Allocate table with entryCount-entries. Reuse freed table if possible.
 */
int TrieMap::allocateTable(const int entryCount) {
    if (entryCount > 0 && entryCount <= MAX_NUM_OF_ENTRIES_IN_ONE_LEVEL) {
        const int tableIndex = readEmptyTableLink(entryCount);
        if (tableIndex > 0) {
            if (!writeEmptyTableLink(readField0(tableIndex), entryCount)) {
                return INVALID_INDEX;
            }
            // Reuse the table.
            return tableIndex;
        }
    }
    // Allocate memory space at tail position of the buffer.
    const int mapIndex = getTailEntryIndex();
    if (!mBuffer.extend(entryCount * ENTRY_SIZE)) {
        return INVALID_INDEX;
    }
    return mapIndex;
}

int TrieMap::getTerminalEntryIndex(const uint32_t key, const uint32_t hashedKey,
        const Entry &bitmapEntry, const int level) const {
    const int label = getLabel(hashedKey, level);
    if (!exists(bitmapEntry.getBitmap(), label)) {
        return INVALID_INDEX;
    }
    const int entryIndex = bitmapEntry.getTableIndex() + popCount(bitmapEntry.getBitmap(), label);
    const Entry entry = readEntry(entryIndex);
    if (entry.isBitmapEntry()) {
        // Move to the next level.
        return getTerminalEntryIndex(key, hashedKey, entry, level + 1);
    }
    if (!entry.isValidTerminalEntry()) {
        return INVALID_INDEX;
    }
    if (entry.getKey() == key) {
        // Terminal entry is found.
        return entryIndex;
    }
    return INVALID_INDEX;
}

/**
 * Get Result corresponding to the key.
 *
 * @param key the key.
 * @param hashedKey the hashed key.
 * @param bitmapEntryIndex the index of bitmap entry
 * @param level current level
 * @return Result instance corresponding to the key. mIsValid indicates whether the key is in the
 * map.
 */
const TrieMap::Result TrieMap::getInternal(const uint32_t key, const uint32_t hashedKey,
        const int bitmapEntryIndex, const int level) const {
    const int terminalEntryIndex = getTerminalEntryIndex(key, hashedKey,
            readEntry(bitmapEntryIndex), level);
    if (terminalEntryIndex == INVALID_INDEX) {
        // Not found.
        return Result(0, false, INVALID_INDEX);
    }
    const Entry terminalEntry = readEntry(terminalEntryIndex);
    if (!terminalEntry.hasTerminalLink()) {
        return Result(terminalEntry.getValue(), true, INVALID_INDEX);
    }
    const int valueEntryIndex = terminalEntry.getValueEntryIndex();
    const Entry valueEntry = readEntry(valueEntryIndex);
    return Result(valueEntry.getValueOfValueEntry(), true, valueEntryIndex + 1);
}

/**
 * Put key to value mapping to the map.
 *
 * @param key the key.
 * @param value the value
 * @param hashedKey the hashed key.
 * @param bitmapEntryIndex the index of bitmap entry
 * @param bitmapEntry the bitmap entry
 * @param level current level
 * @return whether the key-value has been correctly inserted to the map or not.
 */
bool TrieMap::putInternal(const uint32_t key, const uint64_t value, const uint32_t hashedKey,
        const int bitmapEntryIndex, const Entry &bitmapEntry, const int level) {
    const int label = getLabel(hashedKey, level);
    const uint32_t bitmap = bitmapEntry.getBitmap();
    const int mapIndex = bitmapEntry.getTableIndex();
    if (!exists(bitmap, label)) {
        // Current map doesn't contain the label.
        return addNewEntryByExpandingTable(key, value, mapIndex, bitmap, bitmapEntryIndex, label);
    }
    const int entryIndex = mapIndex + popCount(bitmap, label);
    const Entry entry = readEntry(entryIndex);
    if (entry.isBitmapEntry()) {
        // Bitmap entry is found. Go to the next level.
        return putInternal(key, value, hashedKey, entryIndex, entry, level + 1);
    }
    if (!entry.isValidTerminalEntry()) {
        // Overwrite invalid terminal entry.
        return writeTerminalEntry(key, value, entryIndex);
    }
    if (entry.getKey() == key) {
        // Terminal entry for the key is found. Update the value.
        return updateValue(entry, value, entryIndex);
    }
    // Conflict with the existing key.
    return addNewEntryByResolvingConflict(key, value, hashedKey, entry, entryIndex, level);
}

/**
 * Resolve a conflict in the current level and add new entry.
 *
 * @param key the key
 * @param value the value
 * @param hashedKey the hashed key
 * @param conflictedEntry the existing conflicted entry
 * @param conflictedEntryIndex the index of existing conflicted entry
 * @param level current level
 * @return whether the key-value has been correctly inserted to the map or not.
 */
bool TrieMap::addNewEntryByResolvingConflict(const uint32_t key, const uint64_t value,
        const uint32_t hashedKey, const Entry &conflictedEntry, const int conflictedEntryIndex,
        const int level) {
    const int conflictedKeyNextLabel =
            getLabel(getBitShuffledKey(conflictedEntry.getKey()), level + 1);
    const int nextLabel = getLabel(hashedKey, level + 1);
    if (conflictedKeyNextLabel == nextLabel) {
        // Conflicted again in the next level.
        const int newTableIndex = allocateTable(1 /* entryCount */);
        if (newTableIndex == INVALID_INDEX) {
            return false;
        }
        if (!writeEntry(conflictedEntry, newTableIndex)) {
            return false;
        }
        const Entry newBitmapEntry(setExist(0 /* bitmap */, nextLabel), newTableIndex);
        if (!writeEntry(newBitmapEntry, conflictedEntryIndex)) {
            return false;
        }
        return putInternal(key, value, hashedKey, conflictedEntryIndex, newBitmapEntry, level + 1);
    }
    // The conflict has been resolved. Create a table that contains 2 entries.
    const int newTableIndex = allocateTable(2 /* entryCount */);
    if (newTableIndex == INVALID_INDEX) {
        return false;
    }
    if (nextLabel < conflictedKeyNextLabel) {
        if (!writeTerminalEntry(key, value, newTableIndex)) {
            return false;
        }
        if (!writeEntry(conflictedEntry, newTableIndex + 1)) {
            return false;
        }
    } else { // nextLabel > conflictedKeyNextLabel
        if (!writeEntry(conflictedEntry, newTableIndex)) {
            return false;
        }
        if (!writeTerminalEntry(key, value, newTableIndex + 1)) {
            return false;
        }
    }
    const uint32_t updatedBitmap =
            setExist(setExist(0 /* bitmap */, nextLabel), conflictedKeyNextLabel);
    return writeEntry(Entry(updatedBitmap, newTableIndex), conflictedEntryIndex);
}

/**
 * Add new entry to the existing table.
 */
bool TrieMap::addNewEntryByExpandingTable(const uint32_t key, const uint64_t value,
        const int tableIndex, const uint32_t bitmap, const int bitmapEntryIndex, const int label) {
    // Current map doesn't contain the label.
    const int entryCount = popCount(bitmap);
    const int newTableIndex = allocateTable(entryCount + 1);
    if (newTableIndex == INVALID_INDEX) {
        return false;
    }
    const int newEntryIndexInTable = popCount(bitmap, label);
    // Copy from existing table to the new table.
    for (int i = 0; i < entryCount; ++i) {
        if (!copyEntry(tableIndex + i, newTableIndex + i + (i >= newEntryIndexInTable ? 1 : 0))) {
            return false;
        }
    }
    // Write new terminal entry.
    if (!writeTerminalEntry(key, value, newTableIndex + newEntryIndexInTable)) {
        return false;
    }
    // Update bitmap.
    if (!writeEntry(Entry(setExist(bitmap, label), newTableIndex), bitmapEntryIndex)) {
        return false;
    }
    if (entryCount > 0) {
        return freeTable(tableIndex, entryCount);
    }
    return true;
}

bool TrieMap::removeInner(const Entry &bitmapEntry) {
    const int tableSize = popCount(bitmapEntry.getBitmap());
    if (tableSize <= 0) {
        // The table is empty. No need to remove any entries.
        return true;
    }
    for (int i = 0; i < tableSize; ++i) {
        const int entryIndex = bitmapEntry.getTableIndex() + i;
        const Entry entry = readEntry(entryIndex);
        if (entry.isBitmapEntry()) {
            // Delete next bitmap entry recursively.
            if (!removeInner(entry)) {
                return false;
            }
        } else {
            // Invalidate terminal entry just in case.
            if (!writeField1(VALUE_FLAG ^ INVALID_VALUE_IN_KEY_VALUE_ENTRY , entryIndex)) {
                return false;
            }
            if (entry.hasTerminalLink()) {
                const Entry nextLevelBitmapEntry = readEntry(entry.getValueEntryIndex() + 1);
                if (!freeTable(entry.getValueEntryIndex(), TERMINAL_LINKED_ENTRY_COUNT)) {
                    return false;
                }
                if (!removeInner(nextLevelBitmapEntry)) {
                    return false;
                }
            }
        }
    }
    return true;
}

}  // namespace latinime
