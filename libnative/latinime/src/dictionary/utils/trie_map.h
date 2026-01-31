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

#ifndef LATINIME_TRIE_MAP_H
#define LATINIME_TRIE_MAP_H

#include <climits>
#include <cstdint>
#include <cstdio>
#include <vector>

#include "defines.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "utils/byte_array_view.h"

namespace latinime {

/**
 * Trie map derived from Phil Bagwell's Hash Array Mapped Trie.
 * key is int and value is uint64_t.
 * This supports multiple level map. Terminal entries can have a bitmap for the next level map.
 * This doesn't support root map resizing.
 */
class TrieMap {
 public:
    struct Result {
        const uint64_t mValue;
        const bool mIsValid;
        const int mNextLevelBitmapEntryIndex;

        Result(const uint64_t value, const bool isValid, const int nextLevelBitmapEntryIndex)
                : mValue(value), mIsValid(isValid),
                  mNextLevelBitmapEntryIndex(nextLevelBitmapEntryIndex) {}
    };

    /**
     * Struct to record iteration state in a table.
     */
    struct TableIterationState {
        int mTableSize;
        int mTableIndex;
        int mCurrentIndex;

        TableIterationState(const int tableSize, const int tableIndex)
                : mTableSize(tableSize), mTableIndex(tableIndex), mCurrentIndex(0) {}
    };

    class TrieMapRange;
    class TrieMapIterator {
     public:
        class IterationResult {
         public:
            IterationResult(const TrieMap *const trieMap, const int key, const uint64_t value,
                    const int nextLeveBitmapEntryIndex)
                    : mTrieMap(trieMap), mKey(key), mValue(value),
                      mNextLevelBitmapEntryIndex(nextLeveBitmapEntryIndex) {}

            const TrieMapRange getEntriesInNextLevel() const {
                return TrieMapRange(mTrieMap, mNextLevelBitmapEntryIndex);
            }

            bool hasNextLevelMap() const {
                return mNextLevelBitmapEntryIndex != INVALID_INDEX;
            }

            AK_FORCE_INLINE int key() const {
                return mKey;
            }

            AK_FORCE_INLINE uint64_t value() const {
                return mValue;
            }

            AK_FORCE_INLINE int getNextLevelBitmapEntryIndex() const {
                return mNextLevelBitmapEntryIndex;
            }

         private:
            const TrieMap *const mTrieMap;
            const int mKey;
            const uint64_t mValue;
            const int mNextLevelBitmapEntryIndex;
        };

        TrieMapIterator(const TrieMap *const trieMap, const int bitmapEntryIndex)
                : mTrieMap(trieMap), mStateStack(), mBaseBitmapEntryIndex(bitmapEntryIndex),
                  mKey(0), mValue(0), mIsValid(false), mNextLevelBitmapEntryIndex(INVALID_INDEX) {
            if (!trieMap || mBaseBitmapEntryIndex == INVALID_INDEX) {
                return;
            }
            const Entry bitmapEntry = mTrieMap->readEntry(mBaseBitmapEntryIndex);
            mStateStack.emplace_back(
                    mTrieMap->popCount(bitmapEntry.getBitmap()), bitmapEntry.getTableIndex());
            this->operator++();
        }

        const IterationResult operator*() const {
            return IterationResult(mTrieMap, mKey, mValue, mNextLevelBitmapEntryIndex);
        }

        bool operator!=(const TrieMapIterator &other) const {
            // Caveat: This works only for for loops.
            return mIsValid || other.mIsValid;
        }

        const TrieMapIterator &operator++() {
            const Result result = mTrieMap->iterateNext(&mStateStack, &mKey);
            mValue = result.mValue;
            mIsValid = result.mIsValid;
            mNextLevelBitmapEntryIndex = result.mNextLevelBitmapEntryIndex;
            return *this;
        }

     private:
        DISALLOW_DEFAULT_CONSTRUCTOR(TrieMapIterator);
        DISALLOW_ASSIGNMENT_OPERATOR(TrieMapIterator);

