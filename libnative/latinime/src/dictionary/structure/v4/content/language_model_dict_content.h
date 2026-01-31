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

#ifndef LATINIME_LANGUAGE_MODEL_DICT_CONTENT_H
#define LATINIME_LANGUAGE_MODEL_DICT_CONTENT_H

#include <cstdio>
#include <vector>

#include "defines.h"
#include "dictionary/property/word_attributes.h"
#include "dictionary/structure/v4/content/language_model_dict_content_global_counters.h"
#include "dictionary/structure/v4/content/probability_entry.h"
#include "dictionary/structure/v4/content/terminal_position_lookup_table.h"
#include "dictionary/structure/v4/ver4_dict_constants.h"
#include "dictionary/utils/entry_counters.h"
#include "dictionary/utils/trie_map.h"
#include "utils/byte_array_view.h"
#include "utils/int_array_view.h"

namespace latinime {

class HeaderPolicy;

/**
 * Class representing language model.
 *
 * This class provides methods to get and store unigram/n-gram probability information and flags.
 */
class LanguageModelDictContent {
 public:
    // Pair of word id and probability entry used for iteration.
    class WordIdAndProbabilityEntry {
     public:
        WordIdAndProbabilityEntry(const int wordId, const ProbabilityEntry &probabilityEntry)
                : mWordId(wordId), mProbabilityEntry(probabilityEntry) {}

        int getWordId() const { return mWordId; }
        const ProbabilityEntry getProbabilityEntry() const { return mProbabilityEntry; }

     private:
        DISALLOW_DEFAULT_CONSTRUCTOR(WordIdAndProbabilityEntry);
        DISALLOW_ASSIGNMENT_OPERATOR(WordIdAndProbabilityEntry);

        const int mWordId;
        const ProbabilityEntry mProbabilityEntry;
    };

    // Iterator.
    class EntryIterator {
     public:
        EntryIterator(const TrieMap::TrieMapIterator &trieMapIterator,
                const bool hasHistoricalInfo)
                : mTrieMapIterator(trieMapIterator), mHasHistoricalInfo(hasHistoricalInfo) {}

        const WordIdAndProbabilityEntry operator*() const {
            const TrieMap::TrieMapIterator::IterationResult &result = *mTrieMapIterator;
            return WordIdAndProbabilityEntry(
                    result.key(), ProbabilityEntry::decode(result.value(), mHasHistoricalInfo));
        }

        bool operator!=(const EntryIterator &other) const {
            return mTrieMapIterator != other.mTrieMapIterator;
        }

        const EntryIterator &operator++() {
            ++mTrieMapIterator;
            return *this;
        }

     private:
        DISALLOW_DEFAULT_CONSTRUCTOR(EntryIterator);
        DISALLOW_ASSIGNMENT_OPERATOR(EntryIterator);

        TrieMap::TrieMapIterator mTrieMapIterator;
        const bool mHasHistoricalInfo;
    };

    // Class represents range to use range base for loops.
    class EntryRange {
     public:
        EntryRange(const TrieMap::TrieMapRange trieMapRange, const bool hasHistoricalInfo)
                : mTrieMapRange(trieMapRange), mHasHistoricalInfo(hasHistoricalInfo) {}

        EntryIterator begin() const {
            return EntryIterator(mTrieMapRange.begin(), mHasHistoricalInfo);
        }

        EntryIterator end() const {
            return EntryIterator(mTrieMapRange.end(), mHasHistoricalInfo);
        }

     private:
        DISALLOW_DEFAULT_CONSTRUCTOR(EntryRange);
        DISALLOW_ASSIGNMENT_OPERATOR(EntryRange);

        const TrieMap::TrieMapRange mTrieMapRange;
        const bool mHasHistoricalInfo;
    };

    class DumppedFullEntryInfo {
     public:
        DumppedFullEntryInfo(std::vector<int> &prevWordIds, const int targetWordId,
                const WordAttributes &wordAttributes, const ProbabilityEntry &probabilityEntry)
                : mPrevWordIds(prevWordIds), mTargetWordId(targetWordId),
                  mWordAttributes(wordAttributes), mProbabilityEntry(probabilityEntry) {}

        const WordIdArrayView getPrevWordIds() const { return WordIdArrayView(mPrevWordIds); }
        int getTargetWordId() const { return mTargetWordId; }
        const WordAttributes &getWordAttributes() const { return mWordAttributes; }
        const ProbabilityEntry &getProbabilityEntry() const { return mProbabilityEntry; }

     private:
        DISALLOW_ASSIGNMENT_OPERATOR(DumppedFullEntryInfo);

        const std::vector<int> mPrevWordIds;
        const int mTargetWordId;
        const WordAttributes mWordAttributes;
        const ProbabilityEntry mProbabilityEntry;
    };

    LanguageModelDictContent(const ReadWriteByteArrayView *const buffers,
            const bool hasHistoricalInfo)
            : mTrieMap(buffers[TRIE_MAP_BUFFER_INDEX]),
              mGlobalCounters(buffers[GLOBAL_COUNTERS_BUFFER_INDEX]),
              mHasHistoricalInfo(hasHistoricalInfo) {}

    explicit LanguageModelDictContent(const bool hasHistoricalInfo)
            : mTrieMap(), mGlobalCounters(), mHasHistoricalInfo(hasHistoricalInfo) {}

    bool isNearSizeLimit() const {
        return mTrieMap.isNearSizeLimit() || mGlobalCounters.needsToHalveCounters();
    }

