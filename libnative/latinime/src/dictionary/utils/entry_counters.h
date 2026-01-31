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

#ifndef LATINIME_ENTRY_COUNTERS_H
#define LATINIME_ENTRY_COUNTERS_H

#include <array>

#include "defines.h"
#include "utils/ngram_utils.h"

namespace latinime {

// Copyable but immutable
class EntryCounts final {
 public:
    EntryCounts() : mEntryCounts({{0, 0, 0, 0}}) {}

    explicit EntryCounts(const std::array<int, MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1> &counters)
            : mEntryCounts(counters) {}

    int getNgramCount(const NgramType ngramType) const {
        return mEntryCounts[static_cast<int>(ngramType)];
    }

    const std::array<int, MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1> &getCountArray() const {
        return mEntryCounts;
    }

 private:
    DISALLOW_ASSIGNMENT_OPERATOR(EntryCounts);

    // Counts from Unigram (0-th element) to (MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1)-gram
    // (MAX_PREV_WORD_COUNT_FOR_N_GRAM-th element)
    const std::array<int, MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1> mEntryCounts;
};

class MutableEntryCounters final {
 public:
    MutableEntryCounters() {
        mEntryCounters.fill(0);
    }

    explicit MutableEntryCounters(
            const std::array<int, MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1> &counters)
            : mEntryCounters(counters) {}

    const EntryCounts getEntryCounts() const {
        return EntryCounts(mEntryCounters);
    }

    void incrementNgramCount(const NgramType ngramType) {
        ++mEntryCounters[static_cast<int>(ngramType)];
    }

    void decrementNgramCount(const NgramType ngramType) {
        --mEntryCounters[static_cast<int>(ngramType)];
    }

    int getNgramCount(const NgramType ngramType) const {
        return mEntryCounters[static_cast<int>(ngramType)];
    }

    void setNgramCount(const NgramType ngramType, const int count) {
        mEntryCounters[static_cast<int>(ngramType)] = count;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(MutableEntryCounters);

    // Counters from Unigram (0-th element) to (MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1)-gram
    // (MAX_PREV_WORD_COUNT_FOR_N_GRAM-th element)
    std::array<int, MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1> mEntryCounters;
};
} // namespace latinime
#endif /* LATINIME_ENTRY_COUNTERS_H */