        const TrieMap *const mTrieMap;
        std::vector<TrieMap::TableIterationState> mStateStack;
        const int mBaseBitmapEntryIndex;
        int mKey;
        uint64_t mValue;
        bool mIsValid;
        int mNextLevelBitmapEntryIndex;
    };

    /**
     * Class to support iterating entries in TrieMap by range base for loops.
     */
    class TrieMapRange {
     public:
        TrieMapRange(const TrieMap *const trieMap, const int bitmapEntryIndex)
                : mTrieMap(trieMap), mBaseBitmapEntryIndex(bitmapEntryIndex) {};

        TrieMapIterator begin() const {
            return TrieMapIterator(mTrieMap, mBaseBitmapEntryIndex);
        }

        const TrieMapIterator end() const {
            return TrieMapIterator(nullptr, INVALID_INDEX);
        }

     private:
        DISALLOW_DEFAULT_CONSTRUCTOR(TrieMapRange);
        DISALLOW_ASSIGNMENT_OPERATOR(TrieMapRange);

        const TrieMap *const mTrieMap;
        const int mBaseBitmapEntryIndex;
    };

    static const int INVALID_INDEX;
    static const uint64_t MAX_VALUE;

    TrieMap();
    // Construct TrieMap using existing data in the memory region written by save().
    TrieMap(const ReadWriteByteArrayView buffer);
    void dump(const int from = 0, const int to = 0) const;

    bool isNearSizeLimit() const {
        return mBuffer.isNearSizeLimit();
    }

    int getRootBitmapEntryIndex() const {
        return ROOT_BITMAP_ENTRY_INDEX;
    }

    // Returns bitmapEntryIndex. Create the next level map if it doesn't exist.
    int getNextLevelBitmapEntryIndex(const int key) {
        return getNextLevelBitmapEntryIndex(key, ROOT_BITMAP_ENTRY_INDEX);
    }

    int getNextLevelBitmapEntryIndex(const int key, const int bitmapEntryIndex);

    const Result getRoot(const int key) const {
        return get(key, ROOT_BITMAP_ENTRY_INDEX);
    }

    const Result get(const int key, const int bitmapEntryIndex) const;

    bool putRoot(const int key, const uint64_t value) {
        return put(key, value, ROOT_BITMAP_ENTRY_INDEX);
    }

    bool put(const int key, const uint64_t value, const int bitmapEntryIndex);

    const TrieMapRange getEntriesInRootLevel() const {
        return getEntriesInSpecifiedLevel(ROOT_BITMAP_ENTRY_INDEX);
    }

    const TrieMapRange getEntriesInSpecifiedLevel(const int bitmapEntryIndex) const {
        return TrieMapRange(this, bitmapEntryIndex);
    }

    bool save(FILE *const file) const;

    bool remove(const int key, const int bitmapEntryIndex);

 private:
    DISALLOW_COPY_AND_ASSIGN(TrieMap);

    /**
     * Struct represents an entry.
     *
     * Entry is one of these entry types. All entries are fixed size and have 2 fields FIELD_0 and
     * FIELD_1.
     * 1. bitmap entry. bitmap entry contains bitmap and the link to hash table.
     *   FIELD_0(bitmap) FIELD_1(LINK_TO_HASH_TABLE)
     * 2. terminal entry. terminal entry contains hashed key and value or terminal link. terminal
     * entry have terminal link when the value is not fit to FIELD_1 or there is a next level map
     * for the key.
     *   FIELD_0(hashed key) (FIELD_1(VALUE_FLAG VALUE) | FIELD_1(TERMINAL_LINK_FLAG TERMINAL_LINK))
     * 3. value entry. value entry represents a value. Upper order bytes are stored in FIELD_0 and
     * lower order bytes are stored in FIELD_1.
     *   FIELD_0(value (upper order bytes)) FIELD_1(value (lower order bytes))
     */
    struct Entry {
        const uint32_t mData0;
        const uint32_t mData1;

