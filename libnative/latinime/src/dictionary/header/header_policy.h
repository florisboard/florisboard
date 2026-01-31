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

#ifndef LATINIME_HEADER_POLICY_H
#define LATINIME_HEADER_POLICY_H

#include <cstdint>

#include "defines.h"
#include "dictionary/header/header_read_write_utils.h"
#include "dictionary/interface/dictionary_header_structure_policy.h"
#include "dictionary/utils/entry_counters.h"
#include "dictionary/utils/format_utils.h"
#include "utils/char_utils.h"
#include "utils/time_keeper.h"

namespace latinime {

class HeaderPolicy : public DictionaryHeaderStructurePolicy {
 public:
    // Reads information from existing dictionary buffer.
    HeaderPolicy(const uint8_t *const dictBuf, const FormatUtils::FORMAT_VERSION formatVersion)
            : mDictFormatVersion(formatVersion),
              mDictionaryFlags(HeaderReadWriteUtils::getFlags(dictBuf)),
              mSize(HeaderReadWriteUtils::getHeaderSize(dictBuf)),
              mAttributeMap(createAttributeMapAndReadAllAttributes(dictBuf)),
              mLocale(readLocale()),
              mMultiWordCostMultiplier(readMultipleWordCostMultiplier()),
              mRequiresGermanUmlautProcessing(readRequiresGermanUmlautProcessing()),
              mIsDecayingDict(HeaderReadWriteUtils::readBoolAttributeValue(&mAttributeMap,
                      IS_DECAYING_DICT_KEY, false /* defaultValue */)),
              mDate(HeaderReadWriteUtils::readIntAttributeValue(&mAttributeMap,
                      DATE_KEY, TimeKeeper::peekCurrentTime() /* defaultValue */)),
              mLastDecayedTime(HeaderReadWriteUtils::readIntAttributeValue(&mAttributeMap,
                      LAST_DECAYED_TIME_KEY, TimeKeeper::peekCurrentTime() /* defaultValue */)),
              mNgramCounts(readNgramCounts()), mMaxNgramCounts(readMaxNgramCounts()),
              mExtendedRegionSize(HeaderReadWriteUtils::readIntAttributeValue(&mAttributeMap,
                      EXTENDED_REGION_SIZE_KEY, 0 /* defaultValue */)),
              mHasHistoricalInfoOfWords(HeaderReadWriteUtils::readBoolAttributeValue(
                      &mAttributeMap, HAS_HISTORICAL_INFO_KEY, false /* defaultValue */)),
              mForgettingCurveProbabilityValuesTableId(HeaderReadWriteUtils::readIntAttributeValue(
                      &mAttributeMap, FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID_KEY,
                      DEFAULT_FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID)),
              mCodePointTable(HeaderReadWriteUtils::readCodePointTable(&mAttributeMap)) {}

    // Constructs header information using an attribute map.
    HeaderPolicy(const FormatUtils::FORMAT_VERSION dictFormatVersion,
            const std::vector<int> &locale,
            const DictionaryHeaderStructurePolicy::AttributeMap *const attributeMap)
            : mDictFormatVersion(dictFormatVersion),
              mDictionaryFlags(HeaderReadWriteUtils::createAndGetDictionaryFlagsUsingAttributeMap(
                      attributeMap)), mSize(0), mAttributeMap(*attributeMap), mLocale(locale),
              mMultiWordCostMultiplier(readMultipleWordCostMultiplier()),
              mRequiresGermanUmlautProcessing(readRequiresGermanUmlautProcessing()),
              mIsDecayingDict(HeaderReadWriteUtils::readBoolAttributeValue(&mAttributeMap,
                      IS_DECAYING_DICT_KEY, false /* defaultValue */)),
              mDate(HeaderReadWriteUtils::readIntAttributeValue(&mAttributeMap,
                      DATE_KEY, TimeKeeper::peekCurrentTime() /* defaultValue */)),
              mLastDecayedTime(HeaderReadWriteUtils::readIntAttributeValue(&mAttributeMap,
                      DATE_KEY, TimeKeeper::peekCurrentTime() /* defaultValue */)),
              mNgramCounts(readNgramCounts()), mMaxNgramCounts(readMaxNgramCounts()),
              mExtendedRegionSize(0),
              mHasHistoricalInfoOfWords(HeaderReadWriteUtils::readBoolAttributeValue(
                      &mAttributeMap, HAS_HISTORICAL_INFO_KEY, false /* defaultValue */)),
              mForgettingCurveProbabilityValuesTableId(HeaderReadWriteUtils::readIntAttributeValue(
                      &mAttributeMap, FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID_KEY,
                      DEFAULT_FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID)),
              mCodePointTable(HeaderReadWriteUtils::readCodePointTable(&mAttributeMap)) {}

