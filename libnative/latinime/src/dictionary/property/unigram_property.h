/*
 * Copyright (C) 2014 The Android Open Source Project
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

#ifndef LATINIME_UNIGRAM_PROPERTY_H
#define LATINIME_UNIGRAM_PROPERTY_H

#include <vector>

#include "defines.h"
#include "dictionary/property/historical_info.h"

namespace latinime {

class UnigramProperty {
 public:
    class ShortcutProperty {
     public:
        ShortcutProperty(const std::vector<int> &&targetCodePoints, const int probability)
                : mTargetCodePoints(std::move(targetCodePoints)),
                  mProbability(probability) {}

        const std::vector<int> *getTargetCodePoints() const {
            return &mTargetCodePoints;
        }

        int getProbability() const {
            return mProbability;
        }

     private:
        // Default copy constructor is used for using in std::vector.
        DISALLOW_DEFAULT_CONSTRUCTOR(ShortcutProperty);

        const std::vector<int> mTargetCodePoints;
        const int mProbability;
    };

    UnigramProperty()
            : mRepresentsBeginningOfSentence(false), mIsNotAWord(false),
              mIsBlacklisted(false), mIsPossiblyOffensive(false), mProbability(NOT_A_PROBABILITY),
              mHistoricalInfo(), mShortcuts() {}

    // In contexts which do not support the Blacklisted flag (v2, v4<403)
    UnigramProperty(const bool representsBeginningOfSentence, const bool isNotAWord,
            const bool isPossiblyOffensive, const int probability,
            const HistoricalInfo historicalInfo, const std::vector<ShortcutProperty> &&shortcuts)
            : mRepresentsBeginningOfSentence(representsBeginningOfSentence),
              mIsNotAWord(isNotAWord), mIsBlacklisted(false),
              mIsPossiblyOffensive(isPossiblyOffensive), mProbability(probability),
              mHistoricalInfo(historicalInfo), mShortcuts(std::move(shortcuts)) {}

    // Without shortcuts, in contexts which do not support the Blacklisted flag (v2, v4<403)
    UnigramProperty(const bool representsBeginningOfSentence, const bool isNotAWord,
            const bool isPossiblyOffensive, const int probability,
            const HistoricalInfo historicalInfo)
            : mRepresentsBeginningOfSentence(representsBeginningOfSentence),
              mIsNotAWord(isNotAWord), mIsBlacklisted(false),
              mIsPossiblyOffensive(isPossiblyOffensive), mProbability(probability),
              mHistoricalInfo(historicalInfo), mShortcuts() {}

    // In contexts which DO support the Blacklisted flag (v403)
    UnigramProperty(const bool representsBeginningOfSentence, const bool isNotAWord,
            const bool isBlacklisted, const bool isPossiblyOffensive, const int probability,
            const HistoricalInfo historicalInfo, const std::vector<ShortcutProperty> &&shortcuts)
            : mRepresentsBeginningOfSentence(representsBeginningOfSentence),
              mIsNotAWord(isNotAWord), mIsBlacklisted(isBlacklisted),
              mIsPossiblyOffensive(isPossiblyOffensive), mProbability(probability),
              mHistoricalInfo(historicalInfo), mShortcuts(std::move(shortcuts)) {}

    // Without shortcuts, in contexts which DO support the Blacklisted flag (v403)
    UnigramProperty(const bool representsBeginningOfSentence, const bool isNotAWord,
            const bool isBlacklisted, const bool isPossiblyOffensive, const int probability,
            const HistoricalInfo historicalInfo)
            : mRepresentsBeginningOfSentence(representsBeginningOfSentence),
              mIsNotAWord(isNotAWord), mIsBlacklisted(isBlacklisted),
              mIsPossiblyOffensive(isPossiblyOffensive), mProbability(probability),
              mHistoricalInfo(historicalInfo), mShortcuts() {}

    bool representsBeginningOfSentence() const {
        return mRepresentsBeginningOfSentence;
    }

    bool isNotAWord() const {
        return mIsNotAWord;
    }

    bool isPossiblyOffensive() const {
        return mIsPossiblyOffensive;
    }

    bool isBlacklisted() const {
        return mIsBlacklisted;
    }

    bool hasShortcuts() const {
        return !mShortcuts.empty();
    }

    int getProbability() const {
        return mProbability;
    }

    const HistoricalInfo getHistoricalInfo() const {
        return mHistoricalInfo;
    }

    const std::vector<ShortcutProperty> &getShortcuts() const {
        return mShortcuts;
    }

 private:
    // Default copy constructor is used for using as a return value.
    DISALLOW_ASSIGNMENT_OPERATOR(UnigramProperty);

    const bool mRepresentsBeginningOfSentence;
    const bool mIsNotAWord;
    const bool mIsBlacklisted;
    const bool mIsPossiblyOffensive;
    const int mProbability;
    const HistoricalInfo mHistoricalInfo;
    const std::vector<ShortcutProperty> mShortcuts;
};
} // namespace latinime
#endif // LATINIME_UNIGRAM_PROPERTY_H