        Entry(const uint32_t data0, const uint32_t data1) : mData0(data0), mData1(data1) {}

        AK_FORCE_INLINE bool isBitmapEntry() const {
            return (mData1 & VALUE_FLAG) == 0 && (mData1 & TERMINAL_LINK_FLAG) == 0;
        }

        AK_FORCE_INLINE bool hasTerminalLink() const {
            return (mData1 & TERMINAL_LINK_FLAG) != 0;
        }

        // For terminal entry.
        AK_FORCE_INLINE uint32_t getKey() const {
            return mData0;
        }

        // For terminal entry.
        AK_FORCE_INLINE uint32_t getValue() const {
            return mData1 & VALUE_MASK;
        }

        // For terminal entry.
        AK_FORCE_INLINE bool isValidTerminalEntry() const {
            return hasTerminalLink() || ((mData1 & VALUE_MASK) != INVALID_VALUE_IN_KEY_VALUE_ENTRY);
        }

        // For terminal entry.
        AK_FORCE_INLINE uint32_t getValueEntryIndex() const {
            return mData1 & TERMINAL_LINK_MASK;
        }

        // For bitmap entry.
        AK_FORCE_INLINE uint32_t getBitmap() const {
            return mData0;
        }

        // For bitmap entry.
        AK_FORCE_INLINE int getTableIndex() const {
            return static_cast<int>(mData1);
        }

        // For value entry.
        AK_FORCE_INLINE uint64_t getValueOfValueEntry() const {
            return ((static_cast<uint64_t>(mData0) << (FIELD1_SIZE * CHAR_BIT)) ^ mData1);
        }
    };

    BufferWithExtendableBuffer mBuffer;

    static const int FIELD0_SIZE;
    static const int FIELD1_SIZE;
    static const int ENTRY_SIZE;
    static const uint32_t VALUE_FLAG;
    static const uint32_t VALUE_MASK;
    static const uint32_t INVALID_VALUE_IN_KEY_VALUE_ENTRY;
    static const uint32_t TERMINAL_LINK_FLAG;
    static const uint32_t TERMINAL_LINK_MASK;
    static const int NUM_OF_BITS_USED_FOR_ONE_LEVEL;
    static const uint32_t LABEL_MASK;
    static const int MAX_NUM_OF_ENTRIES_IN_ONE_LEVEL;
    static const int ROOT_BITMAP_ENTRY_INDEX;
    static const int ROOT_BITMAP_ENTRY_POS;
    static const Entry EMPTY_BITMAP_ENTRY;
    static const int TERMINAL_LINKED_ENTRY_COUNT;
    static const int MAX_BUFFER_SIZE;

    uint32_t getBitShuffledKey(const uint32_t key) const;
    bool writeValue(const uint64_t value, const int terminalEntryIndex);
    bool updateValue(const Entry &terminalEntry, const uint64_t value,
            const int terminalEntryIndex);
    bool freeTable(const int tableIndex, const int entryCount);
    int allocateTable(const int entryCount);
    int getTerminalEntryIndex(const uint32_t key, const uint32_t hashedKey,
            const Entry &bitmapEntry, const int level) const;
    const Result getInternal(const uint32_t key, const uint32_t hashedKey,
            const int bitmapEntryIndex, const int level) const;
    bool putInternal(const uint32_t key, const uint64_t value, const uint32_t hashedKey,
            const int bitmapEntryIndex, const Entry &bitmapEntry, const int level);
    bool addNewEntryByResolvingConflict(const uint32_t key, const uint64_t value,
            const uint32_t hashedKey, const Entry &conflictedEntry, const int conflictedEntryIndex,
            const int level);
    bool addNewEntryByExpandingTable(const uint32_t key, const uint64_t value,
            const int tableIndex, const uint32_t bitmap, const int bitmapEntryIndex,
            const int label);
    const Result iterateNext(std::vector<TableIterationState> *const iterationState,
            int *const outKey) const;