    // Copy header information
    HeaderPolicy(const HeaderPolicy *const headerPolicy)
            : mDictFormatVersion(headerPolicy->mDictFormatVersion),
              mDictionaryFlags(headerPolicy->mDictionaryFlags), mSize(headerPolicy->mSize),
              mAttributeMap(headerPolicy->mAttributeMap), mLocale(headerPolicy->mLocale),
              mMultiWordCostMultiplier(headerPolicy->mMultiWordCostMultiplier),
              mRequiresGermanUmlautProcessing(headerPolicy->mRequiresGermanUmlautProcessing),
              mIsDecayingDict(headerPolicy->mIsDecayingDict),
              mDate(headerPolicy->mDate), mLastDecayedTime(headerPolicy->mLastDecayedTime),
              mNgramCounts(headerPolicy->mNgramCounts),
              mMaxNgramCounts(headerPolicy->mMaxNgramCounts),
              mExtendedRegionSize(headerPolicy->mExtendedRegionSize),
              mHasHistoricalInfoOfWords(headerPolicy->mHasHistoricalInfoOfWords),
              mForgettingCurveProbabilityValuesTableId(
                      headerPolicy->mForgettingCurveProbabilityValuesTableId),
              mCodePointTable(headerPolicy->mCodePointTable) {}

    // Temporary placeholder header.
    HeaderPolicy()
            : mDictFormatVersion(FormatUtils::UNKNOWN_VERSION), mDictionaryFlags(0), mSize(0),
              mAttributeMap(), mLocale(CharUtils::EMPTY_STRING), mMultiWordCostMultiplier(0.0f),
              mRequiresGermanUmlautProcessing(false), mIsDecayingDict(false),
              mDate(0), mLastDecayedTime(0), mNgramCounts(), mMaxNgramCounts(),
              mExtendedRegionSize(0), mHasHistoricalInfoOfWords(false),
              mForgettingCurveProbabilityValuesTableId(0), mCodePointTable(nullptr) {}

    ~HeaderPolicy() {}

    virtual int getFormatVersionNumber() const {
        // Conceptually this converts the symbolic value we use in the code into the
        // hardcoded of the bytes in the file. But we want the constants to be the
        // same so we use them for both here.
        switch (mDictFormatVersion) {
            case FormatUtils::VERSION_2:
            case FormatUtils::VERSION_201:
                AKLOGE("Dictionary versions 2 and 201 are incompatible with this version");
                return FormatUtils::UNKNOWN_VERSION;
            case FormatUtils::VERSION_202:
                return FormatUtils::VERSION_202;
            case FormatUtils::VERSION_4_ONLY_FOR_TESTING:
                return FormatUtils::VERSION_4_ONLY_FOR_TESTING;
            case FormatUtils::VERSION_402:
                return FormatUtils::VERSION_402;
            case FormatUtils::VERSION_403:
                return FormatUtils::VERSION_403;
            default:
                return FormatUtils::UNKNOWN_VERSION;
        }
    }

    AK_FORCE_INLINE bool isValid() const {
        // Decaying dictionary must have historical information.
        if (!mIsDecayingDict) {
            return true;
        }
        if (mHasHistoricalInfoOfWords) {
            return true;
        } else {
            return false;
        }
    }

    AK_FORCE_INLINE int getSize() const {
        return mSize;
    }

    AK_FORCE_INLINE float getMultiWordCostMultiplier() const {
        return mMultiWordCostMultiplier;
    }

    AK_FORCE_INLINE bool isDecayingDict() const {
        return mIsDecayingDict;
    }

    AK_FORCE_INLINE bool requiresGermanUmlautProcessing() const {
        return mRequiresGermanUmlautProcessing;
    }

    AK_FORCE_INLINE int getDate() const {
        return mDate;
    }

    AK_FORCE_INLINE int getLastDecayedTime() const {
        return mLastDecayedTime;
    }

