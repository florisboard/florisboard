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

#ifndef LATINIME_PROBABILITY_ENTRY_H
#define LATINIME_PROBABILITY_ENTRY_H

#include <climits>
#include <cstdint>

#include "defines.h"
#include "dictionary/property/historical_info.h"
#include "dictionary/property/ngram_property.h"
#include "dictionary/property/unigram_property.h"
#include "dictionary/structure/v4/ver4_dict_constants.h"

namespace latinime {

class ProbabilityEntry {
 public:
    ProbabilityEntry(const ProbabilityEntry &probabilityEntry)
            : mFlags(probabilityEntry.mFlags), mProbability(probabilityEntry.mProbability),
              mHistoricalInfo(probabilityEntry.mHistoricalInfo) {}

    // Placeholder entry
    ProbabilityEntry()
            : mFlags(Ver4DictConstants::FLAG_NOT_A_VALID_ENTRY), mProbability(NOT_A_PROBABILITY),
              mHistoricalInfo() {}

    // Entry without historical information
    ProbabilityEntry(const int flags, const int probability)
            : mFlags(flags), mProbability(probability), mHistoricalInfo() {}

    // Entry with historical information.
    ProbabilityEntry(const int flags, const HistoricalInfo *const historicalInfo)
            : mFlags(flags), mProbability(NOT_A_PROBABILITY), mHistoricalInfo(*historicalInfo) {}

    // Create from unigram property.
    ProbabilityEntry(const UnigramProperty *const unigramProperty)
            : mFlags(createFlags(unigramProperty->representsBeginningOfSentence(),
                    unigramProperty->isNotAWord(), unigramProperty->isBlacklisted(),
                    unigramProperty->isPossiblyOffensive())),
              mProbability(unigramProperty->getProbability()),
              mHistoricalInfo(unigramProperty->getHistoricalInfo()) {}

    // Create from ngram property.
    // TODO: Set flags.
    ProbabilityEntry(const NgramProperty *const ngramProperty)
            : mFlags(0), mProbability(ngramProperty->getProbability()),
              mHistoricalInfo(ngramProperty->getHistoricalInfo()) {}

    bool isValid() const {
        return (mFlags & Ver4DictConstants::FLAG_NOT_A_VALID_ENTRY) == 0;
    }

    bool hasHistoricalInfo() const {
        return mHistoricalInfo.isValid();
    }

    uint8_t getFlags() const {
        return mFlags;
    }

    int getProbability() const {
        return mProbability;
    }

    const HistoricalInfo *getHistoricalInfo() const {
        return &mHistoricalInfo;
    }

    bool representsBeginningOfSentence() const {
        return (mFlags & Ver4DictConstants::FLAG_REPRESENTS_BEGINNING_OF_SENTENCE) != 0;
    }

    bool isNotAWord() const {
        return (mFlags & Ver4DictConstants::FLAG_NOT_A_WORD) != 0;
    }

    bool isBlacklisted() const {
        return (mFlags & Ver4DictConstants::FLAG_BLACKLISTED) != 0;
    }

    bool isPossiblyOffensive() const {
        return (mFlags & Ver4DictConstants::FLAG_POSSIBLY_OFFENSIVE) != 0;
    }

    uint64_t encode(const bool hasHistoricalInfo) const {
        uint64_t encodedEntry = static_cast<uint8_t>(mFlags);
        if (hasHistoricalInfo) {
            encodedEntry = (encodedEntry << (Ver4DictConstants::TIME_STAMP_FIELD_SIZE * CHAR_BIT))
                    | static_cast<uint32_t>(mHistoricalInfo.getTimestamp());
            encodedEntry = (encodedEntry << (Ver4DictConstants::WORD_LEVEL_FIELD_SIZE * CHAR_BIT))
                    | static_cast<uint8_t>(mHistoricalInfo.getLevel());
            encodedEntry = (encodedEntry << (Ver4DictConstants::WORD_COUNT_FIELD_SIZE * CHAR_BIT))
                    | static_cast<uint16_t>(mHistoricalInfo.getCount());
        } else {
            encodedEntry = (encodedEntry << (Ver4DictConstants::PROBABILITY_SIZE * CHAR_BIT))
                    | static_cast<uint8_t>(mProbability);
        }
        return encodedEntry;
    }

    static ProbabilityEntry decode(const uint64_t encodedEntry, const bool hasHistoricalInfo) {
        if (hasHistoricalInfo) {
            const int flags = readFromEncodedEntry(encodedEntry,
                    Ver4DictConstants::FLAGS_IN_LANGUAGE_MODEL_SIZE,
                    Ver4DictConstants::TIME_STAMP_FIELD_SIZE
                            + Ver4DictConstants::WORD_LEVEL_FIELD_SIZE
                            + Ver4DictConstants::WORD_COUNT_FIELD_SIZE);
            const int timestamp = readFromEncodedEntry(encodedEntry,
                    Ver4DictConstants::TIME_STAMP_FIELD_SIZE,
                    Ver4DictConstants::WORD_LEVEL_FIELD_SIZE
                            + Ver4DictConstants::WORD_COUNT_FIELD_SIZE);
            const int level = readFromEncodedEntry(encodedEntry,
                    Ver4DictConstants::WORD_LEVEL_FIELD_SIZE,
                    Ver4DictConstants::WORD_COUNT_FIELD_SIZE);
            const int count = readFromEncodedEntry(encodedEntry,
                    Ver4DictConstants::WORD_COUNT_FIELD_SIZE, 0 /* pos */);
            const HistoricalInfo historicalInfo(timestamp, level, count);
            return ProbabilityEntry(flags, &historicalInfo);
        } else {
            const int flags = readFromEncodedEntry(encodedEntry,
                    Ver4DictConstants::FLAGS_IN_LANGUAGE_MODEL_SIZE,
                    Ver4DictConstants::PROBABILITY_SIZE);
            const int probability = readFromEncodedEntry(encodedEntry,
                    Ver4DictConstants::PROBABILITY_SIZE, 0 /* pos */);
            return ProbabilityEntry(flags, probability);
        }
    }

 private:
    // Copy constructor is public to use this class as a type of return value.
    DISALLOW_ASSIGNMENT_OPERATOR(ProbabilityEntry);

    const uint8_t mFlags;
    const int mProbability;
    const HistoricalInfo mHistoricalInfo;

    static int readFromEncodedEntry(const uint64_t encodedEntry, const int size, const int pos) {
        return static_cast<int>(
                (encodedEntry >> (pos * CHAR_BIT)) & ((1ull << (size * CHAR_BIT)) - 1));
    }

    static uint8_t createFlags(const bool representsBeginningOfSentence,
            const bool isNotAWord, const bool isBlacklisted, const bool isPossiblyOffensive) {
        uint8_t flags = 0;
        if (representsBeginningOfSentence) {
            flags |= Ver4DictConstants::FLAG_REPRESENTS_BEGINNING_OF_SENTENCE;
        }
        if (isNotAWord) {
            flags |= Ver4DictConstants::FLAG_NOT_A_WORD;
        }
        if (isBlacklisted) {
            flags |= Ver4DictConstants::FLAG_BLACKLISTED;
        }
        if (isPossiblyOffensive) {
            flags |= Ver4DictConstants::FLAG_POSSIBLY_OFFENSIVE;
        }
        return flags;
    }
};
} // namespace latinime
#endif /* LATINIME_PROBABILITY_ENTRY_H */