    AK_FORCE_INLINE const Entry readEntry(const int entryIndex) const {
        return Entry(readField0(entryIndex), readField1(entryIndex));
    }

    // Returns whether an entry for the index is existing by testing if the index-th bit in the
    // bitmap is set or not.
    AK_FORCE_INLINE bool exists(const uint32_t bitmap, const int index) const {
        return (bitmap & (1 << index)) != 0;
    }

    // Set index-th bit in the bitmap.
    AK_FORCE_INLINE uint32_t setExist(const uint32_t bitmap, const int index) const {
        return bitmap | (1 << index);
    }

    // Count set bits before index in the bitmap.
    AK_FORCE_INLINE int popCount(const uint32_t bitmap, const int index) const {
        return popCount(bitmap & ((1 << index) - 1));
    }

    // Count set bits in the bitmap.
    AK_FORCE_INLINE int popCount(const uint32_t bitmap) const {
        return __builtin_popcount(bitmap);
        // int v = bitmap - ((bitmap >> 1) & 0x55555555);
        // v = (v & 0x33333333) + ((v >> 2) & 0x33333333);
        // return (((v + (v >> 4)) & 0x0F0F0F0F) * 0x01010101) >> 24;
    }

    AK_FORCE_INLINE int getLabel(const uint32_t hashedKey, const int level) const {
        return (hashedKey >> (level * NUM_OF_BITS_USED_FOR_ONE_LEVEL)) & LABEL_MASK;
    }

    AK_FORCE_INLINE uint32_t readField0(const int entryIndex) const {
        return mBuffer.readUint(FIELD0_SIZE, ROOT_BITMAP_ENTRY_POS + entryIndex * ENTRY_SIZE);
    }

    AK_FORCE_INLINE uint32_t readField1(const int entryIndex) const {
        return mBuffer.readUint(FIELD1_SIZE,
                ROOT_BITMAP_ENTRY_POS + entryIndex * ENTRY_SIZE + FIELD0_SIZE);
    }

    AK_FORCE_INLINE int readEmptyTableLink(const int entryCount) const {
        return mBuffer.readUint(FIELD1_SIZE, (entryCount - 1) * FIELD1_SIZE);
    }

    AK_FORCE_INLINE bool writeEmptyTableLink(const int tableIndex, const int entryCount) {
        return mBuffer.writeUint(tableIndex, FIELD1_SIZE, (entryCount - 1) * FIELD1_SIZE);
    }

    AK_FORCE_INLINE bool writeField0(const uint32_t data, const int entryIndex) {
        return mBuffer.writeUint(data, FIELD0_SIZE,
                ROOT_BITMAP_ENTRY_POS + entryIndex * ENTRY_SIZE);
    }

    AK_FORCE_INLINE bool writeField1(const uint32_t data, const int entryIndex) {
        return mBuffer.writeUint(data, FIELD1_SIZE,
                ROOT_BITMAP_ENTRY_POS + entryIndex * ENTRY_SIZE + FIELD0_SIZE);
    }

    AK_FORCE_INLINE bool writeEntry(const Entry &entry, const int entryIndex) {
        return writeField0(entry.mData0, entryIndex) && writeField1(entry.mData1, entryIndex);
    }

    AK_FORCE_INLINE bool writeTerminalEntry(const uint32_t key, const uint64_t value,
            const int entryIndex) {
        return writeField0(key, entryIndex) && writeValue(value, entryIndex);
    }

    AK_FORCE_INLINE bool copyEntry(const int originalEntryIndex, const int newEntryIndex) {
        return writeEntry(readEntry(originalEntryIndex), newEntryIndex);
    }

    AK_FORCE_INLINE int getTailEntryIndex() const {
        return (mBuffer.getTailPosition() - ROOT_BITMAP_ENTRY_POS) / ENTRY_SIZE;
    }

    bool removeInner(const Entry &bitmapEntry);
};

} // namespace latinime
#endif /* LATINIME_TRIE_MAP_H */