    AK_FORCE_INLINE const EntryCounts &getNgramCounts() const {
        return mNgramCounts;
    }

    AK_FORCE_INLINE const EntryCounts getMaxNgramCounts() const {
        return mMaxNgramCounts;
    }

    AK_FORCE_INLINE int getExtendedRegionSize() const {
        return mExtendedRegionSize;
    }

    AK_FORCE_INLINE bool hasHistoricalInfoOfWords() const {
        return mHasHistoricalInfoOfWords;
    }

    AK_FORCE_INLINE bool shouldBoostExactMatches() const {
        // TODO: Investigate better ways to handle exact matches for personalized dictionaries.
        return !isDecayingDict();
    }

    const DictionaryHeaderStructurePolicy::AttributeMap *getAttributeMap() const {
        return &mAttributeMap;
    }

    AK_FORCE_INLINE int getForgettingCurveProbabilityValuesTableId() const {
        return mForgettingCurveProbabilityValuesTableId;
    }

    void readHeaderValueOrQuestionMark(const char *const key,
            int *outValue, int outValueSize) const;

    bool fillInAndWriteHeaderToBuffer(const bool updatesLastDecayedTime,
            const EntryCounts &entryCounts, const int extendedRegionSize,
            BufferWithExtendableBuffer *const outBuffer) const;

    void fillInHeader(const bool updatesLastDecayedTime, const EntryCounts &entryCounts,
            const int extendedRegionSize,
            DictionaryHeaderStructurePolicy::AttributeMap *outAttributeMap) const;

    AK_FORCE_INLINE const std::vector<int> *getLocale() const {
        return &mLocale;
    }

    bool supportsBeginningOfSentence() const {
        return mDictFormatVersion >= FormatUtils::VERSION_402;
    }

    const int *getCodePointTable() const {
        return mCodePointTable;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(HeaderPolicy);

    static const char *const MULTIPLE_WORDS_DEMOTION_RATE_KEY;
    static const char *const REQUIRES_GERMAN_UMLAUT_PROCESSING_KEY;
    static const char *const IS_DECAYING_DICT_KEY;
    static const char *const DATE_KEY;
    static const char *const LAST_DECAYED_TIME_KEY;
    static const char *const NGRAM_COUNT_KEYS[];
    static const char *const MAX_NGRAM_COUNT_KEYS[];
    static const int DEFAULT_MAX_NGRAM_COUNTS[];
    static const char *const EXTENDED_REGION_SIZE_KEY;
    static const char *const HAS_HISTORICAL_INFO_KEY;
    static const char *const LOCALE_KEY;
    static const char *const FORGETTING_CURVE_OCCURRENCES_TO_LEVEL_UP_KEY;
    static const char *const FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID_KEY;
    static const char *const FORGETTING_CURVE_DURATION_TO_LEVEL_DOWN_IN_SECONDS_KEY;
    static const int DEFAULT_MULTIPLE_WORDS_DEMOTION_RATE;
    static const float MULTIPLE_WORD_COST_MULTIPLIER_SCALE;
    static const int DEFAULT_FORGETTING_CURVE_PROBABILITY_VALUES_TABLE_ID;

    const FormatUtils::FORMAT_VERSION mDictFormatVersion;
    const HeaderReadWriteUtils::DictionaryFlags mDictionaryFlags;
    const int mSize;
    DictionaryHeaderStructurePolicy::AttributeMap mAttributeMap;
    const std::vector<int> mLocale;
    const float mMultiWordCostMultiplier;
    const bool mRequiresGermanUmlautProcessing;
    const bool mIsDecayingDict;
    const int mDate;
    const int mLastDecayedTime;
    const EntryCounts mNgramCounts;
    const EntryCounts mMaxNgramCounts;
    const int mExtendedRegionSize;
    const bool mHasHistoricalInfoOfWords;
    const int mForgettingCurveProbabilityValuesTableId;
    const int *const mCodePointTable;

    const std::vector<int> readLocale() const;
    float readMultipleWordCostMultiplier() const;
    bool readRequiresGermanUmlautProcessing() const;
    const EntryCounts readNgramCounts() const;
    const EntryCounts readMaxNgramCounts() const;
    static DictionaryHeaderStructurePolicy::AttributeMap createAttributeMapAndReadAllAttributes(
            const uint8_t *const dictBuf);
};
} // namespace latinime
#endif /* LATINIME_HEADER_POLICY_H */