    bool save(FILE *const file) const;

    bool runGC(const TerminalPositionLookupTable::TerminalIdMap *const terminalIdMap,
            const LanguageModelDictContent *const originalContent);

    const WordAttributes getWordAttributes(const WordIdArrayView prevWordIds, const int wordId,
            const bool mustMatchAllPrevWords, const HeaderPolicy *const headerPolicy) const;

    ProbabilityEntry getProbabilityEntry(const int wordId) const {
        return getNgramProbabilityEntry(WordIdArrayView(), wordId);
    }

    bool setProbabilityEntry(const int wordId, const ProbabilityEntry *const probabilityEntry) {
        mGlobalCounters.addToTotalCount(probabilityEntry->getHistoricalInfo()->getCount());
        return setNgramProbabilityEntry(WordIdArrayView(), wordId, probabilityEntry);
    }

    bool removeProbabilityEntry(const int wordId) {
        return removeNgramProbabilityEntry(WordIdArrayView(), wordId);
    }

    ProbabilityEntry getNgramProbabilityEntry(const WordIdArrayView prevWordIds,
            const int wordId) const;

    bool setNgramProbabilityEntry(const WordIdArrayView prevWordIds, const int wordId,
            const ProbabilityEntry *const probabilityEntry);

    bool removeNgramProbabilityEntry(const WordIdArrayView prevWordIds, const int wordId);

    EntryRange getProbabilityEntries(const WordIdArrayView prevWordIds) const;

    std::vector<DumppedFullEntryInfo> exportAllNgramEntriesRelatedToWord(
            const HeaderPolicy *const headerPolicy, const int wordId) const;

    bool updateAllProbabilityEntriesForGC(const HeaderPolicy *const headerPolicy,
            MutableEntryCounters *const outEntryCounters) {
        if (!updateAllProbabilityEntriesForGCInner(mTrieMap.getRootBitmapEntryIndex(),
                0 /* prevWordCount */, headerPolicy, mGlobalCounters.needsToHalveCounters(),
                outEntryCounters)) {
            return false;
        }
        if (mGlobalCounters.needsToHalveCounters()) {
            mGlobalCounters.halveCounters();
        }
        return true;
    }

    // entryCounts should be created by updateAllProbabilityEntries.
    bool truncateEntries(const EntryCounts &currentEntryCounts, const EntryCounts &maxEntryCounts,
            const HeaderPolicy *const headerPolicy, MutableEntryCounters *const outEntryCounters);

    bool updateAllEntriesOnInputWord(const WordIdArrayView prevWordIds, const int wordId,
            const bool isValid, const HistoricalInfo historicalInfo,
            const HeaderPolicy *const headerPolicy,
            MutableEntryCounters *const entryCountersToUpdate);

 private:
    DISALLOW_COPY_AND_ASSIGN(LanguageModelDictContent);

    class EntryInfoToTurncate {
     public:
        class Comparator {
         public:
            bool operator()(const EntryInfoToTurncate &left,
                    const EntryInfoToTurncate &right) const;
         private:
            DISALLOW_ASSIGNMENT_OPERATOR(Comparator);
        };

        EntryInfoToTurncate(const int priority, const int count, const int key,
                const int prevWordCount, const int *const prevWordIds);

        int mPriority;
        // TODO: Remove.
        int mCount;
        int mKey;
        int mPrevWordCount;
        int mPrevWordIds[MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1];

     private:
        DISALLOW_DEFAULT_CONSTRUCTOR(EntryInfoToTurncate);
    };

    static const int TRIE_MAP_BUFFER_INDEX;
    static const int GLOBAL_COUNTERS_BUFFER_INDEX;

    TrieMap mTrieMap;
    LanguageModelDictContentGlobalCounters mGlobalCounters;
    const bool mHasHistoricalInfo;

    bool runGCInner(const TerminalPositionLookupTable::TerminalIdMap *const terminalIdMap,
            const TrieMap::TrieMapRange trieMapRange, const int nextLevelBitmapEntryIndex);
    int createAndGetBitmapEntryIndex(const WordIdArrayView prevWordIds);
    int getBitmapEntryIndex(const WordIdArrayView prevWordIds) const;
    bool updateAllProbabilityEntriesForGCInner(const int bitmapEntryIndex, const int prevWordCount,
            const HeaderPolicy *const headerPolicy, const bool needsToHalveCounters,
            MutableEntryCounters *const outEntryCounters);
    bool turncateEntriesInSpecifiedLevel(const HeaderPolicy *const headerPolicy,
            const int maxEntryCount, const int targetLevel, int *const outEntryCount);
    bool getEntryInfo(const HeaderPolicy *const headerPolicy, const int targetLevel,
            const int bitmapEntryIndex, std::vector<int> *const prevWordIds,
            std::vector<EntryInfoToTurncate> *const outEntryInfo) const;
    const ProbabilityEntry createUpdatedEntryFrom(const ProbabilityEntry &originalProbabilityEntry,
            const bool isValid, const HistoricalInfo historicalInfo,
            const HeaderPolicy *const headerPolicy) const;
    void exportAllNgramEntriesRelatedToWordInner(const HeaderPolicy *const headerPolicy,
            const int bitmapEntryIndex, std::vector<int> *const prevWordIds,
            std::vector<DumppedFullEntryInfo> *const outBummpedFullEntryInfo) const;
};
} // namespace latinime
#endif /* LATINIME_LANGUAGE_MODEL_DICT_CONTENT_H */
